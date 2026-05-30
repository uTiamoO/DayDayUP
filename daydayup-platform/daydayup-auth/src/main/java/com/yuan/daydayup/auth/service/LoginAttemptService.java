package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.common.redis.util.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofSeconds(900);
    private static final String MODULE = "auth";
    private static final String BIZ = "login:fail";

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(String username) {
        String count = redisTemplate.opsForValue().get(key(username));
        return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String username) {
        String key = key(username);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, LOCK_DURATION);
        }
    }

    public void clearFailures(String username) {
        redisTemplate.delete(key(username));
    }

    private static String key(String username) {
        return CacheKeys.of(MODULE, BIZ, username);
    }
}
