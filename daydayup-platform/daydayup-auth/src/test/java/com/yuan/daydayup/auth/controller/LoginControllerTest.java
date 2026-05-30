package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.entity.LoginHistory;
import com.yuan.daydayup.auth.mapper.LoginHistoryMapper;
import com.yuan.daydayup.auth.service.JwtTokenService;
import com.yuan.daydayup.auth.service.LoginAttemptService;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    private RemoteUserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService tokenService;
    private LoginAttemptService loginAttemptService;
    private LoginHistoryMapper loginHistoryMapper;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        userService = mock(RemoteUserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(JwtTokenService.class);
        loginAttemptService = mock(LoginAttemptService.class);
        loginHistoryMapper = mock(LoginHistoryMapper.class);
        controller = new LoginController(userService, passwordEncoder, tokenService,
                loginAttemptService, loginHistoryMapper);
    }

    private LoginController.LoginRequest request(String username, String password) {
        LoginController.LoginRequest req = new LoginController.LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    @Test
    void shouldRejectLockedAccount() {
        when(loginAttemptService.isLocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(request("admin", "admin"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.LOGIN_LOCKED.getCode()));
    }

    @Test
    void shouldRejectUnknownUser() {
        when(loginAttemptService.isLocked("nobody")).thenReturn(false);
        when(userService.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(request("nobody", "pass"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.LOGIN_FAILED.getCode()));
        verify(loginAttemptService).recordFailure("nobody");
    }

    @Test
    void shouldRejectDisabledUser() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(0).build()));

        assertThatThrownBy(() -> controller.login(request("admin", "admin"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.LOGIN_FAILED.getCode()));
        verify(loginAttemptService).recordFailure("admin");
    }

    @Test
    void shouldRecordFailureOnBadPassword() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(1).build()));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(request("admin", "wrong"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.LOGIN_FAILED.getCode()));
        verify(loginAttemptService).recordFailure("admin");
    }

    @Test
    void shouldClearFailuresOnSuccess() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(1).build()));
        when(passwordEncoder.matches("admin", "encoded")).thenReturn(true);
        when(tokenService.issue(any(SimpleUser.class), any(), any())).thenReturn(
                new JwtTokenService.TokenResult("access", "refresh", 7200L, "Bearer"));

        controller.login(request("admin", "admin"), new MockHttpServletRequest());

        verify(loginAttemptService).clearFailures("admin");
        verify(loginHistoryMapper).insert(org.mockito.ArgumentMatchers.<LoginHistory>argThat(
                h -> h.getSuccess() == 1));
    }

    @Test
    void shouldRecordFailureReasonOnLockedAccount() {
        when(loginAttemptService.isLocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(request("admin", "admin"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class);
        verify(loginHistoryMapper).insert(org.mockito.ArgumentMatchers.<LoginHistory>argThat(
                h -> "LOGIN_LOCKED".equals(h.getFailureReason()) && h.getUserId() == null));
    }
}
