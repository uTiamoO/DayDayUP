package com.yuan.daydayup.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    @Test
    public void mainsss() throws Exception {
        String enc = "UhQTfQq/qXGCKPd5D+cjxB7Y0AzwiFMYBmcN5nIm2PbFCQr/XNBAKLI/IO5msa3d";

        byte[] key = "f041c49714d39908".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);

        byte[] cipherBytes = Base64.getDecoder().decode(enc);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new IvParameterSpec(iv)
        );

        byte[] plain = cipher.doFinal(cipherBytes);

        System.out.println(new String(plain, StandardCharsets.UTF_8));
    }
}
