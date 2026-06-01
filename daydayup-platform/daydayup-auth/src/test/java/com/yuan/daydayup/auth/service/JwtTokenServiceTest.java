package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.RefreshToken;
import com.yuan.daydayup.auth.mapper.RefreshTokenMapper;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;
    private RefreshTokenMapper refreshTokenMapper;
    private RemoteUserService userService;
    private TokenBlacklistService blacklistService;
    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        jwtEncoder = mock(JwtEncoder.class);
        jwtDecoder = mock(JwtDecoder.class);
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        userService = mock(RemoteUserService.class);
        blacklistService = mock(TokenBlacklistService.class);
        service = new JwtTokenService(jwtEncoder, jwtDecoder, refreshTokenMapper,
                userService, blacklistService, 7200L, 604800L);
    }

    /** 伪造（验签失败）的 token 登出时必须被拒绝，且不得吊销任何用户 token */
    @Test
    void shouldRejectForgedTokenOnLogout() {
        when(jwtDecoder.decode("forged")).thenThrow(new BadJwtException("invalid signature"));

        assertThatThrownBy(() -> service.blacklistAndRevoke("forged"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID.getCode()));

        verify(blacklistService, never()).blacklist(anyString(), anyLong());
        verify(refreshTokenMapper, never()).update(any(), any());
    }

    /** 合法 token 登出：加入黑名单并吊销该用户全部 refresh_token */
    @Test
    void shouldBlacklistAndRevokeOnValidToken() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("valid")
                .header("alg", "RS256")
                .claim("jti", "jti-1")
                .subject("admin")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim(SecurityConstants.CLAIM_USER_ID, 1L)
                .build();
        when(jwtDecoder.decode("valid")).thenReturn(jwt);

        service.blacklistAndRevoke("valid");

        verify(blacklistService).blacklist(eq("jti-1"), longThat(ttl -> ttl > 0 && ttl <= 3600));
        verify(refreshTokenMapper).update(any(), any());
    }

    /** 用户被禁用后即便持有有效 refresh_token 也不能刷新，并吊销其全部 token */
    @Test
    void shouldRejectRefreshForDisabledUser() {
        RefreshToken record = new RefreshToken();
        record.setUserId(1L);
        record.setUsername("admin");
        record.setRevoked(0);
        record.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(record);
        when(userService.findByUsername("admin")).thenReturn(Optional.of(
                SimpleUser.builder().userId(1L).username("admin").status(0).build()));

        assertThatThrownBy(() -> service.refresh("raw-token"))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode()));

        // revokeAllForUser 通过 update(entity, wrapper) 吊销该用户全部 token
        verify(refreshTokenMapper).update(any(), any());
    }
}
