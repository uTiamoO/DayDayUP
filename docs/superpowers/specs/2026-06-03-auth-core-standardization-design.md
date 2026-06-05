# Auth 认证内核标准化设计（SAS 接入）

## 1. 背景与目标

DayDayUP 的 auth 服务当前具备基本的 JWT 签发、刷新、登出（黑名单）、登录锁定与历史记录能力，但其 OAuth2 端点全部为自定义实现（`LoginController`），不是标准 OAuth2.1/OIDC 授权服务器。无法满足以下需求：

- 外部第三方应用接入（需要标准授权码流程、客户端注册、consent 授权同意）
- 内部系统单点登录（需要标准 OIDC 发现、`id_token`）
- 统一认证平台定位（需要 `.well-known/openid-configuration`、`/userinfo`、`/introspect`）

**本次目标**：将 auth 服务从自定义 JWT 签发器升级为标准 OAuth2.1 + OIDC 授权服务器，采用 Spring Authorization Server（SAS）框架。

**本次同时完成**：将用户身份与 RBAC 数据（`sys_user`/`sys_role`/`sys_permission` 及关联表）从 `daydayup_admin` 库迁移到 `daydayup_auth` 库，使 auth 成为独立的身份中心。

**本次不包含**：

- 第三方社会化登录（微信/QQ/GitHub OAuth）—— 独立子项目
- 统一消息中心（短信/邮件验证码）—— 独立子项目
- SSO 跨系统单点登出（SLO）—— 依赖本项目完成后推进
- 多因素认证（MFA）—— 依赖统一消息中心

---

## 2. 现状分析

### 2.1 当前 auth 端点

| 端点 | 方式 | 备注 |
|------|------|------|
| `POST /oauth2/token` | 账号密码直接签 JWT | `LoginController`，非标准 OAuth2 |
| `POST /oauth2/refresh` | 随机串 refresh_token 轮换 | `JwtTokenService.refresh()` |
| `POST /oauth2/logout` | Redis 黑名单 + 吊销 refresh_token | `TokenBlacklistService` |
| `POST /oauth2/revoke/{userId}` | 管理员强制吊销 | 需 `admin:*` 权限 |
| `GET /.well-known/jwks.json` | RSA 公钥端点 | `JwksController`，当前网关默认从该路径拉取公钥 |

### 2.2 当前依赖

- `spring-boot-starter-security`（基础 Security）
- `spring-security-oauth2-jose`（JWT 编解码，**无 SAS**）
- `AuthSecurityConfig`：`anyRequest().permitAll()` + 无状态会话
- `JwtTokenService`：手写 RS256 JWT 签发，claims 为 `iss/iat/exp/jti/sub/uid/username/authorities`，当前 `iss` 为 `daydayup-auth`，无 `aud` claim

### 2.3 身份数据现状

| 表 | 当前库 | 迁移后归属 |
|----|--------|-----------|
| `sys_user` | `daydayup_admin` | → `daydayup_auth` |
| `sys_role` | `daydayup_admin` | → `daydayup_auth` |
| `sys_user_role` | `daydayup_admin` | → `daydayup_auth` |
| `sys_permission` | `daydayup_admin` | → `daydayup_auth` |
| `sys_role_permission` | `daydayup_admin` | → `daydayup_auth` |
| `sys_menu` | `daydayup_admin` | 不动 |
| `sys_dict` / `sys_dict_item` | `daydayup_admin` | 不动 |
| `sys_oper_log` | `daydayup_admin` | 不动，保留 `user_id` / `username` 作为历史冗余字段 |
| `sys_api_key` | `daydayup_admin` | 暂不迁移；当前 SQL 脚本中需补充确认其 DDL 归属 |

> 数据库约束原则：DayDayUP 当前 SQL 不设置显式 `FOREIGN KEY` / `REFERENCES` 约束，后续迁移也保持这一原则。用户、角色、权限、菜单、操作日志之间只通过业务代码、唯一索引、普通索引和服务接口做逻辑一致性校验，避免跨库外键和微服务边界耦合。

### 2.4 当前服务调用现状

- auth 当前通过 Feign 调用 `daydayup-admin-biz` 的 `/users/auth/{username}` 获取密码哈希、用户状态和权限码。
- admin-biz 当前本地操作 `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`。
- gateway 当前通过 `/.well-known/jwks.json` 拉取 auth 公钥，校验 JWT 后透传 `X-User-Id`、`X-User-Name`、`authorities`。

