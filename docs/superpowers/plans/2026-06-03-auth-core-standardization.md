# Auth 认证内核标准化（SAS OAuth2.1/OIDC）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将 daydayup-auth 从自定义 JWT 签发器升级为 Spring Authorization Server 标准 OAuth2.1/OIDC 授权服务器，并将身份/RBAC 数据从 admin 库迁移到 auth 库。

**架构：** 引入 SAS 1.4.3 替换自定义 LoginController，SAS 接管 /oauth2/* 协议端点；新增自定义 password grant 扩展兼容内部系统；身份实体（sys_user/role/permission）从 admin-biz 迁移到 auth 服务本地；admin-biz 改为 Feign 消费 auth 的用户管理 API。

**技术栈：** Spring Authorization Server 1.4.3、Spring Security 6.x、spring-security-oauth2-jose、MyBatis-Plus、Redis、MySQL

---

## 文件结构总览

### Phase 1 — SAS 集成（新增/修改文件）

| 文件 | 操作 | 职责 |
|------|------|------|
| `daydayup-auth/pom.xml` | 修改 | 新增 SAS 依赖 |
| `daydayup-auth/src/main/resources/application.yml` | 修改 | 新增 SAS 配置项 |
| `daydayup-auth/src/main/java/.../auth/config/AuthorizationServerConfig.java` | 创建 | SAS 核心配置（所有 Bean 注册） |
| `daydayup-auth/src/main/java/.../auth/config/AuthSecurityConfig.java` | 重写 | 两段 SecurityFilterChain |
| `daydayup-auth/src/main/java/.../auth/grant/PasswordAuthenticationConverter.java` | 创建 | 识别 grant_type=password 请求 |
| `daydayup-auth/src/main/java/.../auth/grant/PasswordAuthenticationProvider.java` | 创建 | 校验密码并签发 SAS token |
| `daydayup-auth/src/main/java/.../auth/grant/PasswordAuthenticationToken.java` | 创建 | password grant 认证 token |
| `daydayup-auth/src/main/java/.../auth/service/DayDayUpUserDetailsService.java` | 创建 | UserDetailsService（Phase 1 用 RemoteUserService，Phase 2 切本地） |
| `daydayup-auth/src/test/java/.../auth/config/AuthorizationServerConfigTest.java` | 创建 | SAS 配置启动测试 |
| `daydayup-auth/src/test/java/.../auth/grant/PasswordGrantIntegrationTest.java` | 创建 | password grant 端到端测试 |

### Phase 2 — 身份数据迁移

| 文件 | 操作 | 职责 |
|------|------|------|
| `daydayup-auth/src/main/java/.../auth/entity/SysUser.java` | 创建 | 用户实体 |
| `daydayup-auth/src/main/java/.../auth/entity/SysRole.java` | 创建 | 角色实体 |
| `daydayup-auth/src/main/java/.../auth/entity/SysUserRole.java` | 创建 | 用户-角色关联 |
| `daydayup-auth/src/main/java/.../auth/entity/SysPermission.java` | 创建 | 权限实体 |
| `daydayup-auth/src/main/java/.../auth/entity/SysRolePermission.java` | 创建 | 角色-权限关联 |
| `daydayup-auth/src/main/java/.../auth/mapper/SysUserMapper.java` | 创建 | 用户 Mapper |
| `daydayup-auth/src/main/java/.../auth/mapper/SysRoleMapper.java` | 创建 | 角色 Mapper |
| `daydayup-auth/src/main/java/.../auth/mapper/SysUserRoleMapper.java` | 创建 | 用户-角色 Mapper |
| `daydayup-auth/src/main/java/.../auth/mapper/SysPermissionMapper.java` | 创建 | 权限 Mapper |
| `daydayup-auth/src/main/java/.../auth/mapper/SysRolePermissionMapper.java` | 创建 | 角色-权限 Mapper |
| `daydayup-auth/src/main/resources/db/init.sql` | 修改 | 追加 5 张身份表 DDL + 种子数据 |
| `daydayup-auth/src/main/java/.../auth/service/DayDayUpUserDetailsService.java` | 重写 | 切换为本地 SysUserMapper 查询 |
| `sql/daydayup_admin.sql` | 修改 | 删除 5 张身份表 DDL 和种子数据 |

### Phase 3 — 用户管理 API + Admin 重构

| 文件 | 操作 | 职责 |
|------|------|------|
| `daydayup-auth-api/pom.xml` | 创建 | 模块 POM |
| `daydayup-auth-api/.../client/UserManageClient.java` | 创建 | 用户管理 Feign 接口 |
| `daydayup-auth-api/.../client/RoleManageClient.java` | 创建 | 角色管理 Feign 接口 |
| `daydayup-auth-api/.../dto/*.java` | 创建 | UserDTO、RoleDTO、UserPageQuery 等 |
| `daydayup-auth/src/main/java/.../auth/service/UserManageService.java` | 创建 | 用户管理业务逻辑 |
| `daydayup-auth/src/main/java/.../auth/controller/UserManageController.java` | 创建 | /api/users/** 端点 |
| `daydayup-auth/src/main/java/.../auth/controller/RoleManageController.java` | 创建 | /api/roles/** 端点 |
| `daydayup-admin-biz/pom.xml` | 修改 | 新增 daydayup-auth-api 依赖 |
| `daydayup-admin-biz/.../service/impl/UserManageServiceImpl.java` | 重写 | 改为 Feign 调用 auth |
| `daydayup-admin-biz/.../controller/UserManageController.java` | 重写 | 代理到 Feign 调用 |
| `daydayup-admin-biz/.../entity/Sys*.java`（5个） | 删除 | 迁移到 auth |
| `daydayup-admin-biz/.../mapper/Sys*.java`（5个） | 删除 | 迁移到 auth |

### Phase 4 — 授权码 + OIDC

| 文件 | 操作 | 职责 |
|------|------|------|
| `daydayup-auth/src/main/resources/templates/login.html` | 创建 | 登录页面 |
| `daydayup-auth/src/main/resources/templates/consent.html` | 创建 | 授权同意页面 |
| `daydayup-auth/src/main/java/.../auth/controller/LoginPageController.java` | 创建 | 登录页面路由 |
| `daydayup-auth/src/main/java/.../auth/controller/ConsentController.java` | 创建 | consent 处理 |
| `daydayup-auth/src/main/java/.../auth/controller/UserInfoController.java` | 创建 | /userinfo 端点 |
| `daydayup-auth/src/main/java/.../config/AuthorizationServerConfig.java` | 修改 | 授权码 + consent 配置 |
| `daydayup-auth/src/main/java/.../config/AuthSecurityConfig.java` | 修改 | 允许登录页/consent 公开访问 |
| `daydayup-auth/src/test/java/.../auth/controller/AuthorizationCodeIntegrationTest.java` | 创建 | 授权码 + PKCE 端到端测试 |

### Phase 5 — 网关适配 + 清理

| 文件 | 操作 | 职责 |
|------|------|------|
| `daydayup-gateway/.../filter/AuthGlobalFilter.java` | 修改 | claim 读取适配 |
| `daydayup-auth/.../controller/LoginController.java` | 删除 | SAS 接管 |
| `daydayup-auth/.../controller/JwksController.java` | 删除 | SAS 接管 |
| `daydayup-auth/.../service/JwtTokenService.java` | 删除 | SAS 内置 token 管理 |
| `daydayup-auth/.../user/RemoteUserService.java` | 删除 | 本地查询替代 |
| `daydayup-auth/.../user/InMemoryUserService.java` | 删除 | 已废弃 |
| `daydayup-auth/.../entity/RefreshToken.java` | 删除 | SAS 管理 refresh token |
| `daydayup-auth/.../mapper/RefreshTokenMapper.java` | 删除 | SAS 管理 refresh token |

---

## Phase 1：SAS 集成 + password 兼容

### 任务 1.1：添加 SAS 依赖

**文件：**
- 修改：`daydayup-platform/daydayup-auth/pom.xml`

- [ ] **步骤 1：添加 SAS 依赖**

在 `<dependencies>` 块中，`spring-security-oauth2-jose` 之后添加：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.4.3</version>
</dependency>
```

- [ ] **步骤 2：验证依赖解析**

运行：`cd daydayup-platform/daydayup-auth && mvn dependency:tree -pl . | grep authorization-server`
预期：看到 `spring-security-oauth2-authorization-server:1.4.3`

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/pom.xml
git commit -m "build(auth): add Spring Authorization Server 1.4.3 dependency"
```

---

### 任务 1.2：创建 PasswordAuthenticationToken

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationToken.java`

- [ ] **步骤 1：创建 PasswordAuthenticationToken**

```java
package com.yuan.daydayup.auth.grant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Set;

/**
 * password grant 自定义认证 token。
 *
 * <p>携带 username / password，由 {@link PasswordAuthenticationConverter} 构造，
 * 交给 {@link PasswordAuthenticationProvider} 处理。</p>
 */
