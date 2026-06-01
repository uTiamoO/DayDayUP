# Auth 认证系统完善设计

## 1. 背景与目标

DayDayUP 的 auth 服务当前具备基本的登录（`/oauth2/token`）、刷新（`/oauth2/refresh`）和 JWT RSA256 签发能力。但存在以下安全缺口：

- 停用用户仍能登录。
- 无登录失败计数与锁定机制。
- 登录历史（`LoginHistory`）实体已创建但未写入记录。
- 无登出端点，access_token 在过期前无法即时失效。
- 无 Token 黑名单机制。
- `RefreshToken` 的 `clientIp`/`userAgent` 字段未填充。
- 用户最后登录时间/IP 未更新。

本次目标是补齐认证安全能力，实现登录安全闭环和 Token 生命周期管理。

## 2. 总体架构

### 2.1 角色分工

- **auth 服务**：负责登录、登出、刷新、Token 黑名单写入、登录历史记录、登录锁定、用户最后登录信息更新。
- **gateway**：负责 Token 黑名单校验（从 Redis 读取）。
- **admin-biz**：提供用户信息查询（已有）和登录信息更新（新增 Feign 端点）。

### 2.2 数据流

#### 登录流程

1. 客户端 POST `/oauth2/token`（username + password）。
2. auth 通过 Feign 调 admin-biz 获取用户信息。
3. 检查用户 `status`，停用则拒绝（返回 `LOGIN_FAILED`，不暴露具体原因）。
4. 检查 Redis 登录失败计数 `auth:login:fail:{username}`，超过阈值且未过锁定窗口则拒绝（返回 `LOGIN_LOCKED`）。
5. 校验密码。失败则 Redis 计数 +1 并记录 `LoginHistory(success=0)`。
6. 密码通过则清除失败计数、记录 `LoginHistory(success=1)`、通过 Feign 更新用户 `lastLoginAt`/`lastLoginIp`、签发 JWT。

#### 登出流程

1. 客户端 POST `/oauth2/logout`（携带 Authorization: Bearer token）。
2. auth 解析 JWT 获取 JTI 和过期时间。
3. 将 JTI 写入 Redis 黑名单 `auth:token:blacklist:{jti}`，TTL = token 剩余有效期。
4. 吊销该用户的所有有效 refresh_token。

#### 请求鉴权流程（gateway）

1. `HeaderAuthenticationFilter` 从 Header 解析 JWT。
2. 从 JWT 解析 JTI。
3. 查 Redis `auth:token:blacklist:{jti}`，命中则返回 401。
4. 未命中则正常还原 UserContext。

#### 管理员踢下线

1. 通过 admin 后台调用 auth 的批量吊销端点。
2. auth 吊销指定用户所有有效 refresh_token。

## 3. Auth 服务改动

### 3.1 用户状态检查

登录时从 `AuthUserVO.getStatus()` 检查。`status != 1` 则抛出 `BizException(ErrorCode.LOGIN_FAILED)`。不暴露"用户已停用"等具体原因，防止枚举攻击。

### 3.2 登录失败计数与锁定

Redis key：`auth:login:fail:{username}`。

- 失败一次，`INCR` + 设置 TTL = 900 秒（15 分钟窗口）。
- 成功登录时 `DEL` 清除计数。
- 检查计数 >= 5 时拒绝登录，返回 `BizException(ErrorCode.LOGIN_LOCKED)`。

如果用户在锁定窗口内持续失败，每次都会刷新 TTL，形成滑动窗口效果。

### 3.3 登录历史记录

成功和失败都写入 `auth_login_history` 表：

- `userId`：登录用户 ID（失败时可能为 null）。
- `username`：登录用户名。
- `loginAt`：登录时间。
- `clientIp`：客户端 IP，通过 `HttpServletRequest` 获取。
- `userAgent`：User-Agent，通过 `HttpServletRequest` 获取。
- `success`：0 = 失败，1 = 成功。
- `failureReason`：失败原因（密码错误 / 用户停用 / 账号锁定）。

### 3.4 更新用户最后登录信息

登录成功后通过 Feign 调 admin-biz 更新 `lastLoginAt` 和 `lastLoginIp`。

在 `UserClient` Feign 接口新增：

