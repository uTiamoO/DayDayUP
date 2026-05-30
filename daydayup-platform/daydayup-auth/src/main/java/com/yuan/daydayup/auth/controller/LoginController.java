package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.entity.LoginHistory;
import com.yuan.daydayup.auth.mapper.LoginHistoryMapper;
import com.yuan.daydayup.auth.service.JwtTokenService;
import com.yuan.daydayup.auth.service.LoginAttemptService;
import com.yuan.daydayup.auth.service.TokenBlacklistService;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.result.R;
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

import java.time.LocalDateTime;

/**
 * 登录接口
 *
 * <p>P1 简化实现：账号密码 → 直接签发 JWT。</p>
 * <p>P4 阶段将切换为标准 OAuth2 授权流程。</p>
 */
@RestController
@RequestMapping("/oauth2")
@Validated
public class LoginController {

    private final RemoteUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final LoginHistoryMapper loginHistoryMapper;

    public LoginController(RemoteUserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService tokenService,
                           LoginAttemptService loginAttemptService,
                           LoginHistoryMapper loginHistoryMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.loginHistoryMapper = loginHistoryMapper;
    }

    @PostMapping("/token")
    public R<JwtTokenService.TokenResult> login(@RequestBody @Validated LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. 检查是否被锁定
        if (loginAttemptService.isLocked(username)) {
            recordLoginHistory(null, username, clientIp, userAgent, false, "LOGIN_LOCKED");
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        // 2. 查用户
        SimpleUser user = userService.findByUsername(username)
                .orElse(null);
        if (user == null) {
            loginAttemptService.recordFailure(username);
            recordLoginHistory(null, username, clientIp, userAgent, false, "USER_NOT_FOUND");
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            loginAttemptService.recordFailure(username);
            recordLoginHistory(user.getUserId(), username, clientIp, userAgent, false, "ACCOUNT_DISABLED");
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            recordLoginHistory(user.getUserId(), username, clientIp, userAgent, false, "BAD_PASSWORD");
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 5. 登录成功
        loginAttemptService.clearFailures(username);
        recordLoginHistory(user.getUserId(), username, clientIp, userAgent, true, null);

        return R.ok(tokenService.issue(user, clientIp, userAgent));
    }

    @PostMapping("/refresh")
    public R<JwtTokenService.TokenResult> refresh(@RequestBody @Validated RefreshRequest request) {
        return R.ok(tokenService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest httpRequest) {
        String authorization = httpRequest.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        String tokenValue = authorization.substring(SecurityConstants.BEARER_PREFIX.length());
        tokenService.blacklistAndRevoke(tokenValue);
        return R.ok();
    }

    @PostMapping("/revoke/{userId}")
    public R<Void> revoke(@PathVariable Long userId) {
        tokenService.revokeAllForUser(userId);
        return R.ok();
    }

    private void recordLoginHistory(Long userId, String username, String clientIp,
                                     String userAgent, boolean success, String failureReason) {
        LoginHistory history = new LoginHistory();
        history.setUserId(userId);
        history.setUsername(username);
        history.setLoginAt(LocalDateTime.now());
        history.setClientIp(clientIp);
        history.setUserAgent(userAgent);
        history.setSuccess(success ? 1 : 0);
        history.setFailureReason(failureReason);
        loginHistoryMapper.insert(history);
    }

    private static String getClientIp(HttpServletRequest request) {
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

    @Data
    public static class LoginRequest {

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RefreshRequest {

        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }
}