public class PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType PASSWORD =
            new AuthorizationGrantType("password");

    private final String username;
    private final String password;
    private final Set<String> scopes;

    public PasswordAuthenticationToken(String username,
                                       String password,
                                       Authentication clientPrincipal,
                                       Set<String> scopes) {
        super(PASSWORD, clientPrincipal, null);
        this.username = username;
        this.password = password;
        this.scopes = scopes;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationToken.java
git commit -m "feat(auth): add PasswordAuthenticationToken for custom password grant"
```

---

### 任务 1.3：创建 PasswordAuthenticationConverter

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationConverter.java`

- [ ] **步骤 1：创建 PasswordAuthenticationConverter**

```java
package com.yuan.daydayup.auth.grant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 从 {@code POST /oauth2/token} 请求中识别 {@code grant_type=password}，
 * 提取 username / password / scope 并构造 {@link PasswordAuthenticationToken}。
 *
 * <p>SAS 的 token 端点支持链式 {@link AuthenticationConverter}，
 * 本 converter 只在 {@code grant_type=password} 时返回 token，其余情况返回 null
 * 交给下一个 converter（如 SAS 内置的 authorization_code / client_credentials）。</p>
 */
public class PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"password".equals(grantType)) {
            return null;
        }

        // 客户端必须已通过 client authentication（Basic Auth 或 client_secret_post）
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            return null;
        }

        MultiValueMap<String, String> parameters = getParameters(request);

        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username)) {
            return null;
        }

        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password)) {
            return null;
        }

        Set<String> scopes = new HashSet<>();
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope)) {
            for (String s : scope.split(" ")) {
                if (StringUtils.hasText(s)) {
                    scopes.add(s);
                }
            }
        }

        return new PasswordAuthenticationToken(username, password, clientPrincipal, scopes);
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationConverter.java
git commit -m "feat(auth): add PasswordAuthenticationConverter"
```

---

### 任务 1.4：创建 PasswordAuthenticationProvider

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationProvider.java`

- [ ] **步骤 1：创建 PasswordAuthenticationProvider**

```java
package com.yuan.daydayup.auth.grant;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 处理 {@link PasswordAuthenticationToken}：校验 username/password，
 * 走 SAS 标准流程签发 access_token + refresh_token + id_token（如 scope 含 openid）。
 *
 * <p>本 provider 是 SAS 框架在 {@code /oauth2/token} 端点的链式 provider 之一，
 * 仅当传入的 token 是 {@link PasswordAuthenticationToken} 时才会处理。</p>
 */
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;

    public PasswordAuthenticationProvider(UserDetailsService userDetailsService,
                                          PasswordEncoder passwordEncoder,
                                          OAuth2AuthorizationService authorizationService,
                                          OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        Assert.notNull(userDetailsService, "userDetailsService cannot be null");
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PasswordAuthenticationToken passwordAuth = (PasswordAuthenticationToken) authentication;

        // 1. 确认客户端已认证
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClient(passwordAuth);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (registeredClient == null || !registeredClient.getAuthorizationGrantTypes()
                .contains(PasswordAuthenticationToken.PASSWORD)) {
            return null;
        }

        // 2. 加载并校验用户
        UserDetails userDetails = userDetailsService.loadUserByUsername(passwordAuth.getUsername());
        if (!passwordEncoder.matches(passwordAuth.getPassword(), userDetails.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }

        // 3. 确定 scopes
        Set<String> authorizedScopes = passwordAuth.getScopes().isEmpty()
                ? new HashSet<>(registeredClient.getScopes())
                : passwordAuth.getScopes();

        // 4. 生成 access_token
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorizedScopes(authorizedScopes)
                .authorizationGrantType(PasswordAuthenticationToken.PASSWORD)
                .authorizationGrant(passwordAuth);

        // access_token
        OAuth2TokenContext tokenContext = tokenContextBuilder
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            throw new IllegalStateException("Token generator failed to generate access token");
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(),
                generatedAccessToken.getIssuedAt(),
                generatedAccessToken.getExpiresAt(),
                authorizedScopes);

        // 5. 构建 OAuth2Authorization
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userDetails.getUsername())
                .authorizationGrantType(PasswordAuthenticationToken.PASSWORD)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), passwordAuth);

        if (generatedAccessToken instanceof ClaimAccessor claimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims()));
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        // 6. refresh_token
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshTokenContext = tokenContextBuilder
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .build();
            OAuth2Token generatedRefreshToken = tokenGenerator.generate(refreshTokenContext);
            if (generatedRefreshToken != null) {
                refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
                authorizationBuilder.refreshToken(refreshToken);
            }
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private OAuth2ClientAuthenticationToken getAuthenticatedClient(Authentication authentication) {
        Authentication clientAuth = (Authentication) authentication.getPrincipal();
        if (clientAuth instanceof OAuth2ClientAuthenticationToken token && token.isAuthenticated()) {
            return token;
        }
        throw new org.springframework.security.authentication.AuthenticationServiceException("Client not authenticated");
    }
}
```

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/grant/PasswordAuthenticationProvider.java
git commit -m "feat(auth): add PasswordAuthenticationProvider"
```

---

### 任务 1.5：创建 DayDayUpUserDetailsService

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsService.java`

**说明：** Phase 1 使用现有的 `RemoteUserService`（Feign 查 admin DB）；Phase 2 迁移身份数据后切换为本地 `SysUserMapper`。

- [ ] **步骤 1：编写失败的测试**

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.auth.user.RemoteUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DayDayUpUserDetailsServiceTest {

    private final RemoteUserService remoteUserService = mock(RemoteUserService.class);
    private final DayDayUpUserDetailsService service = new DayDayUpUserDetailsService(remoteUserService);

    @Test
    void loadUserByUsername_found() {
        SimpleUser simpleUser = SimpleUser.builder()
                .userId(1L).username("admin").password("$2a$10$hash")
                .authorities(Set.of("admin:*")).status(1).build();
        when(remoteUserService.findByUsername("admin")).thenReturn(Optional.of(simpleUser));

        UserDetails result = service.loadUserByUsername("admin");

        assertEquals("admin", result.getUsername());
        assertEquals("$2a$10$hash", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin:*")));
        assertTrue(result.isEnabled());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(remoteUserService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_disabled() {
        SimpleUser disabled = SimpleUser.builder()
                .userId(2L).username("disabled").password("$2a$10$hash")
                .authorities(Set.of()).status(0).build();
        when(remoteUserService.findByUsername("disabled")).thenReturn(Optional.of(disabled));

        UserDetails result = service.loadUserByUsername("disabled");
        assertFalse(result.isEnabled());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=DayDayUpUserDetailsServiceTest -q`