---

## 3. 架构设计

### 3.1 SAS 接入后端点全景

```
                         daydayup-auth
              ┌──────────────────────────────────────┐
              │                                      │
              │  ┌─ SAS OAuth2 协议端点 ──────────┐  │
              │  │ /oauth2/authorize   (授权码)    │  │
              │  │ /oauth2/token       (令牌端点)  │  │
              │  │ /oauth2/revoke      (令牌吊销)  │  │
              │  │ /oauth2/introspect  (令牌内省)  │  │
              │  │ /oauth2/jwks        (公钥)      │  │
              │  │ /.well-known/openid-configuration │  │
              │  │ /userinfo           (OIDC)      │  │
              │  └────────────────────────────────┘  │
              │                                      │
              │  ┌─ 登录/consent UI ───────────────┐  │
              │  │ /login              (表单登录)   │  │
              │  │ /oauth2/consent     (授权同意)   │  │
              │  └────────────────────────────────┘  │
              │                                      │
              │  ┌─ 用户管理 API ──────────────────┐  │
              │  │ /api/users/**       (CRUD)      │  │
              │  │ /api/roles/**       (CRUD)      │  │
              │  │ /api/permissions/** (CRUD)      │  │
              │  └────────────────────────────────┘  │
              │                                      │
              └──────────────────────────────────────┘
                         ↕ Nacos 注册
        ┌─────────────────┼──────────────────┐
   daydayup-gateway   daydayup-admin     外部第三方
   JWT校验+黑名单     Feign→auth 用户     授权码+PKCE
                     管理API+菜单UI       +consent
```

### 3.2 用户管理 API 归属

用户/角色/权限的 CRUD API 从 `admin-biz` 迁到 `auth` 服务（因为数据在 auth 库）。

- **auth** 新增 `UserManageController`（`/api/users/**`）、`RoleManageController`（`/api/roles/**`）、`PermissionManageController`（`/api/permissions/**`）
- **admin-biz** 过渡期保留原 `UserManageController`、`RoleController`、`PermissionController` 的对外路径，Service 层改为 Feign Client 调用 auth 的管理 API，避免前端一次性改造。稳定后再删除本地用户/角色/权限 Mapper、Entity 和 Service 实现。
- **admin-biz 保留**：`MenuController`、`DictController`、`DictItemController`、`OperLogController`、`ApiKeyController`、`CurrentUserController`。其中 `CurrentUserController` 继续基于网关透传的用户上下文返回当前用户信息，确需补充详情时再调用 auth。

---

## 4. 依赖变更

### 4.1 daydayup-auth pom.xml

**新增**：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.4.3</version>
</dependency>
```

**保留**：`spring-boot-starter-security`、`spring-security-oauth2-jose`、`daydayup-common-mybatis`、`daydayup-common-redis`。

> 版本校验：根 POM 当前使用 Spring Boot `3.5.0`、Java `21`，并在 `dependencyManagement` 中声明 SAS `1.4.3`。引入前需要执行依赖树和启动验证，确认 Spring Security 版本与 SAS 版本无冲突；如 SAS 官方已发布与 Spring Security 6.5 更匹配的版本，优先升级到兼容版本。

### 4.2 daydayup-admin-biz pom.xml

**新增**：

```xml
<dependency>
    <groupId>com.yuan</groupId>
    <artifactId>daydayup-auth-api</artifactId> <!-- 新建模块 -->