```java
@PatchMapping("/users/manage/{userId}/login-info")
R<Void> updateLoginInfo(@PathVariable Long userId,
                        @RequestParam String lastLoginIp);
```

admin-biz 的 `UserManageController` 新增对应实现。

### 3.5 登出端点

`POST /oauth2/logout`

- 从 Authorization Header 获取 Bearer token。
- 解析 JWT 获取 JTI（`jwt.getId()`）和过期时间。
- 写入 Redis：`SET auth:token:blacklist:{jti} "1" EX {剩余秒数}`。
- 吊销当前用户的所有有效 refresh_token。

### 3.6 管理员批量吊销

`POST /oauth2/revoke/{userId}`（需 `admin:*` 权限）

- 吊销指定用户所有有效 refresh_token。
- 已签发的 access_token 在过期前仍有效，除非主动将其 JTI 加入黑名单。

### 3.7 填充 RefreshToken 的 clientIp/userAgent

登录签发 refresh_token 时，从 `HttpServletRequest` 获取 clientIp 和 userAgent，写入 `RefreshToken` 记录。

## 4. Gateway 改动

### 4.1 HeaderAuthenticationFilter 增加黑名单校验

在现有 JWT 解析和 UserContext 还原之后增加：

1. 从 JWT 中提取 JTI（`jwt.getId()`）。
2. 查询 Redis `auth:token:blacklist:{jti}`。
3. 如果 key 存在，返回 401 响应，不继续执行后续 Filter。

### 4.2 依赖调整

gateway 模块需要增加 Redis 依赖（`daydayup-common-redis`），当前 gateway 只依赖了 security 和 feign。

### 4.3 异常处理

Redis 不可用时采用安全优先策略：拒绝请求，避免黑名单失效导致已登出的 token 仍可用。记录告警日志，但不放行。

### 4.4 性能

黑名单校验使用 `StringRedisTemplate.hasKey()`，单次 O(1) 查询，性能开销极小。

## 5. Redis Key 设计

| 用途 | Key 模式 | TTL | 写入方 | 读取方 |
|------|---------|-----|--------|--------|
| 登录失败计数 | `auth:login:fail:{username}` | 900s（15 分钟） | auth | auth |
| Token 黑名单 | `auth:token:blacklist:{jti}` | access_token 剩余有效期 | auth | gateway |

## 6. 错误码

在 `ErrorCode` 枚举中新增：

- `LOGIN_LOCKED`：登录已被锁定，请稍后再试。
- `TOKEN_BLACKLISTED`：Token 已失效。

## 7. Feign 接口扩展

在 `UserClient` 中新增：

```java
@PatchMapping("/users/manage/{userId}/login-info")
R<Void> updateLoginInfo(@PathVariable Long userId,
                        @RequestParam String lastLoginIp);
```

admin-biz 的 `UserManageController` 新增对应实现，校验用户存在后更新 `lastLoginAt` 和 `lastLoginIp`。

## 8. 实施阶段

### 阶段 1：auth 服务基础安全

- 新增 `LOGIN_LOCKED` 和 `TOKEN_BLACKLISTED` 错误码。
- 登录时检查用户状态。
- 登录失败计数与锁定（Redis）。
- 登录历史记录（成功/失败都写入 `LoginHistory`）。
- 填充 RefreshToken 的 clientIp/userAgent。
- Feign 接口扩展：`UserClient.updateLoginInfo`。
- admin-biz 新增 `updateLoginInfo` 端点。
- 登录成功后更新 lastLoginAt/lastLoginIp。

### 阶段 2：auth 服务 Token 管理

- 登出端点 `POST /oauth2/logout`：Redis 黑名单写入 + refresh_token 吊销。
- 管理员批量吊销端点 `POST /oauth2/revoke/{userId}`。

### 阶段 3：gateway 黑名单校验

- gateway 增加 `daydayup-common-redis` 依赖。
- `HeaderAuthenticationFilter` 增加 Redis 黑名单校验。
- Redis 不可用时拒绝请求。

### 阶段 4：验证

- 编译 auth、gateway、admin 模块。
- 验证停用用户被拒绝登录。
- 验证登录失败 5 次后锁定。
- 验证登录历史记录。
- 验证登出后 access_token 被拒绝。
- 验证 gateway 黑名单校验生效。
- 验证 refresh_token 吊销后无法刷新。