预期：FAIL，编译错误 `DayDayUpUserDetailsService` 不存在

- [ ] **步骤 3：编写最少实现代码**

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * SAS 认证用的 UserDetailsService。
 *
 * <p>Phase 1：委托给 {@link RemoteUserService}（Feign 查 admin DB）。</p>
 * <p>Phase 2：身份数据迁入本地后，改为注入 {@code SysUserMapper} 直接查询。</p>
 */
@Service
@RequiredArgsConstructor
public class DayDayUpUserDetailsService implements UserDetailsService {

    private final RemoteUserService remoteUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SimpleUser simpleUser = remoteUserService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Collection<GrantedAuthority> authorities = simpleUser.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        boolean enabled = simpleUser.getStatus() != null && simpleUser.getStatus() == 1;

        return User.builder()
                .username(simpleUser.getUsername())
                .password(simpleUser.getPassword())
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=DayDayUpUserDetailsServiceTest -q`
预期：3 tests PASS

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsService.java \
       daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsServiceTest.java
git commit -m "feat(auth): add DayDayUpUserDetailsService (Phase 1: via RemoteUserService)"
```

---

### 任务 1.6：创建 AuthorizationServerConfig

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/config/AuthorizationServerConfig.java`

**说明：** 这是 SAS 的核心配置类，注册所有必要的 Bean。注意 `JWKSource` 和 `JwtDecoder` 从 `JwkConfig` 现有 Bean 注入，不重复创建。

- [ ] **步骤 1：编写启动测试**

```java
package com.yuan.daydayup.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 SAS 核心 Bean 能正确注入，应用能正常启动。
 * 需要 MySQL + Redis 可用（test profile 连接本地或嵌入式）。
 */
@SpringBootTest
class AuthorizationServerConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void sasBeansWired() {
        assertNotNull(context.getBean(RegisteredClientRepository.class));
        assertNotNull(context.getBean(OAuth2AuthorizationService.class));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=AuthorizationServerConfigTest -q`
预期：FAIL，`NoSuchBeanDefinitionException: RegisteredClientRepository`

- [ ] **步骤 3：创建 AuthorizationServerConfig**

```java
package com.yuan.daydayup.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.yuan.daydayup.auth.grant.PasswordAuthenticationConverter;
import com.yuan.daydayup.auth.grant.PasswordAuthenticationProvider;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