</dependency>
```

---

## 5. 数据模型

### 5.1 新增 SAS 标准表（写入 `daydayup_auth` 库）

SAS 提供 JDBC 实现。实际落库时必须以目标版本 `spring-security-oauth2-authorization-server` 官方 JDBC schema 为准，字段名、字段类型和字段长度不得自行裁剪；本文 DDL 仅作为设计示意，落地前需要与目标依赖版本逐项核对。

数据库约束继续遵循项目约定：**不声明显式外键约束**。例如 `oauth2_authorization.registered_client_id` 与 `oauth2_registered_client.id` 只通过 SAS 的 Repository 代码逻辑保证一致性，不通过数据库 `FOREIGN KEY` 绑定。

#### oauth2_registered_client（OAuth2 客户端注册）

```sql
CREATE TABLE IF NOT EXISTS `oauth2_registered_client` (
    `id`                            VARCHAR(100)  NOT NULL COMMENT '主键',
    `client_id`                     VARCHAR(100)  NOT NULL COMMENT '客户端 ID',
    `client_id_issued_at`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    `client_secret`                 VARCHAR(200)  DEFAULT NULL COMMENT '客户端密钥（BCrypt）',
    `client_secret_expires_at`      TIMESTAMP     DEFAULT NULL COMMENT '密钥过期时间',
    `client_name`                   VARCHAR(200)  NOT NULL COMMENT '应用名称',
    `client_authentication_methods` VARCHAR(1000) NOT NULL COMMENT '认证方式：client_secret_basic,client_secret_post,none',
    `authorization_grant_types`     VARCHAR(1000) NOT NULL COMMENT '授权类型：authorization_code,client_credentials,refresh_token,password',
    `redirect_uris`                 VARCHAR(1000) DEFAULT NULL COMMENT '回调地址',
    `post_logout_redirect_uris`     VARCHAR(1000) DEFAULT NULL COMMENT '登出回调',
    `scopes`                        VARCHAR(1000) NOT NULL COMMENT '允许的 scope',
    `client_settings`               TEXT          NOT NULL COMMENT '客户端配置 JSON（require-authorization-consent,require-proof-key 等）',
    `token_settings`                TEXT          NOT NULL COMMENT '令牌配置 JSON（access-token-ttl,refresh-token-ttl 等）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 客户端注册';
```

#### oauth2_authorization（授权记录）

```sql
CREATE TABLE IF NOT EXISTS `oauth2_authorization` (
    `id`                            VARCHAR(100)  NOT NULL COMMENT '主键',
    `registered_client_id`          VARCHAR(100)  NOT NULL COMMENT '客户端 ID',
    `principal_name`                VARCHAR(200)  NOT NULL COMMENT '用户名',
    `authorization_grant_type`      VARCHAR(100)  NOT NULL COMMENT '授权类型',
    `authorized_scopes`             VARCHAR(1000) DEFAULT NULL COMMENT '已授权 scope',
    `attributes`                    TEXT          DEFAULT NULL COMMENT '扩展属性 JSON',
    `state`                         VARCHAR(500)  DEFAULT NULL COMMENT 'state 参数',
    `authorization_code_value`      TEXT          DEFAULT NULL COMMENT '授权码',
    `authorization_code_issued_at`  TIMESTAMP     DEFAULT NULL,
    `authorization_code_expires_at` TIMESTAMP     DEFAULT NULL,
    `authorization_code_metadata`   TEXT          DEFAULT NULL,
    `access_token_value`            TEXT          DEFAULT NULL COMMENT 'access_token',
    `access_token_issued_at`        TIMESTAMP     DEFAULT NULL,
    `access_token_expires_at`       TIMESTAMP     DEFAULT NULL,
    `access_token_metadata`         TEXT          DEFAULT NULL,
    `access_token_type`             VARCHAR(100)  DEFAULT NULL,
    `access_token_scopes`           VARCHAR(1000) DEFAULT NULL,
    `oidc_id_token_value`           TEXT          DEFAULT NULL COMMENT 'OIDC id_token',
    `oidc_id_token_issued_at`       TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_expires_at`      TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_metadata`        TEXT          DEFAULT NULL,
    `refresh_token_value`           TEXT          DEFAULT NULL COMMENT 'refresh_token',
    `refresh_token_issued_at`       TIMESTAMP     DEFAULT NULL,
    `refresh_token_expires_at`      TIMESTAMP     DEFAULT NULL,
    `refresh_token_metadata`        TEXT          DEFAULT NULL,
    `user_code_value`               TEXT          DEFAULT NULL,
    `user_code_issued_at`           TIMESTAMP     DEFAULT NULL,
    `user_code_expires_at`          TIMESTAMP     DEFAULT NULL,
    `user_code_metadata`            TEXT          DEFAULT NULL,
    `device_code_value`             TEXT          DEFAULT NULL,
    `device_code_issued_at`         TIMESTAMP     DEFAULT NULL,
    `device_code_expires_at`        TIMESTAMP     DEFAULT NULL,
    `device_code_metadata`          TEXT          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 授权记录';
```

#### oauth2_authorization_consent（用户授权同意）

```sql
CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent` (
    `registered_client_id` VARCHAR(100)  NOT NULL,
    `principal_name`       VARCHAR(200)  NOT NULL,
    `authorities`          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户授权同意记录';
```

### 5.2 迁移进 auth 库的表

从 `daydayup_admin.sql` 中将以下表的 DDL + 种子数据搬入 `daydayup_auth.sql`（字段尽量保持不变，索引保留，仍不新增数据库外键）：

- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_permission`
- `sys_role_permission`

从 `daydayup_admin.sql` 中**删除**这 5 张表的 DDL 和种子数据。

迁移后的逻辑关系如下：

| 关系 | 一致性保障方式 | 说明 |
|------|----------------|------|
| 用户 → 角色 | 代码校验 `sys_user_role.user_id` / `role_id` | 不使用数据库外键；新增或分配角色时由 auth Service 校验用户和角色存在。 |
| 角色 → 权限 | 代码校验 `sys_role_permission.role_id` / `permission_id` | 不使用数据库外键；授权时由 auth Service 校验角色和权限存在。 |
| 菜单 → 权限码 | `sys_menu.permission_code` 字符串弱关联 | `sys_menu` 留在 admin 库，不跨库外键；admin 可通过 Feign 查询或缓存 auth 权限码做有效性校验。 |
| 操作日志 → 用户 | `sys_oper_log.user_id` + `username` 冗余 | 日志留在 admin 库，不做跨库 join；展示时优先使用日志内冗余用户名。 |
| API Key → 用户 | 待确认 DDL 后决定归属 | 当前代码已有 API Key 管理能力，但当前 SQL 需补充确认 `sys_api_key` 表定义。 |

### 5.3 OAuth2 客户端种子数据

客户端种子数据不建议直接手写复杂的 `client_settings` / `token_settings` JSON。SAS JDBC 实现会按框架版本序列化配置对象，手写 JSON 容易在版本升级后出现反序列化失败。

推荐在启动初始化逻辑中使用 `RegisteredClient` 构建后调用 `JdbcRegisteredClientRepository.save()` 写入数据库。首批内置客户端如下：

| client_id | 客户端类型 | 认证方式 | 授权类型 | 说明 |
|-----------|------------|----------|----------|------|
| `admin-web` | Public Client | `none` | `authorization_code`、`refresh_token` | 管理后台前端，强制 PKCE，不启用 password grant。 |
| `admin-password-legacy` | 内部兼容客户端 | `client_secret_basic` | `password`、`refresh_token` | 仅用于旧内部系统兼容，后续废弃，不开放给第三方或浏览器前端。 |
| `internal-service` | Confidential Client | `client_secret_basic` | `client_credentials` | 服务间调用；不配置 refresh token，需要时重新用客户端凭证换取 access token。 |

示意代码：

```java
RegisteredClient adminWeb = RegisteredClient.withId("admin-web")
        .clientId("admin-web")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("http://localhost:5173/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .clientSettings(ClientSettings.builder()
                .requireProofKey(true)
                .requireAuthorizationConsent(false)
                .build())
        .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .build())
        .build();
```

> 外部第三方应用的客户端注册在 Phase 4 通过管理 API 动态注册，不在此处种子化。

---

## 6. 令牌设计

### 6.1 access_token（JWT，RS256）

SAS 签发的 JWT 标准 claim + 自定义扩展（通过 `OAuth2TokenCustomizer<JwtEncodingContext>`）：

```json
{
  "iss": "http://127.0.0.1:9200",
  "sub": "1",
  "aud": ["admin-web"],
  "exp": 1749052800,
  "iat": 1749045600,
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  "scope": "openid profile admin:*",
  "authorities": ["admin:*", "user:read", "user:write"],
  "uid": 1,
  "preferred_username": "admin"
}
```

- `iss`：auth 服务的发行者地址（可在 Nacos 动态配置），本地默认与当前服务端口 `9200` 对齐。
- `sub`：用户稳定主体标识，统一使用用户 ID 字符串，access token 与 id token 保持一致。
- `aud`：客户端 ID（SAS 默认）。
- `authorities`：用户权限码列表（自定义扩展，读取 `sys_role_permission`，由代码逻辑校验角色与权限关系）。
- `uid`：用户 ID（自定义扩展，用于网关透传和业务服务）。
- `preferred_username`：用户名，供前端展示和网关兼容读取。

### 6.2 id_token（OIDC，JWT）

当 scope 包含 `openid` 时，SAS 自动签发 id_token：

```json
{
  "iss": "http://127.0.0.1:9200",
  "sub": "1",
  "aud": "admin-web",
  "exp": 1749052800,
  "iat": 1749045600,
  "auth_time": 1749045590,
  "nonce": "n-0S6_WzA2Mj"
}
```

### 6.3 refresh_token

SAS 内置 opaque token（非 JWT），存储在 `oauth2_authorization` 表，支持一次性轮换（`reuse-refresh-tokens: false`）。

### 6.4 与当前令牌兼容

当前签发的自定义 JWT 仍在网关通过 `JwtDecoder` 验签（RSA 公钥相同）。SAS 接入后新签发的 JWT claims 结构变化，网关需兼容读取 `username`、`preferred_username` 和 `sub`：

1. `uid` 优先从 `uid` claim 读取；若未来移除 `uid`，再从 `sub` 转换。
2. 用户名优先读取 `username`，其次读取 `preferred_username`，最后回退到 `sub`。
3. 权限优先读取 `authorities`，必要时再兼容 `scope` / `scp`。
4. 过渡期两种 JWT 并存，通过 `iss` claim 区分：旧 token 的 `iss` 为 `daydayup-auth`，新 token 的 `iss` 为配置化 URL。

---

## 7. 自定义 password grant 扩展

SAS 不内置 password grant（OAuth2.1 废弃）。为兼容内部系统，自定义实现。

### 7.1 PasswordAuthenticationConverter

```java
/**
 * 从 /oauth2/token 请求中识别 grant_type=password，
 * 转换为 PasswordAuthenticationToken 交给 Provider 处理。
 */
public class PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // 1. 判断 grant_type=password
        // 2. 从请求头或 Basic Auth 提取 client_id
        // 3. 提取 username / password
        // 4. 返回 PasswordAuthenticationToken(username, password, clientPrincipal, scopes)
    }
}
```

### 7.2 PasswordAuthenticationProvider

```java
/**
 * 校验用户名密码，走 SAS 标准签发流程。
 *
 * 关键：复用 SAS 的 UserDetailsService 和 PasswordEncoder，
 * 与授权码登录共用同一套用户校验逻辑。
 */
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<?> tokenGenerator;

    @Override
    public Authentication authenticate(Authentication authentication) {
        // 1. 校验客户端已认证且允许 password grant
        // 2. 调 UserDetailsService.loadUserByUsername()
        // 3. 校验密码（passwordEncoder.matches）
        // 4. 构建 OAuth2Authorization
        // 5. 生成 access_token + refresh_token + id_token（如 scope 含 openid）
        // 6. 返回 OAuth2AccessTokenAuthenticationToken
    }
}
```

### 7.3 注册到 SAS

```java
@Configuration
public class AuthorizationServerConfig {

