package com.yuan.daydayup.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceTest {

    @Test
    void shouldBlacklistToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        TokenBlacklistService service = new TokenBlacklistService(redis);

        service.blacklist("jti-123", 3600L);

        verify(ops).set("daydayup:auth:token:blacklist:jti-123", "1", Duration.ofSeconds(3600));
    }

    @Test
    void shouldDetectBlacklistedToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("daydayup:auth:token:blacklist:jti-123")).thenReturn(true);
        TokenBlacklistService service = new TokenBlacklistService(redis);

        assertThat(service.isBlacklisted("jti-123")).isTrue();
    }

    @Test
    void shouldReturnFalseForCleanToken() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey("daydayup:auth:token:blacklist:jti-456")).thenReturn(false);
        TokenBlacklistService service = new TokenBlacklistService(redis);

        assertThat(service.isBlacklisted("jti-456")).isFalse();
    }
}
