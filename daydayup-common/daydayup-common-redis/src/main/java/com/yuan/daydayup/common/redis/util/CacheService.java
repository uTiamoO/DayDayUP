package com.yuan.daydayup.common.redis.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存工具服务
 *
 * <p>封装 Redis 常见操作，业务方可直接注入使用，避免重复编写序列化 / 反序列化逻辑。
 * 缓存 key 前缀遵循 {@link CacheKeys} 规范：daydayup:&lt;模块&gt;:&lt;业务&gt;:&lt;标识&gt;</p>
 *
 * <p>典型用法：</p>
 * <pre>{@code
 *   // 读缓存
 *   UserDetailVO user = cacheService.getJson(CacheKeys.of("admin", "user", userId), UserDetailVO.class);
 *   if (user == null) {
 *       user = queryFromDb(userId);
 *       cacheService.setJson(CacheKeys.of("admin", "user", userId), user, 30, TimeUnit.MINUTES);
 *   }
 *
 *   // 写操作后清缓存
 *   cacheService.delete(CacheKeys.of("admin", "user", userId));
 * }</pre>
 */
@Slf4j
@Component
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 从缓存获取 JSON 值并反序列化
     *
     * @param key   Redis key
     * @param clazz 目标类型
     * @return 缓存值，不存在或反序列化失败时返回 null
     */
    public <T> T getJson(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("缓存读取失败: key={}, error={}", key, e.getMessage());
            // 缓存读取失败不影响业务，降级为 null（由调用方走 DB）
            return null;
        }
    }

    /**
     * 从缓存获取 JSON 值（支持泛型类型引用，如 List&lt;T&gt;）
     */
    public <T> T getJson(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("缓存读取失败: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写入缓存（JSON 序列化）
     *
     * @param key      Redis key
     * @param value    值对象
     * @param ttl      过期时间
     * @param timeUnit 时间单位
     */
    public void setJson(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl, timeUnit);
        } catch (Exception e) {
            log.warn("缓存写入失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 删除单个缓存 key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 按前缀批量删除缓存（适用于清除某类业务的全部缓存）
     *
     * <p>使用 SCAN 迭代器分批获取 key，避免 KEYS 命令阻塞 Redis。</p>
     */
    public void deleteByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(200)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> batch = new ArrayList<>();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 200) {
                        redisTemplate.delete(batch);
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
        }
    }
}
