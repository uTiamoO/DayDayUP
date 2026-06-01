package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.auth.entity.RefreshToken;
import com.yuan.daydayup.auth.mapper.RefreshTokenMapper;
import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * JWT 令牌服务
 *
 * <p>负责 access_token / refresh_token 的签发、刷新（轮换）、登出（黑名单 + 吊销）
 * 与按用户批量吊销。</p>
 *
 * <ul>
 *   <li>access_token：RS256 签名的 JWT，携带 uid / username / authorities claim。</li>
 *   <li>refresh_token：随机串，仅以 SHA-256 哈希入库，刷新时采用「一次性轮换」策略。</li>
 * </ul>
 */
@Service
public class JwtTokenService {

    /** JWT 签发器（RS256 私钥签名） */
    private final JwtEncoder jwtEncoder;

    /** JWT 校验器（公钥验签）：登出时用于解析「可信」token，拒绝伪造 */
    private final JwtDecoder jwtDecoder;

    private final RefreshTokenMapper refreshTokenMapper;
    private final RemoteUserService userService;
    private final TokenBlacklistService blacklistService;

    /** access_token 有效期（秒） */
    private final long accessTokenTtlSeconds;

    /** refresh_token 有效期（秒） */
    private final long refreshTokenTtlSeconds;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           JwtDecoder jwtDecoder,
                           RefreshTokenMapper refreshTokenMapper,
                           RemoteUserService userService,
                           TokenBlacklistService blacklistService,
                           @Value("${daydayup.auth.access-token-ttl-seconds:7200}") long accessTokenTtlSeconds,
                           @Value("${daydayup.auth.refresh-token-ttl-seconds:604800}") long refreshTokenTtlSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userService = userService;
        this.blacklistService = blacklistService;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    /**
     * 签发 access_token + refresh_token（无 HTTP 上下文时使用）。
     *
     * @param user 目标用户
     * @return 令牌对
     */
    public TokenResult issue(SimpleUser user) {
        return issue(user, null, null);
    }

    /**
     * 签发 access_token + refresh_token，并在 refresh_token 记录上保存客户端信息。
     *
     * @param user      目标用户
     * @param clientIp  客户端 IP（可为 null）
     * @param userAgent 客户端 User-Agent（可为 null）
     * @return 令牌对
     */
    public TokenResult issue(SimpleUser user, String clientIp, String userAgent) {
        String accessToken = createAccessToken(user);
        String refreshToken = createRefreshToken(user, clientIp, userAgent);
        return new TokenResult(accessToken, refreshToken, accessTokenTtlSeconds, "Bearer");
    }

    /**
     * 将 access_token 加入黑名单并吊销该用户所有 refresh_token。
     *
     * <p>必须先「验签」再处理：只接受本认证中心签发且未过期的 token，
     * 防止伪造 token 借此吊销任意用户（强制下线攻击）。</p>
     *
     * @param tokenValue access_token 原始串
     * @throws BizException token 无效（验签失败 / 已过期 / 格式错误）时抛出
     */
    public void blacklistAndRevoke(String tokenValue) {
        // 1. 验签解析：验签失败 / 已过期 / 格式非法都会抛 JwtException，统一转为 TOKEN_INVALID
        //    —— 关键安全点：绝不能用「只解析不验签」的方式，否则伪造 token 也能触发吊销
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(tokenValue);
        } catch (JwtException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 2. 把当前 access_token 的 jti 加入黑名单，存活时间取其「剩余有效期」
        //    —— 到期自动清理，既能让 token 立即失效又不会让黑名单无限膨胀
        String jti = jwt.getId();
        Instant expiresAt = jwt.getExpiresAt();
        if (jti != null && expiresAt != null) {
            long ttlSeconds = Math.max(0, Instant.now().until(expiresAt, ChronoUnit.SECONDS));
            if (ttlSeconds > 0) {
                blacklistService.blacklist(jti, ttlSeconds);
            }
        }

        // 3. 吊销该用户全部 refresh_token，否则对方可立即用 refresh_token 换回新的 access_token
        Long userId = readUserId(jwt);
        if (userId != null) {
            revokeAllForUser(userId);
        }
    }

