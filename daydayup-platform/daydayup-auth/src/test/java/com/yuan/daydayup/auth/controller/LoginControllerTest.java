package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.service.JwtTokenService;
import com.yuan.daydayup.auth.service.LoginAttemptService;
import com.yuan.daydayup.auth.service.LoginHistoryService;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    private RemoteUserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService tokenService;
    private LoginAttemptService loginAttemptService;
    private LoginHistoryService loginHistoryService;
    private LoginController controller;

    @BeforeEach
    void setUp() {
        userService = mock(RemoteUserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(JwtTokenService.class);
        loginAttemptService = mock(LoginAttemptService.class);
        loginHistoryService = mock(LoginHistoryService.class);
        controller = new LoginController(userService, passwordEncoder, tokenService,
                loginAttemptService, loginHistoryService);
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
    void shouldClearFailuresAndUpdateLoginInfoOnSuccess() {
        when(loginAttemptService.isLocked("admin")).thenReturn(false);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").password("encoded")
                        .authorities(Set.of("admin:*")).status(1).build()));
        when(passwordEncoder.matches("admin", "encoded")).thenReturn(true);
        when(tokenService.issue(any(SimpleUser.class), any(), any())).thenReturn(
                new JwtTokenService.TokenResult("access", "refresh", 7200L, "Bearer"));

        controller.login(request("admin", "admin"), new MockHttpServletRequest());

        verify(loginAttemptService).clearFailures("admin");
        verify(loginHistoryService).record(eq(1L), eq("admin"), eq(true), isNull(), any());
        verify(userService).updateLoginInfo(eq(1L), any());
    }

    @Test
    void shouldRecordFailureReasonOnLockedAccount() {
        when(loginAttemptService.isLocked("admin")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(request("admin", "admin"), new MockHttpServletRequest()))
                .isInstanceOf(BizException.class);
        verify(loginHistoryService).record(isNull(), eq("admin"), eq(false), eq("LOGIN_LOCKED"), any());
    }

    @Test
    void shouldRejectRevokeWithoutAdminAuthority() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThatThrownBy(() -> controller.revoke(1L, httpRequest))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.FORBIDDEN.getCode()));
        verify(tokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    void shouldRejectRevokeWithNonAdminAuthority() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(SecurityConstants.CLAIM_AUTHORITIES, "user:read,order:write");

        assertThatThrownBy(() -> controller.revoke(1L, httpRequest))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.FORBIDDEN.getCode()));
        verify(tokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    void shouldAllowRevokeWithAdminAuthority() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader(SecurityConstants.CLAIM_AUTHORITIES, "user:read,admin:*");

        controller.revoke(1L, httpRequest);

        verify(tokenService).revokeAllForUser(1L);
    }
}
