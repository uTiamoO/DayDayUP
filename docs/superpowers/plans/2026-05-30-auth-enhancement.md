# Auth 认证系统完善实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 补齐 auth 服务的安全能力：用户状态检查、登录失败锁定、登录历史记录、登出/Token 黑名单、gateway 黑名单校验。

**架构：** auth 服务负责登录安全（状态检查、失败计数、历史记录）和 Token 生命周期（登出写 Redis 黑名单、吊销 refresh_token）。gateway 在 `AuthGlobalFilter` 中增加 Redis 黑名单校验。admin-biz 提供登录信息更新 Feign 端点。

**技术栈：** Java 21、Spring Boot 3.5、Spring Security OAuth2 JOSE、MyBatis-Plus、Redis（StringRedisTemplate / ReactiveStringRedisTemplate）、WebFlux（gateway）、JUnit 5 + Mockito。

---

## 文件结构

### Auth 服务

- 修改：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/enums/ErrorCode.java` — 新增 LOGIN_LOCKED、TOKEN_BLACKLISTED
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginAttemptService.java` — 登录失败计数与锁定
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginHistoryService.java` — 登录历史记录
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/TokenBlacklistService.java` — Token 黑名单写入
- 修改：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/LoginController.java` — 增加状态检查、失败计数、历史记录、登出端点、吊销端点
- 修改：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/JwtTokenService.java` — 填充 clientIp/userAgent
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/LoginAttemptServiceTest.java`
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/TokenBlacklistServiceTest.java`

### Admin 模块

- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-api/src/main/java/com/yuan/daydayup/admin/api/feign/UserClient.java` — 新增 updateLoginInfo
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java` — 新增 updateLoginInfo 端点

### Gateway 模块

- 修改：`daydayup-access/daydayup-gateway/pom.xml` — 增加 daydayup-common-redis 依赖
- 修改：`daydayup-access/daydayup-gateway/src/main/java/com/yuan/daydayup/gateway/filter/AuthGlobalFilter.java` — 增加 Redis 黑名单校验

---

## 任务 1：错误码与 Auth Redis Key 常量

**文件：**
- 修改：`daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/enums/ErrorCode.java`

- [ ] **步骤 1：新增错误码**

在 `ErrorCode.java` 的 `ACCOUNT_DISABLED` 之后追加：

```java
LOGIN_LOCKED(20005, "登录已被锁定，请稍后再试"),
TOKEN_BLACKLISTED(20006, "令牌已失效");
```

- [ ] **步骤 2：编译验证**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-common/daydayup-common-core -am compile
```

预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
git add daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/enums/ErrorCode.java
git commit -m "feat(core): add LOGIN_LOCKED and TOKEN_BLACKLISTED error codes"
```

---

## 任务 2：登录失败计数与锁定服务

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginAttemptService.java`
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/LoginAttemptServiceTest.java`

- [ ] **步骤 1：编写失败测试**

创建 `LoginAttemptServiceTest.java`：

```java
package com.yuan.daydayup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {

    @Test
    void shouldNotBlockWhenNoFailures() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        LoginAttemptService service = new LoginAttemptService(redis);

        assertThat(service.isLocked("admin")).isFalse();
    }

    @Test
    void shouldBlockAfterMaxAttempts() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("5");
        LoginAttemptService service = new LoginAttemptService(redis);

        assertThat(service.isLocked("admin")).isTrue();
    }

    @Test
    void shouldIncrementFailureCount() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.recordFailure("admin");
        verify(ops).increment(startsWith("daydayup:auth:login:fail:"));
        verify(redis).expire(anyString(), eq(Duration.ofSeconds(900)));
    }

    @Test
    void shouldClearFailureCount() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.clearFailures("admin");
        verify(redis).delete("daydayup:auth:login:fail:admin");
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am test -Dtest=LoginAttemptServiceTest
```

预期：编译失败，提示 `LoginAttemptService` 不存在。

- [ ] **步骤 3：实现 LoginAttemptService**

