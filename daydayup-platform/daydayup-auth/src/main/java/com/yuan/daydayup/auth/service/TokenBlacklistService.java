package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.core.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 黑名单服务
 *
 * <p>登出时把 access_token 的 jti 写入 Redis 黑名单；网关在 JWT 验签通过后会查询此黑名单，
 * 命中则拒绝放行——以此实现「JWT 在自然过期前主动失效」。</p>
 *
 * <p>key 与网关 {@code AuthGlobalFilter} 共用 {@link SecurityConstants#TOKEN_BLACKLIST_KEY_PREFIX} 前缀。</p>
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 将指定 jti 加入黑名单。
     *
     * @param jti        access_token 的唯一标识（JWT 的 jti claim）
     * @param ttlSeconds 黑名单存活秒数，应取 token 的剩余有效期，到期自动清理、避免无限堆积
     */
    public void blacklist(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(jti), "1", Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 判断指定 jti 是否已被列入黑名单。
     *
     * @param jti access_token 的唯一标识
     * @return true 表示该 token 已失效
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    /**
     * 构造黑名单 key，与网关共用同一前缀（{@link SecurityConstants#TOKEN_BLACKLIST_KEY_PREFIX}），
     * 确保认证中心写入、网关读取的 key 完全一致。
     */
    private static String key(String jti) {
        return SecurityConstants.TOKEN_BLACKLIST_KEY_PREFIX + jti;
    }
}
