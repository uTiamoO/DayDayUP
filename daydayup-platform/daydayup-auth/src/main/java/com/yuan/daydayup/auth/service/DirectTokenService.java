package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.core.constant.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 直接签发 JWT（不走 SAS OAuth2 流程）。
 *
 * <p>供平台自有登录页使用：前端 POST 用户名密码，本服务直接返回 access_token / refresh_token，
 * 无需 client_id、无需授权码流程。</p>
 */
@Service
public class DirectTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuerUrl;

    public DirectTokenService(JwtEncoder jwtEncoder,
                              @Value("${daydayup.auth.issuer-url:http://127.0.0.1:9200}") String issuerUrl) {
        this.jwtEncoder = jwtEncoder;
        this.issuerUrl = issuerUrl;
    }

    /**
     * 签发 access_token
     *
     * @param userId      用户 ID
     * @param username    用户名
     * @param authorities 权限列表
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        String authoritiesClaim = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUrl)
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .subject(username)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_AUTHORITIES, authoritiesClaim)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 签发 refresh_token（JWT 格式，有效期 7 天）
     */
    public String generateRefreshToken(Long userId, String username) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUrl)
                .issuedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .subject(username)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim("type", "refresh")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