    @Bean
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            RegisteredClientRepository clientRepository,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServer =
            OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.securityMatcher(authorizationServer.getEndpointsMatcher())
            .with(authorizationServer, config -> config
                .tokenEndpoint(token -> token
                    // 在 SAS 默认的 converter 链之前插入自定义 password converter
                    .accessTokenRequestConverter(new PasswordAuthenticationConverter())
                    .authenticationProvider(new PasswordAuthenticationProvider(
                        userDetailsService, passwordEncoder,
                        authorizationService, tokenGenerator))
                )
            );
        // ... 其他 SAS 配置
    }
}
```

---

## 8. SAS 核心配置

### 8.1 AuthorizationServerConfig

```java
@Configuration
public class AuthorizationServerConfig {

    // 1. RegisteredClientRepository：JDBC 实现，读 oauth2_registered_client 表
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    // 2. OAuth2AuthorizationService：JDBC 实现
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository clientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, clientRepository);
    }

    // 3. OAuth2AuthorizationConsentService：JDBC 实现
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository clientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clientRepository);
    }

    // 4. JWKSource：复用现有的 RSA 密钥对（从 DB 加载，与当前 JwkConfig 一致）
    @Bean
    public JWKSource<SecurityContext> jwkSource(JwkKeyProvider jwkKeyProvider) {
        RSAKey rsaKey = jwkKeyProvider.getActiveRsaKey();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    // 5. JWT 自定义：注入 uid / authorities claim
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                Authentication principal = context.getPrincipal();
                // 从 UserDetails 中提取 uid 和 authorities，写入 JWT claims
                context.getClaims().claim("uid", extractUid(principal));
                context.getClaims().claim("authorities", extractAuthorities(principal));
            }
        };
    }

    // 6. ProviderSettings：issuer URL（可从 Nacos 配置读取）
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://127.0.0.1:9200")
                .build();
    }
}
```

### 8.2 AuthSecurityConfig 改造

当前 `AuthSecurityConfig` 将所有请求 `permitAll()`，SAS 接入后需要拆分为协议端点、管理 API 和静态/文档端点三类安全边界：

```java
@Configuration
public class AuthSecurityConfig {

