package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.core.constant.SecurityConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 用户权限版本管理服务（auth 模块，身份数据拥有方）
 *
 * <p>当用户的权限被降级（角色移除、权限收回、账号禁用）时，在 Redis 中记录降级时间戳。
 * 网关校验 JWT 时，对比 token 的签发时间（iat）是否早于该时间戳：
 * 若是则说明该 token 携带的是旧（更高）权限，予以拒绝，实现「降级即时生效」。</p>
 *
 * <p>权限新增（升级）时不调用此服务——旧 token 的权限是新权限的子集，不存在越权风险，
 * 用户在下次 token 刷新时自然获得新权限。这是安全性与用户体验的折中。</p>
 *
 * <p>Redis key：{@link SecurityConstants#PERM_CHANGED_KEY_PREFIX} + userId；
 * 值：权限降级时间的 epoch 秒数；
 * TTL：与 access_token 有效期对齐（默认 2 小时），过期后自动清理。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionVersionService {

    /** 默认 TTL 与 access_token 有效期对齐，过期后无需再拦截 */
    private static final long DEFAULT_TTL_HOURS = 2;

    private final StringRedisTemplate redisTemplate;

    /**
     * 标记用户权限被降级（禁用、角色移除等），强制其已签发的旧 token 失效。
     *
     * @param userId 用户 ID
     */
    public void markPermissionRevoked(Long userId) {
        if (userId == null) {
            return;
        }
        String key = SecurityConstants.PERM_CHANGED_KEY_PREFIX + userId;
        String nowEpochSecond = String.valueOf(Instant.now().getEpochSecond());
        redisTemplate.opsForValue().set(key, nowEpochSecond, DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }
}