    /**
     * 吊销指定用户的所有未失效 refresh_token。
     *
     * @param userId 用户 ID
     */
    public void revokeAllForUser(Long userId) {
        // 批量 UPDATE：把该用户所有 revoked=0 的记录置为已吊销（带吊销时间）
        RefreshToken update = new RefreshToken();
        update.setRevoked(1);
        update.setRevokedAt(LocalDateTime.now());
        refreshTokenMapper.update(update,
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getUserId, userId)
                        .eq(RefreshToken::getRevoked, 0));
    }

    /**
     * 刷新令牌：校验 refresh_token 后轮换签发新的 access_token + refresh_token。
     *
     * <p>流程：哈希比对查库 → 校验未吊销且未过期 → 吊销旧 refresh_token →
     * 回查用户（含最新权限与状态）→ 校验未被禁用 → 签发新令牌对。</p>
     *
     * @param refreshTokenValue 客户端持有的 refresh_token 明文
     * @return 新的令牌对
     * @throws BizException refresh_token 无效 / 过期，或用户已被禁用
     */
    public TokenResult refresh(String refreshTokenValue) {
        // 1. 用 refresh_token 的 SHA-256 哈希查库（库里只存哈希、不存明文），且只取未吊销的记录
        String hash = sha256(refreshTokenValue);
        RefreshToken record = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, hash)
                        .eq(RefreshToken::getRevoked, 0));

        // 2. 记录不存在（伪造 / 已吊销）→ 拒绝
        if (record == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 记录已过期 → 拒绝
        LocalDateTime now = LocalDateTime.now();
        if (record.getExpiresAt().isBefore(now)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 吊销旧 refresh_token（一次性轮换：旧令牌用一次即作废，降低泄露后被重放的风险）
        record.setRevoked(1);
        record.setRevokedAt(now);
        refreshTokenMapper.updateById(record);

        // 5. 通过 username 回查用户，拿到「最新」权限与状态，而非沿用旧令牌里的过时快照
        SimpleUser user = userService.findByUsername(record.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.TOKEN_INVALID));

        // 6. 用户在签发后被禁用：拒绝刷新并吊销其全部 refresh_token，避免封禁被绕过
        if (user.getStatus() == null || user.getStatus() != 1) {
            revokeAllForUser(user.getUserId());
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 7. 校验全部通过，签发新的令牌对（沿用原客户端 IP / UA）
        return issue(user, record.getClientIp(), record.getUserAgent());
    }

    /** 生成 access_token：RS256 签名的 JWT，携带 uid / username / authorities claim */
    private String createAccessToken(SimpleUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS);

        // 组装标准声明（iss/iat/exp/jti/sub）+ 自定义声明（uid/username/authorities）
        // jti 用随机 UUID，保证每个 token 唯一，是黑名单能精确定位单个 token 的前提
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

        // 以 RS256 私钥签名，生成紧凑序列化（header.payload.signature）的 JWT
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return jwt.getTokenValue();
    }

    /**
     * 生成 refresh_token：返回随机明文给客户端，数据库仅存其 SHA-256 哈希，
     * 避免库被读取后 refresh_token 直接泄露。
     */
    private String createRefreshToken(SimpleUser user, String clientIp, String userAgent) {
        // 随机明文令牌仅本次返回给客户端；入库的是其哈希，二者通过 sha256 关联
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

    /** 从 JWT 的 uid claim 解析用户 ID，兼容 Number 与 String 两种表示 */
    private static Long readUserId(Jwt jwt) {
        Object value = jwt.getClaim(SecurityConstants.CLAIM_USER_ID);
        // 经 JSON 序列化后数字 claim 可能是 Integer / Long，统一按 Number 取 long
        if (value instanceof Number number) {
            return number.longValue();
        }
        // 兜底兼容字符串形式的 uid
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 计算 SHA-256 十六进制摘要，用于 refresh_token 的哈希存储与比对 */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 令牌签发结果。
     *
     * @param accessToken  访问令牌（JWT）
     * @param refreshToken 刷新令牌（随机串明文，仅本次返回）
     * @param expiresIn    access_token 有效期（秒）
     * @param tokenType    令牌类型，固定为 Bearer
     */
    public record TokenResult(String accessToken, String refreshToken, long expiresIn, String tokenType) {
    }
}