    // Chain 1：SAS OAuth2 协议端点（由 SAS 框架接管）
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
        // SAS 默认配置：/oauth2/*, /.well-known/* 等
        // 应用 OAuth2AuthorizationServerConfigurer
        // 允许 /login 页面公开访问
    }

    // Chain 2：管理 API（auth 自身作为 Resource Server 校验 Bearer Token）
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) {
        // /api/users/**, /api/roles/**, /api/permissions/** 需要 Bearer Token 认证
        // 根据 authorities / scope 做方法级或路径级授权
    }

    // Chain 3：文档、健康检查等公开端点
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
        // /swagger-ui/**, /v3/api-docs/**, /actuator/health 公开
    }
}
```

### 8.3 UserDetailsService

```java
@Service
public class DayDayUpUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) {
        // 1. 查 sys_user（本地 auth 库，不再 Feign）
        // 2. 查 sys_user_role + sys_role + sys_role_permission 得到权限码
        // 3. 返回 UserDetails（实现含 uid / status 等扩展字段）
    }
}
```

---

## 9. Gateway 集成

### 9.1 JWT 校验（基本不变）

RSA 密钥对不变，但网关的 JWK Set URI 需要随端点迁移同步调整。当前网关默认从 `/.well-known/jwks.json` 拉取公钥；SAS 接入后标准端点为 `/oauth2/jwks`。过渡期建议同时保留旧路径和新路径，待网关配置切换完成后再删除旧 `JwksController`。

### 9.2 Claim 解析适配

当前 `AuthGlobalFilter` 从 JWT 读取 `uid`、`username`、`authorities` claim 并设置到请求头。SAS JWT 的 claim 结构变化：

| 当前 claim | SAS JWT claim | 改动 |
|-----------|---------------|------|
| `uid` | `uid`（OAuth2TokenCustomizer 注入） | 不变 |
| `username` | `preferred_username` / `sub` | 过渡期优先读 `username`，其次读 `preferred_username`，最后回退到 `sub` |
| `authorities` | `authorities`（OAuth2TokenCustomizer 注入） | 不变 |

`AuthGlobalFilter` 不应只做简单的 `username` → `sub` 替换，而应在过渡期兼容新旧 token：

- 用户 ID：优先读取 `uid`，必要时再从 `sub` 转换。
- 用户名：优先读取 `username`，其次读取 `preferred_username`，最后回退到 `sub`。
- 权限：优先读取 `authorities`，必要时兼容 `scope` / `scp`。

### 9.3 黑名单校验（不变）

Redis 黑名单 key 前缀保持当前实现：`daydayup:auth:token:blacklist:{jti}`。SAS JWT 同样有 `jti` claim，登出时继续将当前 access token 的 `jti` 写入 Redis，TTL 使用 token 剩余有效期。

### 9.4 introspect（可选增强）

SAS 内置 `/oauth2/introspect` 端点，网关可通过内省验证 opaque token（如 refresh_token 直接当 access_token 用的场景）。Phase 1 暂不启用，Phase 2 按需接入。

---

## 10. Admin 集成改造

### 10.1 新建 daydayup-auth-api 模块

```
daydayup-platform/
├── daydayup-auth/
│   └── ...（SAS 授权服务器 + 用户管理 API）
├── daydayup-auth-api/     ← 新建
│   └── src/main/java/com/yuan/daydayup/auth/api/
│       ├── client/
│       │   ├── UserManageClient.java    (Feign 接口)
│       │   ├── RoleManageClient.java    (Feign 接口)
│       │   └── PermissionManageClient.java (Feign 接口)
│       ├── dto/
│       │   ├── UserDTO.java
│       │   ├── RoleDTO.java
│       │   └── ...
│       └── query/
│           ├── UserPageQuery.java
│           └── ...
```

### 10.2 admin-biz 改造

1. **过渡期保留 Controller**：`UserManageController`、`RoleController`、`PermissionController` 的外部路径先不变，避免前端和网关路由一次性调整。
2. **Service 改为 Feign**：原 `UserManageService`、`RoleService`、`PermissionService` 内部改为调用 auth 的管理 API。稳定后再删除本地 Mapper、Entity 和本地实现。
3. **新增依赖**：admin-biz 引入 `daydayup-auth-api`；如当前模块没有直接 OpenFeign 依赖，需要同步补充或通过公共模块传递。
4. **保留模块**：`MenuController`、`DictController`、`DictItemController`、`OperLogController`、`ApiKeyController`、`CurrentUserController`。
5. **菜单/权限码**：`sys_menu.permission_code` 引用的权限码现在 auth 库里，admin 查询菜单时通过 Feign 校验权限码有效性（或缓存）。数据库不设置跨库外键。

### 10.3 auth 用户管理 API 端点设计

遵循 admin-api-design.md 中已定义的 RESTful 约定（分页列表、详情、新增、修改、状态变更、逻辑删除），接口路径统一加 `/api` 前缀以区分 OAuth2 协议端点：

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/users/page` | 用户分页列表 |
| `GET` | `/api/users/{id}` | 用户详情 |
| `POST` | `/api/users` | 新增用户 |
| `PUT` | `/api/users/{id}` | 修改用户 |
| `PATCH` | `/api/users/{id}/status` | 启用/停用 |
| `DELETE` | `/api/users/{id}` | 逻辑删除 |
| `PATCH` | `/api/users/{id}/password` | 重置密码 |
| `PATCH` | `/api/users/{id}/login-info` | 更新最后登录信息 |
| `GET` | `/api/roles/page` | 角色分页列表 |
| `POST` | `/api/roles` | 新增角色 |
| `PUT` | `/api/roles/{id}` | 修改角色 |
| `DELETE` | `/api/roles/{id}` | 逻辑删除 |
| `POST` | `/api/users/{id}/roles` | 分配角色 |
| `GET` | `/api/permissions/page` | 权限分页列表 |
| `POST` | `/api/permissions` | 新增权限 |