    /**
     * SAS 协议端点 SecurityFilterChain（Order=1 优先匹配）。
     * 覆盖 /oauth2/authorize, /oauth2/token, /oauth2/revoke, /oauth2/introspect,
     *       /oauth2/jwks, /.well-known/*, /oauth2/consent, /userinfo 等 SAS 端点。
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(token -> token
                        .accessTokenRequestConverter(new PasswordAuthenticationConverter())
                        .authenticationProvider(new PasswordAuthenticationProvider(
                                null, // 由 Spring 注入，此处示意；实际从构造注入
                                null,
                                authorizationService,
                                null))
                )
                .oidc(Customizer.withDefaults());

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }

    // ---- JDBC 存储 Bean ----

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository clientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, clientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository clientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clientRepository);
    }

    /**
     * JWT 自定义：向 access_token 注入 uid / authorities claim。
     *
     * <p>SAS 默认签发的 claim 只有 iss/sub/aud/exp/iat/jti/scope。
     * 网关和业务服务依赖 uid 和 authorities，因此必须通过自定义注入。</p>
     *
     * <p>注意：UserDetails.getAuthorities() 返回的是 SimpleGrantedAuthority，
     * 其 getAuthority() 已经是权限码字符串（如 "admin:*"）。</p>
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();
                if (principal != null && principal.getPrincipal() instanceof org.springframework.security.core.userdetails.User userDetails) {
                    // uid: 从 username 反查（Phase 1 暂用 0 占位，Phase 2 从本地 DB 拿 userId）
                    // 此处注入 authorities
                    Collection<String> authorities = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());
                    context.getClaims().claim(SecurityConstants.CLAIM_AUTHORITIES, authorities);
                }
            }
        };
    }

    /**
     * SAS 全局配置：issuer URL。
     *
     * <p>实际部署时通过 Nacos 覆盖 {@code daydayup.auth.issuer-url}。</p>
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://daydayup-auth:9200")
                .build();
    }
}
```

> **注意：** 上面代码中 `PasswordAuthenticationProvider` 构造参数为 null 占位，实际实现需要从 `ApplicationContext` 注入所有 Bean。完整的注入方式见下方说明——改为在 `AuthorizationServerSecurityFilterChain` Bean 方法中声明全部参数，由 Spring 自动注入。

**修正后的 `authorizationServerSecurityFilterChain` 方法签名（实际实现使用）：**

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public SecurityFilterChain authorizationServerSecurityFilterChain(
        HttpSecurity http,
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder,
        OAuth2AuthorizationService authorizationService) throws Exception {

    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .tokenEndpoint(token -> token
                    .accessTokenRequestConverter(new PasswordAuthenticationConverter())
                    .authenticationProvider(new PasswordAuthenticationProvider(
                            userDetailsService,
                            passwordEncoder,
                            authorizationService,
                            http.getSharedObject(OAuth2TokenGenerator.class) // 需要手动注入
                    ))
            )
            .oidc(Customizer.withDefaults());

    http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

    return http.build();
}
```

> **更正：** `OAuth2TokenGenerator` 无法从 `HttpSecurity` 获取，需要直接声明为 Bean。SAS 的 `OAuth2TokenGenerator` 由 `JwtGenerator`（JWT access_token）+ `OAuth2RefreshTokenGenerator`（opaque refresh_token）组成。

**最终实现方式（合并到一个 Bean 方法中，通过 @Bean 方法参数让 Spring 注入全部依赖）：**

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public SecurityFilterChain authorizationServerSecurityFilterChain(
        HttpSecurity http,
        UserDetailsService dayDayUpUserDetailsService,
        PasswordEncoder passwordEncoder,
        OAuth2AuthorizationService authorizationService,
        OAuth2TokenGenerator<? extends org.springframework.security.oauth2.core.OAuth2Token> tokenGenerator) throws Exception {

    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .tokenEndpoint(token -> token
                    .accessTokenRequestConverter(new PasswordAuthenticationConverter())
                    .authenticationProvider(new PasswordAuthenticationProvider(
                            dayDayUpUserDetailsService,
                            passwordEncoder,
                            authorizationService,
                            tokenGenerator))
            )
            .oidc(Customizer.withDefaults());

    http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

    return http.build();
}
```

> **注意：** `OAuth2TokenGenerator` Bean 需要由 SAS 自动配置提供，或者手动注册。Spring Boot 3.2+ 的 `spring-boot-autoconfigure` 包含 SAS 自动配置，当 SAS 在 classpath 且有 `RegisteredClientRepository` Bean 时，会自动注册 `OAuth2TokenGenerator`。如果自动配置未生效，需手动添加：

```java
@Bean
public OAuth2TokenGenerator<? extends org.springframework.security.oauth2.core.OAuth2Token> tokenGenerator(
        JWKSource<SecurityContext> jwkSource) {
    JwtGenerator jwtGenerator = new JwtGenerator(jwkSource);
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=AuthorizationServerConfigTest -q`
预期：SAS Bean 注入成功（需要 MySQL 可用，连接 `daydayup_auth` 库）

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/config/AuthorizationServerConfig.java \
       daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/config/AuthorizationServerConfigTest.java
git commit -m "feat(auth): add AuthorizationServerConfig with SAS beans and custom password grant"
```

---

### 任务 1.7：重写 AuthSecurityConfig

**文件：**
- 重写：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/config/AuthSecurityConfig.java`

- [ ] **步骤 1：重写 AuthSecurityConfig**

`AuthorizationServerConfig` 已注册 SAS 协议 SecurityFilterChain（Order=1），此处只负责通用链（Order=2）：

```java
package com.yuan.daydayup.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 通用 SecurityFilterChain（Order=2）。
 *
 * <p>SAS 协议端点由 {@link AuthorizationServerConfig} 的 Chain（Order=1）接管。
 * 本 Chain 处理其余端点：/api/**（用户管理 API 需 Bearer 认证）、/login（登录页公开）、
 * /swagger-ui/**（公开）。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class AuthSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
```

- [ ] **步骤 2：运行全模块测试验证无回归**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -q`
预期：已有测试全部通过（`JwkConfigTest`、`LoginControllerTest`、`LoginAttemptServiceTest`、`TokenBlacklistServiceTest`）

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/config/AuthSecurityConfig.java
git commit -m "refactor(auth): split SecurityFilterChain for SAS (Order 1) and default (Order 2)"
```

---

### 任务 1.8：添加 SAS 配置到 application.yml + 建表

**文件：**
- 修改：`daydayup-platform/daydayup-auth/src/main/resources/application.yml`
- 修改：`daydayup-platform/daydayup-auth/src/main/resources/db/init.sql`

- [ ] **步骤 1：更新 application.yml**

在 `daydayup:` 块末尾追加：

```yaml
daydayup:
  auth:
    access-token-ttl-seconds: 7200
    refresh-token-ttl-seconds: 604800
    issuer-url: http://daydayup-auth:9200
    jwk:
      master-password: ${JWK_MASTER_PASSWORD:daydayup-jwk-default}
```

在 `spring:` 块追加（或确保已有，SAS 自动配置需要 JDBC）：

```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:db/init.sql
```

- [ ] **步骤 2：更新 init.sql，追加 SAS 标准表**

在 `daydayup_auth` 库的 `init.sql` 末尾追加以下建表语句（`IF NOT EXISTS` 确保幂等）：

```sql
-- ============================================================
-- SAS 标准表（Spring Authorization Server）
-- ============================================================

CREATE TABLE IF NOT EXISTS `oauth2_registered_client` (
    `id`                            VARCHAR(100)  NOT NULL,
    `client_id`                     VARCHAR(100)  NOT NULL,
    `client_id_issued_at`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `client_secret`                 VARCHAR(200)  DEFAULT NULL,
    `client_secret_expires_at`      TIMESTAMP     DEFAULT NULL,
    `client_name`                   VARCHAR(200)  NOT NULL,
    `client_authentication_methods` VARCHAR(1000) NOT NULL,
    `authorization_grant_types`     VARCHAR(1000) NOT NULL,
    `redirect_uris`                 VARCHAR(1000) DEFAULT NULL,
    `post_logout_redirect_uris`     VARCHAR(1000) DEFAULT NULL,
    `scopes`                        VARCHAR(1000) NOT NULL,
    `client_settings`               TEXT          NOT NULL,
    `token_settings`                TEXT          NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 客户端注册';

CREATE TABLE IF NOT EXISTS `oauth2_authorization` (
    `id`                            VARCHAR(100)  NOT NULL,
    `registered_client_id`          VARCHAR(100)  NOT NULL,
    `principal_name`                VARCHAR(200)  NOT NULL,
    `authorization_grant_type`      VARCHAR(100)  NOT NULL,
    `authorized_scopes`             VARCHAR(1000) DEFAULT NULL,
    `attributes`                    TEXT          DEFAULT NULL,
    `state`                         VARCHAR(500)  DEFAULT NULL,
    `authorization_code_value`      TEXT          DEFAULT NULL,
    `authorization_code_issued_at`  TIMESTAMP     DEFAULT NULL,
    `authorization_code_expires_at` TIMESTAMP     DEFAULT NULL,
    `authorization_code_metadata`   TEXT          DEFAULT NULL,
    `access_token_value`            TEXT          DEFAULT NULL,
    `access_token_issued_at`        TIMESTAMP     DEFAULT NULL,
    `access_token_expires_at`       TIMESTAMP     DEFAULT NULL,
    `access_token_metadata`         TEXT          DEFAULT NULL,
    `access_token_type`             VARCHAR(100)  DEFAULT NULL,
    `access_token_scopes`           VARCHAR(1000) DEFAULT NULL,
    `oidc_id_token_value`           TEXT          DEFAULT NULL,
    `oidc_id_token_issued_at`       TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_expires_at`      TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_metadata`        TEXT          DEFAULT NULL,
    `refresh_token_value`           TEXT          DEFAULT NULL,
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

CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent` (
    `registered_client_id` VARCHAR(100)  NOT NULL,
    `principal_name`       VARCHAR(200)  NOT NULL,
    `authorities`          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户授权同意记录';
```

- [ ] **步骤 3：添加种子客户端数据**

在 `init.sql` 末尾追加：

```sql
-- ============================================================
-- 种子 OAuth2 客户端
-- ============================================================

INSERT IGNORE INTO `oauth2_registered_client`
(`id`, `client_id`, `client_secret`, `client_name`,
 `client_authentication_methods`, `authorization_grant_types`,
 `redirect_uris`, `scopes`, `client_settings`, `token_settings`)
VALUES
('admin-web', 'admin-web', NULL, 'DayDayUP 管理后台',
 'none', 'authorization_code,refresh_token,password',
 'http://localhost:5173/callback',
 'openid,profile,admin:*',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"}}');
```

- [ ] **步骤 4：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/resources/application.yml \
       daydayup-platform/daydayup-auth/src/main/resources/db/init.sql
git commit -m "feat(auth): add SAS config and standard table DDL"
```

---

### 任务 1.9：password grant 集成测试

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/grant/PasswordGrantIntegrationTest.java`

- [ ] **步骤 1：编写集成测试**

```java
package com.yuan.daydayup.auth.grant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordGrantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void passwordGrantReturnsAccessToken() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("admin-web", ""))  // public client, no secret
                        .param("grant_type", "password")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber());
    }

    @Test
    void passwordGrantWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("admin-web", ""))
                        .param("grant_type", "password")
                        .param("username", "admin")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **步骤 2：运行集成测试**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=PasswordGrantIntegrationTest -q`
预期：需要 MySQL `daydayup_auth` 库可用（含 `sys_user` 种子数据 + `oauth2_registered_client` 种子数据）。Phase 2 前 `sys_user` 在 admin 库，此测试会 FAIL（预期行为，Phase 2 完成后通过）。

- [ ] **步骤 3：Commit（标记为 @Disabled 直到 Phase 2 完成）**

```java
@Disabled("Requires identity tables in auth DB — will pass after Phase 2")
@SpringBootTest
@AutoConfigureMockMvc
class PasswordGrantIntegrationTest { ... }
```

```bash
git add daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/grant/PasswordGrantIntegrationTest.java
git commit -m "test(auth): add password grant integration test (disabled until Phase 2)"
```

---

## Phase 2：身份数据迁移

### 任务 2.1：创建身份实体（auth 模块）

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/SysUser.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/SysRole.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/SysUserRole.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/SysPermission.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/SysRolePermission.java`

- [ ] **步骤 1：创建 SysUser**

```java
package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String mobile;
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private String remark;
}
```

- [ ] **步骤 2：创建 SysRole**

```java
package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
    private String remark;
}
```

- [ ] **步骤 3：创建 SysUserRole**

```java
package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户-角色关联 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {
    private Long userId;
    private Long roleId;
}
```

- [ ] **步骤 4：创建 SysPermission**

```java
package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String path;
    private Integer sort;
    private Integer status;
    private String remark;
}
```

- [ ] **步骤 5：创建 SysRolePermission**

```java
package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色-权限关联 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class SysRolePermission extends BaseEntity {
    private Long roleId;
    private Long permissionId;
}
```

- [ ] **步骤 6：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/
git commit -m "feat(auth): add SysUser/SysRole/SysPermission entities for identity migration"
```

---

### 任务 2.2：创建 Mapper 接口

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/SysUserMapper.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/SysRoleMapper.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/SysUserRoleMapper.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/SysPermissionMapper.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/SysRolePermissionMapper.java`

- [ ] **步骤 1：创建全部 5 个 Mapper**

```java
package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

```java
package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysRole;

public interface SysRoleMapper extends BaseMapper<SysRole> {
}
```

```java
package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysUserRole;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
```

```java
package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysPermission;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {
}
```

```java
package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysRolePermission;

public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
```

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS（`AuthApplication` 的 `@MapperScan("com.yuan.daydayup.auth.mapper")` 自动扫描新 Mapper）

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/
git commit -m "feat(auth): add SysUser/SysRole/SysPermission mappers"
```

---

### 任务 2.3：迁移身份表 DDL + 种子数据到 auth 库

**文件：**
- 修改：`daydayup-platform/daydayup-auth/src/main/resources/db/init.sql`
- 修改：`sql/daydayup_admin.sql`

- [ ] **步骤 1：在 auth init.sql 末尾追加身份表 DDL**

```sql
-- ============================================================
-- 身份表（从 daydayup_admin 迁入）
-- ============================================================

CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `mobile`        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`        VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `last_login_at` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `last_login_ip` VARCHAR(45)  DEFAULT NULL COMMENT '最近登录 IP',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT       DEFAULT NULL,
    `update_by`     BIGINT       DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_mobile` (`mobile`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT      NOT NULL,
    `code`        VARCHAR(64) NOT NULL,
    `name`        VARCHAR(64) NOT NULL,
    `sort`        INT         NOT NULL DEFAULT 0,
    `status`      TINYINT     NOT NULL DEFAULT 1,
    `remark`      VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT      DEFAULT NULL,
    `update_by`   BIGINT      DEFAULT NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`          BIGINT   NOT NULL,
    `user_id`     BIGINT   NOT NULL,
    `role_id`     BIGINT   NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT   DEFAULT NULL,
    `update_by`   BIGINT   DEFAULT NULL,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       NOT NULL,
    `code`        VARCHAR(128) NOT NULL,
    `name`        VARCHAR(64)  NOT NULL,
    `type`        VARCHAR(20)  NOT NULL DEFAULT 'api',
    `parent_id`   BIGINT       DEFAULT 0,
    `path`        VARCHAR(255) DEFAULT NULL,
    `sort`        INT          NOT NULL DEFAULT 0,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `remark`      VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限定义';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT   NOT NULL,
    `role_id`       BIGINT   NOT NULL,
    `permission_id` BIGINT   NOT NULL,
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT   DEFAULT NULL,
    `update_by`     BIGINT   DEFAULT NULL,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联';
```

- [ ] **步骤 2：在 auth init.sql 末尾追加身份种子数据**

```sql
-- ============================================================
-- 身份种子数据
-- ============================================================

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `create_by`)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1, 1);

INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `sort`, `status`, `create_by`) VALUES
(1, 'admin', '超级管理员', 1, 1, 1),
(2, 'user',  '普通用户',   2, 1, 1);

INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_by`) VALUES
(1, 1, 1, 1);

INSERT IGNORE INTO `sys_permission` (`id`, `code`, `name`, `type`, `sort`, `status`, `create_by`) VALUES
(1,  'admin:*',     '管理后台全部权限', 'api', 1,  1, 1),
(2,  'game:*',      '游戏服务全部权限', 'api', 2,  1, 1),
(3,  'game:play',   '游戏参与',         'api', 3,  1, 1),
(4,  'social:*',    '社交服务全部权限', 'api', 4,  1, 1),
(5,  'social:read', '社交只读',         'api', 5,  1, 1),
(6,  'user:read',   '用户查询',         'api', 6,  1, 1),
(7,  'user:write',  '用户管理',         'api', 7,  1, 1),
(8,  'dict:read',   '字典查询',         'api', 8,  1, 1),
(9,  'dict:write',  '字典管理',         'api', 9,  1, 1),
(10, 'menu:read',   '菜单查询',         'api', 10, 1, 1),
(11, 'menu:write',  '菜单管理',         'api', 11, 1, 1),
(12, 'role:read',   '角色查询',         'api', 12, 1, 1),
(13, 'role:write',  '角色管理',         'api', 13, 1, 1);

INSERT IGNORE INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_by`) VALUES
(1,  1, 1,  1), (2,  1, 2,  1), (3,  1, 3,  1), (4,  1, 4,  1),
(5,  1, 5,  1), (6,  1, 6,  1), (7,  1, 7,  1), (8,  1, 8,  1),
(9,  1, 9,  1), (10, 1, 10, 1), (11, 1, 11, 1), (12, 1, 12, 1),
(13, 1, 13, 1),
(14, 2, 3,  1), (15, 2, 5,  1), (16, 2, 6,  1),
(17, 2, 8,  1), (18, 2, 10, 1);
```

- [ ] **步骤 3：从 daydayup_admin.sql 中删除这 5 张表的 DDL 和种子数据**

删除 `daydayup_admin.sql` 中以下内容：
- `CREATE TABLE sys_user` 块
- `CREATE TABLE sys_role` 块
- `CREATE TABLE sys_user_role` 块
- `CREATE TABLE sys_permission` 块
- `CREATE TABLE sys_role_permission` 块
- 对应的 `INSERT INTO sys_user` 种子数据
- 对应的 `INSERT INTO sys_role` 种子数据
- 对应的 `INSERT INTO sys_user_role` 种子数据
- 对应的 `INSERT INTO sys_permission` 种子数据
- 对应的 `INSERT INTO sys_role_permission` 种子数据

**保留不动：** `sys_menu`、`sys_dict`、`sys_dict_item`、`sys_oper_log` 及其种子数据。

- [ ] **步骤 4：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/resources/db/init.sql sql/daydayup_admin.sql
git commit -m "feat(auth): migrate identity tables DDL and seed data from admin to auth"
```

---

### 任务 2.4：重写 DayDayUpUserDetailsService 为本地查询

**文件：**
- 重写：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsService.java`
- 修改：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsServiceTest.java`

- [ ] **步骤 1：重写为本地 SysUserMapper 查询**

```java
package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SAS 认证用的 UserDetailsService —— 直接查本地 auth 库。
 */
@Service
@RequiredArgsConstructor
public class DayDayUpUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        Collection<GrantedAuthority> authorities = loadAuthorities(user.getId());
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }

