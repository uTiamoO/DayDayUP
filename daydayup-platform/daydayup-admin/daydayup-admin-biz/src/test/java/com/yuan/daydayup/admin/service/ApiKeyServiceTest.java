package com.yuan.daydayup.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private ApiKeyService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        objectMapper = new ObjectMapper();
        service = new ApiKeyService(redisTemplate, objectMapper);
    }

    @Test
    void shouldNotStorePlaintextApiKeyInRedisKey() {
        String apiKey = service.createApiKey(1L, "admin", List.of("admin:perm"));

        // 捕获写入 Redis 的 Key
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), any(), anyLong(), any());

        String actualKey = keyCaptor.getValue();
        // 验证写入的 Key 绝不能直接包含明文 API Key
        assertThat(actualKey).doesNotContain(apiKey);
    }
}