---

## 11. 登录页面与 Consent 页面

### 11.1 登录页面（`/login`）

SAS 要求授权码流程时重定向到 `/login`（标准 Spring Security 表单登录页）。

授权码流程需要 auth 服务提供 `/login` 页面。Phase 3 提供服务端渲染的简洁 HTML 表单（Thymeleaf 或纯 HTML），包含用户名/密码输入和 CSRF token。不做前端 UI 框架适配（留给前端子项目）。

内部 admin 前端仍可使用自己的登录页，并通过授权码 + PKCE 跳转 auth；旧 password 登录仅作为兼容入口，不作为长期推荐方案。

### 11.2 Consent 授权同意页面（`/oauth2/consent`）

当客户端配置 `require-authorization-consent: true` 时，SAS 重定向用户到 consent 页面，展示应用请求的 scope 列表，用户确认或拒绝。

Phase 3 提供基础 HTML 表单实现。consent 记录存入 `oauth2_authorization_consent` 表，用户同意后下次不再弹出（除非 scope 变更）。

---

## 12. 安全考虑

| 项目 | 措施 |
|------|------|
| 客户端密钥存储 | BCrypt 哈希，与用户密码同等保护 |
| PKCE | 内部 Web 客户端强制要求（`require-proof-key: true`），防止授权码拦截 |
| password grant 安全 | 仅限 `client_id` 明确注册了 `password` grant 的内部客户端，且不开放给外部应用 |
| 授权码有效期 | 默认 5 分钟，一次性使用 |
| refresh_token 轮换 | `reuse-refresh-tokens: false`，每次刷新产生新 refresh_token，旧的立即失效 |
| 黑名单 | 保留现有 Redis 黑名单机制，登出时 `jti` 入黑名单；管理员强制下线优先使用用户级 `revokedAt` / 权限版本时间让旧 token 失效 |
| CORS | 由网关层统一处理（现有 `CorsSecurityWebFilter`） |
| 管理 API 鉴权 | auth 的 `/api/**` 不能继续 `permitAll`，必须作为 Resource Server 校验 Bearer Token，并按权限码授权 |
| 数据一致性 | 数据库不设置显式外键，用户、角色、权限、菜单等关系通过 Service 层代码逻辑校验 |