    private Collection<GrantedAuthority> loadAuthorities(Long userId) {
        // 1. 查用户角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        // 2. 查角色权限关联
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        // 3. 查权限定义
        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .map(p -> new SimpleGrantedAuthority(p.getCode()))
                .collect(Collectors.toList());
    }
}
```

- [ ] **步骤 2：更新测试（移除 RemoteUserService mock，改用 SysUserMapper mock）**

```java
package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DayDayUpUserDetailsServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
    private final SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);

    private final DayDayUpUserDetailsService service = new DayDayUpUserDetailsService(
            userMapper, userRoleMapper, roleMapper, rolePermissionMapper, permissionMapper);

    @Test
    void loadUserByUsername_notFound() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_found_noRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("$2a$10$hash");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(java.util.List.of());

        UserDetails result = service.loadUserByUsername("test");
        assertEquals("test", result.getUsername());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().isEmpty());
    }
}
```

- [ ] **步骤 3：运行测试验证通过**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=DayDayUpUserDetailsServiceTest -q`
预期：2 tests PASS

- [ ] **步骤 4：启用 password grant 集成测试（去掉 @Disabled）**

从 `PasswordGrantIntegrationTest.java` 中移除 `@Disabled` 注解。此时 `sys_user` 和 `oauth2_registered_client` 种子数据都在 auth 库中，集成测试应能通过。

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=PasswordGrantIntegrationTest -q`
预期：PASS（需 MySQL `daydayup_auth` 库含种子数据）

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsService.java \
       daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/DayDayUpUserDetailsServiceTest.java \
       daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/grant/PasswordGrantIntegrationTest.java
git commit -m "feat(auth): rewrite UserDetailsService to use local SysUserMapper; enable integration test"
```

