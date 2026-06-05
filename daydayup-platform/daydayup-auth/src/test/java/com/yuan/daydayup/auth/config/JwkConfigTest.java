package com.yuan.daydayup.auth.config;

import com.yuan.daydayup.auth.entity.JwkKey;
import com.yuan.daydayup.auth.mapper.JwkKeyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwkConfigTest {

    private JwkKeyMapper jwkKeyMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        jwkKeyMapper = mock(JwkKeyMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldOnlyInsertOnceUnderConcurrency() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        AtomicInteger insertCount = new AtomicInteger(0);
        AtomicBoolean inserted = new AtomicBoolean(false);

        JwkKey createdRecord = new JwkKey();
        createdRecord.setKid("test-kid-1");
        createdRecord.setStatus("ACTIVE");

        // 未插入前返回 null，插入后返回已创建的记录（适配 double-check 模式）
        doAnswer(invocation -> inserted.get() ? createdRecord : null)
                .when(jwkKeyMapper).selectOne(any());

        doAnswer(invocation -> {
            insertCount.incrementAndGet();
            inserted.set(true);
            return 1;
        }).when(jwkKeyMapper).insert(any(JwkKey.class));

        AtomicInteger lockAttempts = new AtomicInteger(0);
        when(valueOperations.setIfAbsent(eq("daydayup:auth:jwk-lock"), anyString(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    int attempts = lockAttempts.incrementAndGet();
                    return attempts == 1; // 仅第一个线程获取成功
                });

        JwkConfig config = new JwkConfig(jwkKeyMapper, redisTemplate, "test-password");

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    config.init();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(insertCount.get()).isEqualTo(1);
    }
}