---

## 13. 实施阶段

### Phase 0：兼容性与基线验证

- 核对 SAS 版本与 Spring Boot `3.5.0` / Spring Security 版本兼容性。
- 引入最小 SAS 配置后执行编译、启动和端点冒烟验证。
- 明确 `issuer`、JWK Set URI、旧 JWKS 路径兼容策略。
- 明确旧 `/oauth2/token`、`/oauth2/refresh`、`/oauth2/logout`、`/oauth2/revoke/{userId}` 的迁移策略。

### Phase 1：身份数据迁移 + auth 本地用户体系

- `daydayup_auth.sql` 新增 5 张身份表（`sys_user/role/permission + 关联表`）+ 种子数据。
- `daydayup_admin.sql` 删除对应 5 张表。
- 保留普通索引和唯一索引，但不新增 `FOREIGN KEY`；关系一致性通过 auth Service 代码校验。
- auth 新增 Entity / Mapper / Service（从 admin-biz 搬过来）。
- 实现 `DayDayUpUserDetailsService`（本地查 auth 库）。
- auth 实现用户管理 API（`/api/users/**`、`/api/roles/**`、`/api/permissions/**`）。
- 新建 `daydayup-auth-api` 模块（Feign 接口 + DTO）。
- admin-biz 过渡期保留原 Controller 路径，Service 层改为 Feign 调用 auth 管理 API。
- 验证：admin 后台通过 Feign 调用 auth 管理 API 正常工作；旧登录链路仍可用。