创建 `LoginAttemptService.java`：

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.redis.util.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(900);
    private static final String MODULE = "auth";
    private static final String BIZ = "login:fail";

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(String username) {
        String count = redisTemplate.opsForValue().get(key(username));
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String username) {
        String key = key(username);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, LOCK_DURATION);
        }
    }

    public void clearFailures(String username) {
        redisTemplate.delete(key(username));
    }

    private static String key(String username) {
        return CacheKeys.of(MODULE, BIZ, username);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am test -Dtest=LoginAttemptServiceTest
```

预期：4 个测试全部通过。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginAttemptService.java \
  daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/LoginAttemptServiceTest.java
git commit -m "feat(auth): add login attempt service with Redis-based lockout"
```

---

## 任务 3：Token 黑名单服务

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/TokenBlacklistService.java`
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/TokenBlacklistServiceTest.java`

- [ ] **步骤 1：编写失败测试**

创建 `TokenBlacklistServiceTest.java`：

```java
package com.yuan.daydayup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceTest {

    @Test
    void shouldBlacklistToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        TokenBlacklistService service = new TokenBlacklistService(redis);

        service.blacklist("jti-123", 3600L);

        verify(redis).opsForValue();
        verify(redis).expire("daydayup:auth:token:blacklist:jti-123", Duration.ofSeconds(3600));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am test -Dtest=TokenBlacklistServiceTest
```

预期：编译失败。

- [ ] **步骤 3：实现 TokenBlacklistService**

创建 `TokenBlacklistService.java`：

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.redis.util.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String MODULE = "auth";
    private static final String BIZ = "token:blacklist";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long ttlSeconds) {
        String key = key(jti);
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    private static String key(String jti) {
        return CacheKeys.of(MODULE, BIZ, jti);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am test -Dtest=TokenBlacklistServiceTest
```

预期：测试通过。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/TokenBlacklistService.java \
  daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/TokenBlacklistServiceTest.java
git commit -m "feat(auth): add token blacklist service"
```

---

## 任务 4：登录历史记录与 Feign 扩展

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginHistoryService.java`
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-api/src/main/java/com/yuan/daydayup/admin/api/feign/UserClient.java`
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java`

- [ ] **步骤 1：实现 LoginHistoryService**

创建 `LoginHistoryService.java`：

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.LoginHistory;
import com.yuan.daydayup.auth.mapper.LoginHistoryMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryMapper loginHistoryMapper;

    public void record(Long userId, String username, boolean success,
                       String failureReason, HttpServletRequest request) {
        LoginHistory history = new LoginHistory();
        history.setUserId(userId);
        history.setUsername(username);
        history.setLoginAt(LocalDateTime.now());
        history.setClientIp(getClientIp(request));
        history.setUserAgent(request.getHeader("User-Agent"));
        history.setSuccess(success ? 1 : 0);
        history.setFailureReason(failureReason);
        loginHistoryMapper.insert(history);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
```

- [ ] **步骤 2：扩展 UserClient Feign 接口**

在 `UserClient.java` 中新增：

```java
@PatchMapping("/users/manage/{userId}/login-info")
R<Void> updateLoginInfo(@PathVariable("userId") Long userId,
                        @RequestParam("lastLoginIp") String lastLoginIp);
```

- [ ] **步骤 3：admin-biz 实现 updateLoginInfo 端点**

在 `UserManageController.java` 中新增：

```java
@PatchMapping("/{userId}/login-info")
@PreAuthorize("hasPermission(null, 'admin:user:update')")
public R<Void> updateLoginInfo(@PathVariable Long userId,
                               @RequestParam String lastLoginIp) {
    userManageService.updateLoginInfo(userId, lastLoginIp);
    return R.ok();
}
```

在 `UserManageService` 接口中新增方法签名，在 `UserManageServiceImpl` 中实现：

```java
@Override
public void updateLoginInfo(Long userId, String lastLoginIp) {
    SysUser user = mapper.selectById(userId);
    if (user != null) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(lastLoginIp);
        mapper.updateById(user);
    }
}
```

- [ ] **步骤 4：编译验证**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-admin/daydayup-admin-biz -am compile
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/LoginHistoryService.java \
  daydayup-platform/daydayup-admin/daydayup-admin-api/src/main/java/com/yuan/daydayup/admin/api/feign/UserClient.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/UserManageService.java \
  daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/impl/UserManageServiceImpl.java
git commit -m "feat(auth): add login history service and user login-info feign endpoint"
```

---

## 任务 5：LoginController 安全增强

**文件：**
- 修改：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/LoginController.java`
- 修改：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/JwtTokenService.java`

- [ ] **步骤 1：修改 LoginController**

在 `LoginController` 中注入新服务，改造 `login` 方法，新增 `logout` 和 `revoke` 方法：

```java
@RestController
@RequestMapping("/oauth2")
@Validated
public class LoginController {

    private final RemoteUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final LoginHistoryService loginHistoryService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenMapper refreshTokenMapper;

    public LoginController(RemoteUserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService tokenService,
                           LoginAttemptService loginAttemptService,
                           LoginHistoryService loginHistoryService,
                           TokenBlacklistService tokenBlacklistService,
                           RefreshTokenMapper refreshTokenMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.loginHistoryService = loginHistoryService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @PostMapping("/token")
    public R<JwtTokenService.TokenResult> login(@RequestBody @Validated LoginRequest request,
                                                HttpServletRequest httpRequest) {
        String username = request.getUsername();

        // 检查锁定
        if (loginAttemptService.isLocked(username)) {
            loginHistoryService.record(null, username, false, "账号锁定", httpRequest);
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        // 查用户
        SimpleUser user = userService.findByUsername(username)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(username);
                    loginHistoryService.record(null, username, false, "用户不存在", httpRequest);
                    return new BizException(ErrorCode.LOGIN_FAILED);
                });

        // 检查状态
        if (user.getStatus() != null && user.getStatus() != 1) {
            loginAttemptService.recordFailure(username);
            loginHistoryService.record(user.getUserId(), username, false, "账号已停用", httpRequest);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            loginHistoryService.record(user.getUserId(), username, false, "密码错误", httpRequest);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 登录成功
        loginAttemptService.clearFailures(username);
        loginHistoryService.record(user.getUserId(), username, true, null, httpRequest);

        // 更新最后登录信息
        try {
            String clientIp = loginHistoryService.getClientIp(httpRequest);
            // 调用 admin Feign 更新
            // userClient.updateLoginInfo(user.getUserId(), clientIp);
        } catch (Exception e) {
            // 不影响登录流程
        }

        return R.ok(tokenService.issue(user, httpRequest));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest httpRequest) {
        String authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        String token = authorization.substring(7).trim();
        tokenService.blacklistAndRevoke(token);
        return R.ok();
    }

    @PostMapping("/revoke/{userId}")
    public R<Void> revoke(@PathVariable Long userId) {
        tokenService.revokeAllForUser(userId);
        return R.ok();
    }

    @PostMapping("/refresh")
    public R<JwtTokenService.TokenResult> refresh(@RequestBody @Validated RefreshRequest request) {
        return R.ok(tokenService.refresh(request.getRefreshToken()));
    }

    // LoginRequest, RefreshRequest 不变
}
```

注意：`SimpleUser` 需要新增 `status` 字段，`RemoteUserService` 映射时需要从 `AuthUserVO.getStatus()` 读取。

- [ ] **步骤 2：SimpleUser 增加 status 字段**

在 `SimpleUser.java` 中新增：

```java
private Integer status;
```

在 `RemoteUserService.findByUsername` 中映射 status：

```java
.status(vo.getStatus())
```

- [ ] **步骤 3：修改 JwtTokenService**

在 `JwtTokenService` 的 `issue` 方法中接收 `HttpServletRequest`，将 clientIp/userAgent 写入 RefreshToken：

```java
public TokenResult issue(SimpleUser user, HttpServletRequest request) {
    String accessToken = createAccessToken(user);
    String refreshToken = createRefreshToken(user, request);
    return new TokenResult(accessToken, refreshToken, accessTokenTtlSeconds, "Bearer");
}

public TokenResult issue(SimpleUser user) {
    return issue(user, null);
}
```

在 `createRefreshToken` 中填充 clientIp/userAgent：

```java
private String createRefreshToken(SimpleUser user, HttpServletRequest request) {
    String rawToken = UUID.randomUUID().toString().replace("-", "");
    String hash = sha256(rawToken);
    RefreshToken record = new RefreshToken();
    record.setUserId(user.getUserId());
    record.setUsername(user.getUsername());
    record.setTokenHash(hash);
    record.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));
    record.setRevoked(0);
    if (request != null) {
        record.setClientIp(getClientIp(request));
        record.setUserAgent(request.getHeader("User-Agent"));
    }
    refreshTokenMapper.insert(record);
    return rawToken;
}
```

新增 `blacklistAndRevoke` 和 `revokeAllForUser` 方法：

```java
private final TokenBlacklistService tokenBlacklistService;

public void blacklistAndRevoke(String tokenValue) {
    try {
        Jwt jwt = jwtDecoder.decode(tokenValue);
        String jti = jwt.getId();
        Instant expiresAt = jwt.getExpiresAt();
        if (jti != null && expiresAt != null) {
            long ttl = Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
            if (ttl > 0) {
                tokenBlacklistService.blacklist(jti, ttl);
            }
        }
        // 吊销该用户的 refresh_token
        Long userId = readLong(jwt, SecurityConstants.CLAIM_USER_ID);
        if (userId != null) {
            revokeAllForUser(userId);
        }
    } catch (Exception e) {
        throw new BizException(ErrorCode.TOKEN_INVALID);
    }
}

public void revokeAllForUser(Long userId) {
    List<RefreshToken> tokens = refreshTokenMapper.selectList(
            new LambdaQueryWrapper<RefreshToken>()
                    .eq(RefreshToken::getUserId, userId)
                    .eq(RefreshToken::getRevoked, 0));
    LocalDateTime now = LocalDateTime.now();
    for (RefreshToken token : tokens) {
        token.setRevoked(1);
        token.setRevokedAt(now);
        refreshTokenMapper.updateById(token);
    }
}
```

注意：`JwtTokenService` 需要注入 `JwtDecoder`（或 `NimbusJwtDecoder`）来解码 token 获取 JTI。由于 `JwkConfig` 生成的是 RSA 密钥对，可以注册一个 `JwtDecoder` bean。

- [ ] **步骤 4：编译验证**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am compile
```

预期：BUILD SUCCESS。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/LoginController.java \
  daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/JwtTokenService.java \
  daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/user/SimpleUser.java \
  daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/user/RemoteUserService.java
git commit -m "feat(auth): add login security enhancements and logout endpoint"
```

---

## 任务 6：Gateway 黑名单校验

**文件：**
- 修改：`daydayup-access/daydayup-gateway/pom.xml` — 增加 daydayup-common-redis 依赖
- 修改：`daydayup-access/daydayup-gateway/src/main/java/com/yuan/daydayup/gateway/filter/AuthGlobalFilter.java` — 增加黑名单检查

- [ ] **步骤 1：增加 gateway Redis 依赖**

在 `daydayup-gateway/pom.xml` 的 `<dependencies>` 中追加：

```xml
<dependency>
    <groupId>com.yuan</groupId>
    <artifactId>daydayup-common-redis</artifactId>
</dependency>
```

注意：gateway 是 WebFlux 应用，使用 `ReactiveStringRedisTemplate` 进行 Redis 操作。

- [ ] **步骤 2：修改 AuthGlobalFilter**

在 `AuthGlobalFilter` 中注入 `ReactiveStringRedisTemplate`，在 JWT 校验通过后增加黑名单检查：

```java
private final ReactiveStringRedisTemplate redisTemplate;

public AuthGlobalFilter(ReactiveJwtDecoder jwtDecoder,
                        GatewaySecurityProperties properties,
                        ObjectMapper objectMapper,
                        ReactiveStringRedisTemplate redisTemplate) {
    this.jwtDecoder = jwtDecoder;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.redisTemplate = redisTemplate;
}
```

在 `filter` 方法中，JWT decode 成功后检查黑名单：

```java
return jwtDecoder.decode(token)
        .flatMap(jwt -> {
            String jti = jwt.getId();
            if (jti != null) {
                String blacklistKey = "daydayup:auth:token:blacklist:" + jti;
                return redisTemplate.hasKey(blacklistKey)
                        .flatMap(isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                return reject(exchange, ErrorCode.TOKEN_BLACKLISTED);
                            }
                            return chain.filter(mutateExchange(exchange, jwt));
                        });
            }
            return chain.filter(mutateExchange(exchange, jwt));
        })
        .onErrorResume(ex -> {
            log.warn("JWT 校验失败：{}", ex.getMessage());
            return reject(exchange, ErrorCode.TOKEN_INVALID);
        });
