package com.yuan.daydayup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    @Test
    void shouldNotBlockWhenNoFailures() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(null);
        LoginAttemptService service = new LoginAttemptService(redis);

        assertThat(service.isLocked("admin")).isFalse();
    }

    @Test
    void shouldBlockAfterMaxAttempts() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn("5");
        LoginAttemptService service = new LoginAttemptService(redis);

        assertThat(service.isLocked("admin")).isTrue();
    }

    @Test
    void shouldIncrementFailureCountAtomically() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.recordFailure("admin");

        // 计数与过期通过单条 Lua 脚本原子执行，900 秒为锁定时长参数
        verify(redis).execute(
                any(RedisScript.class),
                eq(List.of("daydayup:auth:login:fail:admin")),
                eq("900"));
    }

    @Test
    void shouldClearFailureCount() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.clearFailures("admin");
        verify(redis).delete("daydayup:auth:login:fail:admin");
    }
}