### Phase 2：SAS 基础接入 + password 兼容

- 引入 SAS 依赖，编写 `AuthorizationServerConfig`。
- SAS 标准建表（`oauth2_registered_client`、`oauth2_authorization`、`oauth2_authorization_consent`）。
- 注册内部客户端种子数据，优先通过 `RegisteredClientRepository.save()` 写入。
- 实现自定义 password grant 扩展（`PasswordAuthenticationConverter` + `PasswordAuthenticationProvider`），仅用于内部兼容客户端。
- `AuthSecurityConfig` 拆分为 SAS 协议链、管理 API 资源服务器链、文档/健康检查公开链。
- 复用现有 RSA 密钥对（`JwkKeyProvider` → `JWKSource`）。
- JWT 自定义：注入 `uid`、`authorities`、`preferred_username` claim。
- `LoginController` 降级为 legacy 兼容端点或移除（由 SAS `/oauth2/token` 接管）。
- 验证：`POST /oauth2/token`（password + client_credentials + refresh_token）均返回标准格式；旧 `/oauth2/refresh` 如保留则仅作为兼容适配层。

### Phase 3：授权码流程 + OIDC

- 实现 `/oauth2/authorize` 端点（SAS 内置，需要配置 SecurityFilterChain 允许授权码流程）
- 实现 `/login` 登录页面（简洁 HTML 表单）
- 实现 `/oauth2/consent` 授权同意页面
- PKCE 支持（客户端 `require-proof-key: true`）
- OIDC 发现（`/.well-known/openid-configuration`）—— SAS 自动启用
- `/oauth2/jwks` 由 SAS 接管；过渡期保留 `/.well-known/jwks.json` 兼容路径，网关切换后再移除 `JwksController`
- `/userinfo` 端点启用
- 验证：授权码 + PKCE 完整流程，`.well-known` 返回正确配置

### Phase 4：客户端管理

- admin 后台新增 OAuth2 客户端管理页面（CRUD `oauth2_registered_client`）
- auth 新增 `/api/clients/**` 管理 API
- 外部应用通过 admin 后台注册，获取 `client_id` / `client_secret`

### Phase 5：网关适配 + 清理

- 网关 `AuthGlobalFilter` 适配 SAS JWT claim 结构，兼容读取 `username`、`preferred_username`、`sub`
- 移除或废弃 `LoginController`（由 SAS `/oauth2/token` 完全接管）
- 移除 `JwksController`（确认网关已切换到 SAS `/oauth2/jwks` 后执行）
- 更新网关安全配置，兼容新旧 JWT（过渡期）
- 端到端验证：内部系统登录 → 调用受保护 API → 登出 → token 失效

---

## 14. 不在本次范围（后续子项目）

| 子项目 | 说明 |
|--------|------|
| 第三方社会化登录 | auth 作为 OAuth2 客户端对接微信/QQ/GitHub，需新表 `sys_oauth2_social_binding` |
| 统一消息中心 | 独立服务 `daydayup-message`，auth 消费其验证码能力 |
| 多因素认证（MFA） | TOTP/短信验证码，依赖统一消息中心 |
| SSO 跨域单点登录 | 需要 OIDC session management + front-channel logout |
| 统一单点登出（SLO） | 需要 back-channel / front-channel logout 机制 |
| OAuth2 客户端动态注册 | RFC 7591，可在 Phase 4 基础上扩展 |
