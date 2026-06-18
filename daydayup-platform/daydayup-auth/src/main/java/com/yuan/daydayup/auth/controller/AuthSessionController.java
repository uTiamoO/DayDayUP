package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OIDC 授权码流程的后端会话 API。
 *
 * <p>本控制器只提供 JSON API，不渲染登录页。独立 auth-ui 在浏览器跳转到
 * /oauth2/authorize 后，调用本接口建立 Spring Security 会话，再回到授权流程。</p>
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class AuthSessionController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @GetMapping("/login-required")
    public ResponseEntity<R<Void>> loginRequired() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.fail(ErrorCode.UNAUTHORIZED));
    }

    @PostMapping("/login")
    public ResponseEntity<R<SessionLoginVO>> login(@Valid @RequestBody SessionLoginDTO dto,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());
        if (!passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.fail(ErrorCode.UNAUTHORIZED));
        }
        if (!userDetails.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(R.fail(ErrorCode.FORBIDDEN));
        }

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        SessionLoginVO vo = new SessionLoginVO();
        vo.setAuthenticated(true);
        vo.setUsername(userDetails.getUsername());
        return ResponseEntity.ok(R.ok(vo));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return R.ok();
    }

    @Data
    public static class SessionLoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class SessionLoginVO {
        private Boolean authenticated;
        private String username;
    }
}