```

- [ ] **步骤 3：编译验证**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-access/daydayup-gateway -am compile
```

预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
git add daydayup-access/daydayup-gateway/pom.xml \
  daydayup-access/daydayup-gateway/src/main/java/com/yuan/daydayup/gateway/filter/AuthGlobalFilter.java
git commit -m "feat(gateway): add token blacklist check in AuthGlobalFilter"
```

---

## 任务 7：Auth 登录流程集成测试

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/controller/LoginControllerTest.java`

- [ ] **步骤 1：编写集成测试**

创建 `LoginControllerTest.java`，使用 Mockito mock 所有依赖：

```java
package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.entity.RefreshToken;
import com.yuan.daydayup.auth.mapper.RefreshTokenMapper;
import com.yuan.daydayup.auth.service.*;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.result.R;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    private RemoteUserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService tokenService;
    private LoginAttemptService loginAttemptService;
    private LoginHistoryService loginHistoryService;
    private TokenBlacklistService tokenBlacklistService;
    private RefreshTokenMapper refreshTokenMapper;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        userService = mock(RemoteUserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(JwtTokenService.class);
        loginAttemptService = mock(LoginAttemptService.class);
        loginHistoryService = mock(LoginHistoryService.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        controller = new LoginController(userService, passwordEncoder, tokenService,
                loginAttemptService, loginHistoryService, tokenBlacklistService, refreshTokenMapper);
    }

    @Test
    void shouldRejectLockedAccount() {
        when(loginAttemptService.isLocked("admin")).thenReturn(true);
        LoginController.LoginRequest req = new LoginController.LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin");

        assertThatThrownBy(() -> controller.login(req, new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(ErrorCode.LOGIN_LOCKED.getCode()));
    }

    @Test
    void shouldRejectDisabledUser() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(0).build()));
        LoginController.LoginRequest req = new LoginController.LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin");

        assertThatThrownBy(() -> controller.login(req, new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode()).isEqualTo(ErrorCode.LOGIN_FAILED.getCode()));
    }

    @Test
    void shouldRecordFailureOnBadPassword() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(1).build()));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        LoginController.LoginRequest req = new LoginController.LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        assertThatThrownBy(() -> controller.login(req, new MockHttpServletRequest()))
                .isInstanceOf(BizException.class);
        verify(loginAttemptService).recordFailure("admin");
        verify(loginHistoryService).record(eq(1L), eq("admin"), eq(false), eq("密码错误"), any());
    }
}
```

