package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jwt.SignedJWT;
import com.yuan.daydayup.auth.entity.RefreshToken;
import com.yuan.daydayup.auth.mapper.RefreshTokenMapper;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * JWT 签发服务（含 Refresh Token）
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenMapper refreshTokenMapper;
    private final RemoteUserService userService;
    private final TokenBlacklistService blacklistService;

    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           RefreshTokenMapper refreshTokenMapper,
                           RemoteUserService userService,
                           TokenBlacklistService blacklistService,
                           @Value("${daydayup.auth.access-token-ttl-seconds:7200}") long accessTokenTtlSeconds,
                           @Value("${daydayup.auth.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userService = userService;
        this.blacklistService = blacklistService;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    /**
     * 签发 access_token + refresh_token（无 HTTP 上下文时使用）
     */
    public TokenResult issue(SimpleUser user) {
        return issue(user, null, null);
    }

    /**
     * 签发 access_token + refresh_token，记录客户端信息
     */
    public TokenResult issue(SimpleUser user, String clientIp, String userAgent) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user, clientIp, userAgent);
        return new TokenResult(accessToken, refreshToken, accessTokenTtlSeconds, "Bearer");
    }

    /**
     * 将 token 加入黑名单并吊销该用户所有 refresh_token
     */
    public void blacklistAndRevoke(String tokenValue) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(tokenValue);
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            Instant expiration = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
            long ttlSeconds = Math.max(0, Instant.now().until(expiration, ChronoUnit.SECONDS));
            if (jti != null && ttlSeconds > 0) {
                blacklistService.blacklist(jti, ttlSeconds);
            }
            Long userId = signedJWT.getJWTClaimsSet().getLongClaim(SecurityConstants.CLAIM_USER_ID);
            if (userId != null) {
                revokeAllForUser(userId);
            }
        } catch (ParseException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 吊销指定用户的所有 refresh_token
     */
    public void revokeAllForUser(Long userId) {
        RefreshToken update = new RefreshToken();
        update.setRevoked(1);
        update.setRevokedAt(LocalDateTime.now());
        refreshTokenMapper.update(update,
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getUserId, userId)
                        .eq(RefreshToken::getRevoked, 0));
    }

    /**
     * 刷新 token：校验 refresh_token 后签发新的 access_token + refresh_token
     */
    public TokenResult refresh(String refreshTokenValue) {
        String hash = sha256(refreshTokenValue);

        RefreshToken record = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, hash)
                        .eq(RefreshToken::getRevoked, 0));

        if (record == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        if (record.getExpiresAt().isBefore(now)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 吊销旧 refresh_token
        record.setRevoked(1);
        record.setRevokedAt(now);
        refreshTokenMapper.updateById(record);

        // 签发新 token 对（通过 username 查回完整用户信息含 authorities）
        SimpleUser user = userService.findByUsername(record.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.TOKEN_INVALID));
        return issue(user, record.getClientIp(), record.getUserAgent());
    }

    private String createAccessToken(SimpleUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("daydayup-auth")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim(SecurityConstants.CLAIM_USER_ID, user.getUserId())
                .claim(SecurityConstants.CLAIM_USERNAME, user.getUsername())
                .claim(SecurityConstants.CLAIM_AUTHORITIES, user.getAuthorities())
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return jwt.getTokenValue();
    }

    private String createRefreshToken(SimpleUser user, String clientIp, String userAgent) {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String hash = sha256(rawToken);

        RefreshToken record = new RefreshToken();
        record.setUserId(user.getUserId());
        record.setUsername(user.getUsername());
        record.setTokenHash(hash);
        record.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenTtlSeconds));
        record.setRevoked(0);
        record.setClientIp(clientIp);
        record.setUserAgent(userAgent);
        refreshTokenMapper.insert(record);

        return rawToken;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record TokenResult(String accessToken, String refreshToken, long expiresIn, String tokenType) {
    }
}
