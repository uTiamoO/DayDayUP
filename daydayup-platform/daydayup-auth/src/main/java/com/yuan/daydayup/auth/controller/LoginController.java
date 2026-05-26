package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.service.JwtTokenService;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
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
public class LoginController {

    private final RemoteUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public LoginController(RemoteUserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService tokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public R<JwtTokenService.TokenResult> login(@RequestBody @Validated LoginRequest request) {
        SimpleUser user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        return R.ok(tokenService.issue(user));
    }

    @PostMapping("/refresh")
    public R<JwtTokenService.TokenResult> refresh(@RequestBody @Validated RefreshRequest request) {
        return R.ok(tokenService.refresh(request.getRefreshToken()));
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
