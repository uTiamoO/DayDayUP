package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.redis.util.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

/**
 * 登录失败锁定服务
 *
 * <p>基于 Redis 对「用户名维度」的连续登录失败计数，达到 {@link #MAX_ATTEMPTS} 次后
 * 锁定 {@link #LOCK_DURATION}，期间拒绝登录，用于缓解暴力破解。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** 触发锁定的连续失败次数阈值 */
    private static final int MAX_ATTEMPTS = 5;

    /** 锁定时长：达到阈值后计数 key 的存活时间 */
    private static final Duration LOCK_DURATION = Duration.ofSeconds(900);

    /** 缓存 key 的模块段 */
    private static final String MODULE = "auth";

    /** 缓存 key 的业务段 */
    private static final String BIZ = "login:fail";

    /**
     * 原子地「计数 +1，并在首次出现时设置过期时间」。
     *
     * <p>用 Lua 保证 INCR 与 EXPIRE 不可分割，避免 INCR 成功后 EXPIRE 失败
     * 导致计数 key 永不过期、账号被永久锁定。脚本返回当前失败次数。</p>
     */
    private static final DefaultRedisScript<Long> INCR_AND_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 判断用户名当前是否处于锁定状态。
     *
     * @param username 登录用户名
     * @return 连续失败次数达到阈值时返回 true
     */
    public boolean isLocked(String username) {
        String count = redisTemplate.opsForValue().get(key(username));
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    /**
     * 记录一次登录失败：失败计数 +1，首次失败时开始计时锁定窗口。
     *
     * @param username 登录用户名
     */
    public void recordFailure(String username) {
        redisTemplate.execute(INCR_AND_EXPIRE,
                Collections.singletonList(key(username)),
                String.valueOf(LOCK_DURATION.getSeconds()));
    }

    /**
     * 清除某用户名的失败计数（登录成功后调用）。
     *
     * @param username 登录用户名
     */
    public void clearFailures(String username) {
        redisTemplate.delete(key(username));
    }

    /** 计数 key，形如 {@code daydayup:auth:login:fail:<username>} */
    private static String key(String username) {
        return CacheKeys.of(MODULE, BIZ, username);
    }
}