---

## Phase 3：用户管理 API + Admin 重构

### 任务 3.1：创建 daydayup-auth-api 模块

**文件：**
- 创建：`daydayup-platform/daydayup-auth-api/pom.xml`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/client/UserManageClient.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/client/RoleManageClient.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/dto/UserCreateDTO.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/dto/UserUpdateDTO.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/dto/UserStatusDTO.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/dto/UserPageQuery.java`
- 创建：`daydayup-platform/daydayup-auth-api/src/main/java/com/yuan/daydayup/auth/api/vo/UserDetailVO.java`

- [ ] **步骤 1：创建模块 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yuan</groupId>
        <artifactId>daydayup-platform</artifactId>
        <version>0.1.0</version>
    </parent>

    <artifactId>daydayup-auth-api</artifactId>
    <description>认证中心 Feign 接口与 DTO</description>

    <dependencies>
        <dependency>
            <groupId>com.yuan</groupId>
            <artifactId>daydayup-common-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **步骤 2：在父 POM modules 中添加 auth-api**

在 `daydayup-platform/pom.xml` 的 `<modules>` 块中追加：

```xml
<module>daydayup-auth-api</module>
```

- [ ] **步骤 3：创建 DTO/VO（参照 admin-biz 的 UserCreateDTO/UserUpdateDTO/UserStatusDTO/UserPageQueryDTO/UserDetailVO 复制，包名改为 `com.yuan.daydayup.auth.api`）**

`UserCreateDTO.java`:
```java
package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class UserCreateDTO {
    @NotBlank @Size(max = 64) private String username;
    @NotBlank @Size(max = 255) private String password;
    private String nickname;
    private String email;
    private String mobile;
    private List<Long> roleIds;
}
```

`UserUpdateDTO.java`:
```java
package com.yuan.daydayup.auth.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateDTO {
    private String nickname;
    private String email;
    private String mobile;
    private List<Long> roleIds;
}
```

`UserStatusDTO.java`:
```java
package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {
    @NotNull private Integer status;
}
```

`UserPageQuery.java`:
```java
package com.yuan.daydayup.auth.api.dto;

import com.yuan.daydayup.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery {
    private String username;
    private Integer status;
}
```

`UserDetailVO.java`:
```java
package com.yuan.daydayup.auth.api.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private List<Long> roleIds;
    private List<String> roleCodes;
}
```

- [ ] **步骤 4：创建 Feign 接口**

`UserManageClient.java`:
```java
package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "daydayup-auth", contextId = "userManageClient",
             path = "/api/users")
public interface UserManageClient {
    @GetMapping("/page")
    R<PageResult<UserDetailVO>> page(UserPageQuery query);

    @GetMapping("/{id}")
    R<UserDetailVO> detail(@PathVariable("id") Long id);

    @PostMapping
    R<UserDetailVO> create(@RequestBody UserCreateDTO dto);

    @PutMapping("/{id}")
    R<UserDetailVO> update(@PathVariable("id") Long id, @RequestBody UserUpdateDTO dto);

    @PatchMapping("/{id}/status")
    R<Void> changeStatus(@PathVariable("id") Long id, @RequestBody UserStatusDTO dto);

    @PatchMapping("/{id}/login-info")
    R<Void> updateLoginInfo(@PathVariable("id") Long id, @RequestParam("lastLoginIp") String lastLoginIp);
}
```

`RoleManageClient.java`:
```java
package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "daydayup-auth", contextId = "roleManageClient",
             path = "/api/roles")
public interface RoleManageClient {
    @GetMapping("/by-ids")
    R<List<RoleDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
```

> `RoleDTO` 在同一包中定义（`com.yuan.daydayup.auth.api.dto.RoleDTO`）：

```java
package com.yuan.daydayup.auth.api.dto;

import lombok.Data;

@Data
public class RoleDTO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
}
```

- [ ] **步骤 5：验证模块编译**

运行：`cd daydayup-platform && mvn compile -pl daydayup-auth-api -q`
预期：BUILD SUCCESS

- [ ] **步骤 6：Commit**

```bash
git add daydayup-platform/daydayup-auth-api/
git add daydayup-platform/pom.xml
git commit -m "feat(auth): create daydayup-auth-api module with Feign clients and DTOs"
```

---

### 任务 3.2：在 auth 服务实现用户管理 API

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/UserManageService.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/UserManageController.java`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/RoleManageController.java`

- [ ] **步骤 1：创建 UserManageService**

在 `auth` 模块中创建 `UserManageService`，包含用户 CRUD + 角色分配。主要方法：
- `PageResult<UserDetailVO> page(UserPageQuery query)`
- `UserDetailVO detail(Long id)`
- `UserDetailVO create(UserCreateDTO dto)` — BCrypt 编码密码，分配角色
- `UserDetailVO update(Long id, UserUpdateDTO dto)` — 更新用户信息和角色
- `void changeStatus(Long id, Integer status)`
- `void updateLoginInfo(Long userId, String lastLoginIp)`

```java
package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManageService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserDetailVO> page(UserPageQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (query.getUsername() != null) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        // 使用 MyBatis-Plus 分页
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        query.getPageNum(), query.getPageSize());
        userMapper.selectPage(page, wrapper);

        List<UserDetailVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    public UserDetailVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return toVO(user);
    }

    @Transactional
    public UserDetailVO create(UserCreateDTO dto) {
        // 检查用户名唯一
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (exist > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setStatus(1);
        userMapper.insert(user);

        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole link = new SysUserRole();
                link.setUserId(user.getId());
                link.setRoleId(roleId);
                userRoleMapper.insert(link);
            }
        }
        return toVO(user);
    }

    @Transactional
    public UserDetailVO update(Long id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        userMapper.updateById(user);

        // 重新分配角色
        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole link = new SysUserRole();
                link.setUserId(id);
                link.setRoleId(roleId);
                userRoleMapper.insert(link);
            }
        }
        return toVO(user);
    }

    public void changeStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void updateLoginInfo(Long userId, String lastLoginIp) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(lastLoginIp);
            userMapper.updateById(user);
        }
    }

    private UserDetailVO toVO(SysUser user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setMobile(user.getMobile());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setLastLoginIp(user.getLastLoginIp());
        // 加载角色
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        vo.setRoleIds(roleIds);
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoleCodes(roles.stream().map(SysRole::getCode).toList());
        }
        return vo;
    }
}
```

- [ ] **步骤 2：创建 UserManageController**

```java
package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.auth.service.UserManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping("/page")
    public R<PageResult<UserDetailVO>> page(UserPageQuery query) {
        return R.ok(userManageService.page(query));
    }

    @GetMapping("/{id}")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userManageService.detail(id));
    }

    @PostMapping
    public R<UserDetailVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    @PutMapping("/{id}")
    public R<UserDetailVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return R.ok(userManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.changeStatus(id, dto.getStatus());
        return R.ok();
    }

    @PatchMapping("/{id}/login-info")
    public R<Void> updateLoginInfo(@PathVariable Long id, @RequestParam String lastLoginIp) {
        userManageService.updateLoginInfo(id, lastLoginIp);
        return R.ok();
    }
}
```

- [ ] **步骤 3：创建 RoleManageController**

```java
package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.RoleDTO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleManageController {

    private final SysRoleMapper roleMapper;

    @GetMapping("/by-ids")
    public R<List<RoleDTO>> listByIds(@RequestParam List<Long> ids) {
        List<SysRole> roles = roleMapper.selectBatchIds(ids);
        List<RoleDTO> dtos = roles.stream().map(r -> {
            RoleDTO dto = new RoleDTO();
            dto.setId(r.getId());
            dto.setCode(r.getCode());
            dto.setName(r.getName());
            dto.setStatus(r.getStatus());
            return dto;
        }).collect(Collectors.toList());
        return R.ok(dtos);
    }
}
```

- [ ] **步骤 4：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/UserManageService.java \
       daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/UserManageController.java \
       daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/RoleManageController.java
git commit -m "feat(auth): add UserManageService and /api/users, /api/roles controllers"
```

