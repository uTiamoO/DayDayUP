package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.service.JwtTokenService;
import com.yuan.daydayup.auth.service.LoginAttemptService;
import com.yuan.daydayup.auth.service.LoginHistoryService;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口
 *
 * <p>P1 简化实现：账号密码 → 直接签发 JWT。</p>
 * <p>P4 阶段将切换为标准 OAuth2 授权流程。</p>
 */
@RestController
@RequestMapping("/oauth2")
@Validated
@Tag(name = "认证登录", description = "登录、刷新、登出、令牌吊销")
public class LoginController {

    /** 吊销端点要求的管理权限码 */
    private static final String ADMIN_AUTHORITY = "admin:*";

    private final RemoteUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final LoginHistoryService loginHistoryService;

    public LoginController(RemoteUserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService tokenService,
                           LoginAttemptService loginAttemptService,
                           LoginHistoryService loginHistoryService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.loginHistoryService = loginHistoryService;
    }

    @PostMapping("/token")
    @Operation(summary = "账号密码登录",
            description = "校验账号密码，成功后签发 access_token 与 refresh_token；连续失败 5 次锁定 15 分钟。")
    @SecurityRequirements
    public R<JwtTokenService.TokenResult> login(@RequestBody @Validated LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String clientIp = loginHistoryService.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. 检查是否被锁定
        if (loginAttemptService.isLocked(username)) {
            loginHistoryService.record(null, username, false, "LOGIN_LOCKED", httpRequest);
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        // 2. 查用户
        SimpleUser user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            loginAttemptService.recordFailure(username);
            loginHistoryService.record(null, username, false, "USER_NOT_FOUND", httpRequest);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            loginAttemptService.recordFailure(username);
            loginHistoryService.record(user.getUserId(), username, false, "ACCOUNT_DISABLED", httpRequest);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            loginHistoryService.record(user.getUserId(), username, false, "BAD_PASSWORD", httpRequest);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 5. 登录成功：清除失败计数、记录历史、回写最后登录信息
        loginAttemptService.clearFailures(username);
        loginHistoryService.record(user.getUserId(), username, true, null, httpRequest);
        userService.updateLoginInfo(user.getUserId(), clientIp);

        return R.ok(tokenService.issue(user, clientIp, userAgent));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌",
            description = "用 refresh_token 换取新的 access_token 与 refresh_token，旧 refresh_token 立即失效。")
    @SecurityRequirements
    public R<JwtTokenService.TokenResult> refresh(@RequestBody @Validated RefreshRequest request) {
        return R.ok(tokenService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出",
            description = "将当前 access_token 加入黑名单并吊销该用户全部 refresh_token；需在 Authorization 头携带 Bearer token。")
    public R<Void> logout(HttpServletRequest httpRequest) {
        String authorization = httpRequest.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        String tokenValue = authorization.substring(SecurityConstants.BEARER_PREFIX.length());
        tokenService.blacklistAndRevoke(tokenValue);
        return R.ok();
    }

    /**
     * 管理员强制吊销指定用户的全部 refresh_token。
     *
     * <p>需 {@code admin:*} 权限（校验网关透传的 authorities 头）。网关已对本端点
     * 强制 JWT 校验，此处再校验权限，形成纵深防御。</p>
     */
    @PostMapping("/revoke/{userId}")
    @Operation(summary = "吊销指定用户全部令牌", description = "管理员强制下线指定用户，需 admin:* 权限。")
    public R<Void> revoke(@PathVariable Long userId, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        tokenService.revokeAllForUser(userId);
        return R.ok();
    }

    /** 校验当前请求是否具备管理员权限（读取网关透传的 authorities 头），不具备则抛 403 */
    private void requireAdmin(HttpServletRequest httpRequest) {
        String authorities = httpRequest.getHeader(SecurityConstants.CLAIM_AUTHORITIES);
        if (authorities == null || !hasAdminAuthority(authorities)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    /** 判断逗号分隔的权限串中是否包含管理权限码 {@link #ADMIN_AUTHORITY} */
    private static boolean hasAdminAuthority(String authoritiesHeader) {
        for (String authority : authoritiesHeader.split(",")) {
            if (ADMIN_AUTHORITY.equals(authority.trim())) {
                return true;
            }
        }
        return false;
    }

    @Data
    @Schema(description = "登录请求")
    public static class LoginRequest {

        @Schema(description = "用户名", example = "admin")
        @NotBlank(message = "用户名不能为空")
        private String username;

        @Schema(description = "密码", example = "123456")
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    @Schema(description = "刷新令牌请求")
    public static class RefreshRequest {

        @Schema(description = "刷新令牌")
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }
}
