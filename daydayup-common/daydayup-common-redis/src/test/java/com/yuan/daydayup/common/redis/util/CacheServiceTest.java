package com.yuan.daydayup.common.redis.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

class CacheServiceTest {

    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = mock(ObjectMapper.class);
        cacheService = new CacheService(redisTemplate, objectMapper);
    }

    @Test
    void shouldNotUseKeysCommandForDeleteByPrefix() {
        // Mock SCAN 返回一个包含两个 key 的游标
        @SuppressWarnings("unchecked")
        Cursor<String> mockCursor = mock(Cursor.class);
        when(mockCursor.hasNext()).thenReturn(true, true, false);
        when(mockCursor.next()).thenReturn("test:a", "test:b");
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(mockCursor);

        cacheService.deleteByPrefix("test:");

        // 验证使用了 SCAN 而非 KEYS
        verify(redisTemplate, never()).keys(anyString());
        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate).delete(any(Collection.class));
    }
}