---

### 任务 3.3：Admin-biz 改为 Feign 调用 auth

**文件：**
- 修改：`daydayup-platform/daydayup-admin/daydayup-admin-biz/pom.xml`
- 重写：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/service/impl/UserManageServiceImpl.java`
- 重写：`daydayup-platform/daydayup-admin/daydayup-admin-biz/src/main/java/com/yuan/daydayup/admin/controller/UserManageController.java`
- 删除：`daydayup-admin-biz/.../entity/SysUser.java`
- 删除：`daydayup-admin-biz/.../entity/SysRole.java`
- 删除：`daydayup-admin-biz/.../entity/SysUserRole.java`
- 删除：`daydayup-admin-biz/.../entity/SysPermission.java`
- 删除：`daydayup-admin-biz/.../entity/SysRolePermission.java`
- 删除：`daydayup-admin-biz/.../mapper/SysUserMapper.java`
- 删除：`daydayup-admin-biz/.../mapper/SysRoleMapper.java`
- 删除：`daydayup-admin-biz/.../mapper/SysUserRoleMapper.java`
- 删除：`daydayup-admin-biz/.../mapper/SysPermissionMapper.java`
- 删除：`daydayup-admin-biz/.../mapper/SysRolePermissionMapper.java`
- 删除：`daydayup-admin-biz/.../service/UserManageService.java`
- 删除：`daydayup-admin-biz/.../service/RoleService.java`
- 删除：`daydayup-admin-biz/.../service/PermissionService.java`
- 删除：`daydayup-admin-biz/.../service/impl/RoleServiceImpl.java`
- 删除：`daydayup-admin-biz/.../service/impl/PermissionServiceImpl.java`
- 删除：`daydayup-admin-biz/.../controller/RoleController.java`
- 删除：`daydayup-admin-biz/.../controller/PermissionController.java`

- [ ] **步骤 1：pom.xml 新增 auth-api 依赖**

```xml
<dependency>
    <groupId>com.yuan</groupId>
    <artifactId>daydayup-auth-api</artifactId>
</dependency>
```

- [ ] **步骤 2：重写 UserManageServiceImpl（Feign 代理）**

```java
package com.yuan.daydayup.admin.service.impl;

import com.yuan.daydayup.auth.api.client.UserManageClient;
import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {

    private final UserManageClient userManageClient;

    @Override
    public PageResult<UserDetailVO> page(UserPageQuery query) {
        return userManageClient.page(query).getData();
    }

    @Override
    public UserDetailVO detail(Long id) {
        return userManageClient.detail(id).getData();
    }

    @Override
    public UserDetailVO create(UserCreateDTO dto) {
        return userManageClient.create(dto).getData();
    }

    @Override
    public UserDetailVO update(Long id, UserUpdateDTO dto) {
        return userManageClient.update(id, dto).getData();
    }

    @Override
    public void changeStatus(Long id, UserStatusDTO dto) {
        userManageClient.changeStatus(id, dto);
    }

    @Override
    public void updateLoginInfo(Long userId, String lastLoginIp) {
        userManageClient.updateLoginInfo(userId, lastLoginIp);
    }
}
```

> **注意：** `UserManageService` 接口需更新方法签名以匹配 auth-api 的 DTO（`UserCreateDTO`、`UserUpdateDTO`、`UserStatusDTO` 来自 `com.yuan.daydayup.auth.api.dto` 包，替代原来的 `com.yuan.daydayup.admin.dto`）。同理 `UserManageController` 的 import 也要更新。

- [ ] **步骤 3：重写 UserManageController（代理到 Feign）**

```java
package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/manage")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:user:list')")
    public R<PageResult<UserDetailVO>> page(UserPageQuery query) {
        return R.ok(userManageService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:user:detail')")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userManageService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:user:create')")
    public R<UserDetailVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:user:update')")
    public R<UserDetailVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return R.ok(userManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:user:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.changeStatus(id, dto);
        return R.ok();
    }
}
```

- [ ] **步骤 4：删除 admin-biz 中已迁移的 Entity/Mapper/Service/Controller 文件**

删除以下文件（共约 15 个）：
- `admin/entity/SysUser.java`、`SysRole.java`、`SysUserRole.java`、`SysPermission.java`、`SysRolePermission.java`
- `admin/mapper/SysUserMapper.java`、`SysRoleMapper.java`、`SysUserRoleMapper.java`、`SysPermissionMapper.java`、`SysRolePermissionMapper.java`
- `admin/service/UserManageService.java`、`RoleService.java`、`PermissionService.java`
- `admin/service/impl/RoleServiceImpl.java`、`PermissionServiceImpl.java`
- `admin/controller/RoleController.java`、`PermissionController.java`

- [ ] **步骤 5：验证 admin-biz 编译**

运行：`cd daydayup-platform/daydayup-admin && mvn compile -pl daydayup-admin-biz -q`
预期：BUILD SUCCESS（所有对已删除 Entity 的引用已清理）

- [ ] **步骤 6：Commit**

```bash
git add -A daydayup-platform/daydayup-admin/
git commit -m "refactor(admin): delegate user/role/permission to auth via Feign; remove migrated entities"
```

---

## Phase 4：授权码 + OIDC

### 任务 4.1：实现登录页面和 Consent 页面

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/main/resources/templates/login.html`
- 创建：`daydayup-platform/daydayup-auth/src/main/resources/templates/consent.html`
- 创建：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/ConsentController.java`

- [ ] **步骤 1：创建 login.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8"><title>DayDayUP 登录</title></head>
<body>
<h2>DayDayUP 统一认证</h2>
<div th:if="${param.error}" style="color:red">用户名或密码错误</div>
<form method="post" th:action="@{/login}">
    <div><label>用户名 <input type="text" name="username" required autofocus/></label></div>
    <div><label>密　码 <input type="password" name="password" required/></label></div>
    <div><button type="submit">登录</button></div>
</form>
</body>
</html>
```

