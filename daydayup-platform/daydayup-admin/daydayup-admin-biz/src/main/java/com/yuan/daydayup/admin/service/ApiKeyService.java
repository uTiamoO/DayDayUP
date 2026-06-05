package com.yuan.daydayup.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * API Key 管理服务
 *
 * <p>管理服务间调用 / 定时任务 / 第三方集成使用的 API Key。
 * API Key 经 SHA-256 哈希后作为 Redis Key，不存储明文。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>定时任务（XXL-JOB）调用业务接口</li>
 *   <li>服务间 Feign 调用（无需用户 JWT）</li>
 *   <li>第三方系统集成</li>
 * </ul>
 *
 * <p>Redis key 格式：daydayup:apikey:{sha256(apiKey)} → JSON {userId, username, authorities}</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "daydayup:apikey:";
    private static final long DEFAULT_TTL_DAYS = 365;
    private static final HexFormat HEX = HexFormat.of();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 API Key
     *
     * @param userId      关联用户 ID
     * @param username    关联用户名
     * @param authorities 权限列表
     * @return 生成的 API Key（明文，仅本次返回；Redis 中仅存哈希）
     */
    public String createApiKey(Long userId, String username, List<String> authorities) {
        String apiKey = UUID.randomUUID().toString().replace("-", "");
        String hashedKey = KEY_PREFIX + hashApiKey(apiKey);

        Map<String, Object> info = Map.of(
                "userId", userId,
                "username", username,
                "authorities", authorities
        );

        try {
            String json = objectMapper.writeValueAsString(info);
            redisTemplate.opsForValue().set(hashedKey, json, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
            log.info("API Key 已创建: userId={}, username={}", userId, username);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("API Key 序列化失败", e);
        }

        return apiKey;
    }

    /**
     * 吊销 API Key
     *
     * @param apiKey 要吊销的 API Key（明文）
     * @return 是否成功吊销（false 表示 key 不存在）
     */
    public boolean revokeApiKey(String apiKey) {
        String hashedKey = KEY_PREFIX + hashApiKey(apiKey);
        Boolean deleted = redisTemplate.delete(hashedKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("API Key 已吊销: {}", maskKey(apiKey));
            return true;
        }
        return false;
    }

    /**
     * 查询 API Key 是否有效
     *
     * @param apiKey API Key（明文）
     * @return true=有效
     */
    public boolean isValid(String apiKey) {
        String hashedKey = KEY_PREFIX + hashApiKey(apiKey);
        return Boolean.TRUE.equals(redisTemplate.hasKey(hashedKey));
    }

    /**
     * 对 API Key 做 SHA-256 哈希，用作 Redis Key（避免明文存储）
     */
    static String hashApiKey(String apiKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 遮蔽 API Key 中间部分，用于日志输出 */
    private static String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
