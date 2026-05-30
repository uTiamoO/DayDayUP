package com.yuan.daydayup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void shouldIncrementFailureCount() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.recordFailure("admin");
        verify(ops).increment(startsWith("daydayup:auth:login:fail:"));
        verify(redis).expire(anyString(), eq(Duration.ofSeconds(900)));
    }

    @Test
    void shouldClearFailureCount() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginAttemptService service = new LoginAttemptService(redis);

        service.clearFailures("admin");
        verify(redis).delete("daydayup:auth:login:fail:admin");
    }
}