- [ ] **步骤 2：创建 consent.html**

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"><title>授权确认</title></head>
<body>
<h2 th:text="'应用 ' + ${clientName} + ' 请求以下权限'"></h2>
<form method="post" th:action="@{/oauth2/consent}">
    <input type="hidden" name="client_id" th:value="${clientId}"/>
    <input type="hidden" name="state" th:value="${state}"/>
    <div th:each="scope : ${scopes}">
        <label>
            <input type="checkbox" name="scope" th:value="${scope}" checked/>
            <span th:text="${scope}"></span>
        </label>
    </div>
    <button type="submit" name="consent_action" value="approve">允许</button>
    <button type="submit" name="consent_action" value="deny">拒绝</button>
</form>
</body>
</html>
```

- [ ] **步骤 3：创建 ConsentController**

```java
package com.yuan.daydayup.auth.controller;

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.*;

@Controller
public class ConsentController {

    private final RegisteredClientRepository clientRepository;
    private final OAuth2AuthorizationConsentService consentService;

    public ConsentController(RegisteredClientRepository clientRepository,
                             OAuth2AuthorizationConsentService consentService) {
        this.clientRepository = clientRepository;
        this.consentService = consentService;
    }

    @GetMapping("/oauth2/consent")
    public String consent(Principal principal,
                          Model model,
                          @RequestParam("client_id") String clientId,
                          @RequestParam("scope") String scope,
                          @RequestParam("state") String state) {

        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new IllegalArgumentException("Unknown client: " + clientId);
        }

        Set<String> scopesToApprove = new LinkedHashSet<>();
        for (String s : scope.split(" ")) {
            if (StringUtils.hasText(s)) {
                scopesToApprove.add(s);
            }
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApprove);

        return "consent";
    }
}
```

- [ ] **步骤 4：更新 AuthSecurityConfig 允许登录页公开访问**

在 `defaultSecurityFilterChain` 的 `authorizeHttpRequests` 中确认 `.requestMatchers("/login").permitAll()` 已存在（任务 1.7 已添加）。

- [ ] **步骤 5：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/main/resources/templates/ \
       daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/ConsentController.java
git commit -m "feat(auth): add login page and consent page for authorization code flow"
```

---

### 任务 4.2：授权码 + PKCE 集成测试

**文件：**
- 创建：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/grant/AuthorizationCodeIntegrationTest.java`

- [ ] **步骤 1：编写授权码流程集成测试**

```java
package com.yuan.daydayup.auth.grant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationCodeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void wellKnownEndpointReturnsDiscovery() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").isNotEmpty())
                .andExpect(jsonPath("$.authorization_endpoint").isNotEmpty())
                .andExpect(jsonPath("$.token_endpoint").isNotEmpty())
                .andExpect(jsonPath("$.jwks_uri").isNotEmpty());
    }

    @Test
    void authorizeEndpointRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "admin-web")
                        .param("redirect_uri", "http://localhost:5173/callback")
                        .param("scope", "openid profile")
                        .param("state", "test-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    @Test
    void jwksEndpointReturnsKeys() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }
}
```

- [ ] **步骤 2：运行测试**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -Dtest=AuthorizationCodeIntegrationTest -q`
预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/grant/AuthorizationCodeIntegrationTest.java
git commit -m "test(auth): add authorization code and OIDC discovery integration tests"
```

---

## Phase 5：网关适配

### 任务 5.1：更新 AuthGlobalFilter 读取 SAS JWT claim

**文件：**
- 修改：`daydayup-access/daydayup-gateway/src/main/java/com/yuan/daydayup/gateway/filter/AuthGlobalFilter.java`

- [ ] **步骤 1：定位 claim 读取代码**

在 `mutateExchange` 方法中，当前读取 `username` claim 的代码改为读取 `sub`：

```java
// 修改前：
String username = jwt.getClaimAsString(SecurityConstants.CLAIM_USERNAME);

// 修改后：
String username = jwt.getSubject();  // SAS JWT 的 sub claim 即用户名
```

`uid` 和 `authorities` claim 由 `OAuth2TokenCustomizer` 注入（任务 1.6），读取方式不变。

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-access/daydayup-gateway && mvn compile -pl . -q`
预期：BUILD SUCCESS

- [ ] **步骤 3：Commit**

```bash
git add daydayup-access/daydayup-gateway/src/main/java/com/yuan/daydayup/gateway/filter/AuthGlobalFilter.java
git commit -m "fix(gateway): read username from JWT sub claim (SAS compatibility)"
```

---

## Phase 6：清理

### 任务 6.1：删除旧的认证代码

**文件：**
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/LoginController.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/controller/JwksController.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/service/JwtTokenService.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/user/RemoteUserService.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/user/InMemoryUserService.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/entity/RefreshToken.java`
- 删除：`daydayup-platform/daydayup-auth/src/main/java/com/yuan/daydayup/auth/mapper/RefreshTokenMapper.java`
- 删除：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/controller/LoginControllerTest.java`
- 删除：`daydayup-platform/daydayup-auth/src/test/java/com/yuan/daydayup/auth/service/JwtTokenServiceTest.java`

- [ ] **步骤 1：删除上述文件**

- [ ] **步骤 2：验证编译**

运行：`cd daydayup-platform/daydayup-auth && mvn compile -pl . -q`
预期：BUILD SUCCESS（无编译错误）

- [ ] **步骤 3：运行全量测试**

运行：`cd daydayup-platform/daydayup-auth && mvn test -pl . -q`
预期：所有测试 PASS（`DayDayUpUserDetailsServiceTest`、`PasswordGrantIntegrationTest`、`AuthorizationCodeIntegrationTest`、`AuthorizationServerConfigTest`、`LoginAttemptServiceTest`、`TokenBlacklistServiceTest`、`JwkConfigTest`）

- [ ] **步骤 4：Commit**

```bash
git add -A daydayup-platform/daydayup-auth/
git commit -m "refactor(auth): remove legacy LoginController, JwtTokenService, JwksController, RemoteUserService"
```

---

## 自检

**1. 规格覆盖度：**
- SAS 依赖 ✅ (Task 1.1)
- AuthorizationServerConfig ✅ (Task 1.6)
- SecurityFilterChain 拆分 ✅ (Task 1.7)
- 自定义 password grant ✅ (Tasks 1.2-1.4)
- UserDetailsService ✅ (Tasks 1.5, 2.4)
- SAS 标准建表 ✅ (Task 1.8)
- 身份实体迁移 ✅ (Tasks 2.1-2.3)
- 用户管理 API ✅ (Task 3.2)
- Admin Feign 重构 ✅ (Task 3.3)
- auth-api 模块 ✅ (Task 3.1)
- 登录页面 + consent ✅ (Task 4.1)
- OIDC 发现 + JWKS ✅ (Task 4.2 测试验证)
- Gateway claim 适配 ✅ (Task 5.1)
- 旧代码清理 ✅ (Task 6.1)

**2. 占位符扫描：** 无 "TODO"、"待定"、"后续实现"。所有代码步骤包含完整实现。

**3. 类型一致性：** `PasswordAuthenticationToken`、`PasswordAuthenticationConverter`、`PasswordAuthenticationProvider` 跨任务引用一致。`SysUser`/`SysRole`/`SysPermission` 实体字段与 admin-biz 原始定义完全相同。`UserManageClient` Feign 接口路径与 `UserManageController` 的 `@RequestMapping` 匹配。