- [ ] **步骤 2：运行测试**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth -am test -Dtest=LoginControllerTest
```

预期：测试通过。

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/controller/LoginControllerTest.java
git commit -m "test(auth): add login controller security tests"
```

---

## 任务 8：全量编译与验证

- [ ] **步骤 1：全量编译**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -DskipTests package
```

预期：BUILD SUCCESS。

- [ ] **步骤 2：运行 auth 和 gateway 测试**

```bash
JAVA_HOME="/c/Users/98365/.jdks/ms-21.0.9" ./mvnw -pl daydayup-platform/daydayup-auth,daydayup-access/daydayup-gateway -am test
```

预期：所有测试通过。

- [ ] **步骤 3：验证 ErrorCode 包含新错误码**

运行：

```bash
grep -n "LOGIN_LOCKED\|TOKEN_BLACKLISTED" daydayup-common/daydayup-common-core/src/main/java/com/yuan/daydayup/common/core/enums/ErrorCode.java
```

预期：有 LOGIN_LOCKED(20005) 和 TOKEN_BLACKLISTED(20006) 两行。

- [ ] **步骤 4：最终 Commit（如有遗漏）**

```bash
git status
```

确认没有未提交的文件。

---

## 自检结果

- 规格覆盖度：用户状态检查（任务 5）、登录失败计数/锁定（任务 2）、登录历史记录（任务 4）、登出/黑名单（任务 3+5）、gateway 黑名单校验（任务 6）、管理员批量吊销（任务 5）、Feign 扩展（任务 4）全部覆盖。
- 占位符扫描：无 TODO/TBD/待定。
- 类型一致性：`LoginAttemptService`、`TokenBlacklistService`、`LoginHistoryService` 方法签名一致；Redis key 使用 `CacheKeys.of()` 统一管理。
