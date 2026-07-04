package com.yuan.daydayup.reading.runtime.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SourceRateLimiter} 单测：concurrentRate 解析 + 窗口间隔实际生效。
 */
class SourceRateLimiterTest {

    @Test
    void parseFormats() {
        assertEquals(new SourceRateLimiter.Rate(5, 1000), SourceRateLimiter.parse("5/1000", 200));
        assertEquals(new SourceRateLimiter.Rate(1, 500), SourceRateLimiter.parse("500", 200));
        assertEquals(new SourceRateLimiter.Rate(1, 200), SourceRateLimiter.parse(null, 200));
        assertEquals(new SourceRateLimiter.Rate(1, 200), SourceRateLimiter.parse("  ", 200));
        assertEquals(new SourceRateLimiter.Rate(1, 200), SourceRateLimiter.parse("abc", 200));
        assertEquals(new SourceRateLimiter.Rate(1, 200), SourceRateLimiter.parse("0/1000", 200));
    }

    @Test
    void enforcesMinInterval() {
        ReadingHttpProperties props = new ReadingHttpProperties();
        SourceRateLimiter limiter = new SourceRateLimiter(props);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            limiter.acquire("s1", "1/80");
            limiter.release("s1");
        }
        long elapsed = System.currentTimeMillis() - start;
        // 3 次请求、80ms 窗口每次 1 个 → 至少跨 2 个完整窗口
        assertTrue(elapsed >= 160, "限速未生效，elapsed=" + elapsed);
    }

    @Test
    void windowAllowsBurstWithinCount() {
        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setMaxConcurrentPerSource(5);
        SourceRateLimiter limiter = new SourceRateLimiter(props);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            limiter.acquire("s2", "3/5000");
            limiter.release("s2");
        }
        long elapsed = System.currentTimeMillis() - start;
        // 窗口内额度 3，三次应立即通过
        assertTrue(elapsed < 1000, "窗口内突发被误限，elapsed=" + elapsed);
    }
}
