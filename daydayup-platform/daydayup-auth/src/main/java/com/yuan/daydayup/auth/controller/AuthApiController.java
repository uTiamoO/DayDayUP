package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.service.DayDayUpUser;
import com.yuan.daydayup.auth.service.DirectTokenService;
import com.yuan.daydayup.auth.service.TokenBlacklistService;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 平台登录 API（不走 SAS OAuth2 流程）。
 *
 * <p>前端直接 POST 用户名密码，返回 access_token + refresh_token。
 * 无需 client_id / client_secret / 授权码。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final DirectTokenService directTokenService;
    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 登录：用户名 + 密码 → token
     */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());

        if (!passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        if (!userDetails.isEnabled()) {
            return R.fail(403, "账号已停用");
        }

        DayDayUpUser user = (DayDayUpUser) userDetails;
        String accessToken = directTokenService.generateAccessToken(
                user.getUserId(), user.getUsername(), user.getAuthorities());
        String refreshToken = directTokenService.generateRefreshToken(
                user.getUserId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(1800); // 30 分钟
        return R.ok(vo);
    }

    /**
     * 刷新 token：用 refresh_token 换新的 access_token
     */
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshDTO dto) {
        try {
            Jwt jwt = jwtDecoder.decode(dto.getRefreshToken());

            // 校验是否为 refresh_token
            String type = jwt.getClaimAsString("type");
            if (!"refresh".equals(type)) {
                return R.fail(401, "无效的 refresh_token");
            }

            if (jwt.getId() != null && tokenBlacklistService.isBlacklisted(jwt.getId())) {
                return R.fail(401, "refresh_token 已失效");
            }

            String username = jwt.getSubject();
            Long userId = jwt.getClaim(SecurityConstants.CLAIM_USER_ID);

            // 重新加载用户信息（可能权限已变更）
            DayDayUpUser user = (DayDayUpUser) userDetailsService.loadUserByUsername(username);

            String accessToken = directTokenService.generateAccessToken(
                    user.getUserId(), user.getUsername(), user.getAuthorities());
            String newRefreshToken = directTokenService.generateRefreshToken(
                    user.getUserId(), user.getUsername());

            LoginVO vo = new LoginVO();
            vo.setAccessToken(accessToken);
            vo.setRefreshToken(newRefreshToken);
            vo.setExpiresIn(1800);
            return R.ok(vo);
        } catch (Exception e) {
            return R.fail(401, "refresh_token 已过期或无效");
        }
    }

    /**
     * 登出：将所提交 token 的 jti 加入黑名单。网关在验签后查询黑名单并拒绝放行（access_token），
     * refresh_token 的失效在 /api/refresh 处校验。
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) LogoutDTO dto) {
        if (dto != null) {
            blacklist(dto.getAccessToken());
            blacklist(dto.getRefreshToken());
        }
        return R.ok();
    }

    /** 解析 token 取 jti，按剩余有效期写入黑名单；无效或无 jti 的 token 静默跳过。 */
    private void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (jwt.getId() == null || jwt.getExpiresAt() == null) {
                return;
            }
            long ttlSeconds = jwt.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
            if (ttlSeconds > 0) {
                tokenBlacklistService.blacklist(jwt.getId(), ttlSeconds);
            }
        } catch (Exception ignored) {
            // 无效或已过期的 token 无需拉黑
        }
    }

    // ==================== DTO / VO ====================

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RefreshDTO {
        @NotBlank(message = "refresh_token 不能为空")
        private String refreshToken;
    }

    @Data
    public static class LogoutDTO {
        private String accessToken;
        private String refreshToken;
    }

    @Data
    public static class LoginVO {
        private String accessToken;
        private String refreshToken;
        /** access_token 有效期（秒） */
        private Integer expiresIn;
    }
}
