package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * per-source 出站限速器（PRD §5 / NFR）：单书源并发上限 + 滑动窗口频次。
 *
 * <p>聚合调用、后台批量任务与（后续的）JS 内 {@code java.ajax} 都必须经
 * {@link HttpFetcher} 统一出口过本限速器，不得绕行。</p>
 *
 * <p>Legado {@code concurrentRate} 语义：{@code "N/M"} = M 毫秒窗口内最多 N 次；
 * 纯数字 {@code "N"} = 每次请求间隔至少 N 毫秒。缺省用
 * {@link ReadingHttpProperties#getDefaultMinIntervalMs()}。</p>
 */
@Component
public class SourceRateLimiter {

    /** 解析后的频次规格 */
    public record Rate(int count, long windowMillis) {
    }

    private final ReadingHttpProperties props;
    private final Map<String, Limiter> limiters = new ConcurrentHashMap<>();

    public SourceRateLimiter(ReadingHttpProperties props) {
        this.props = props;
    }

    /**
     * 取得一次出站配额：先占并发位，再等滑动窗口。必须与 {@link #release} 成对（finally）。
     */
    public void acquire(String sourceKey, String concurrentRate) {
        Limiter limiter = limiters.computeIfAbsent(sourceKey,
                k -> new Limiter(props.getMaxConcurrentPerSource()));
        Rate rate = parse(concurrentRate, props.getDefaultMinIntervalMs());
        try {
            if (!limiter.permits.tryAcquire(props.getRateWaitTimeoutMs(), TimeUnit.MILLISECONDS)) {
                throw new BizException(ErrorCode.READING_UPSTREAM_TIMEOUT,
                        "书源并发配额等待超时: " + sourceKey);
            }
            try {
                limiter.awaitWindow(rate, props.getRateWaitTimeoutMs());
            } catch (Exception e) {
                limiter.permits.release();
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.READING_UPSTREAM_FETCH_FAILED, "限速等待被中断: " + sourceKey);
        }
    }

    public void release(String sourceKey) {
        Limiter limiter = limiters.get(sourceKey);
        if (limiter != null) {
            limiter.permits.release();
        }
    }

    /** 解析 concurrentRate；null / 空 / 非法 → 每 defaultIntervalMs 一次 */
    public static Rate parse(String concurrentRate, long defaultIntervalMs) {
        if (concurrentRate == null || concurrentRate.isBlank()) {
            return new Rate(1, defaultIntervalMs);
        }
        String s = concurrentRate.strip();
        try {
            int slash = s.indexOf('/');
            if (slash < 0) {
                long interval = Long.parseLong(s);
                return new Rate(1, Math.max(0, interval));
            }
            int count = Integer.parseInt(s.substring(0, slash).strip());
            long window = Long.parseLong(s.substring(slash + 1).strip());
            return (count <= 0 || window < 0) ? new Rate(1, defaultIntervalMs) : new Rate(count, window);
        } catch (NumberFormatException e) {
            return new Rate(1, defaultIntervalMs);
        }
    }

    private static final class Limiter {
        final Semaphore permits;
        final Deque<Long> stamps = new ArrayDeque<>();

        Limiter(int maxConcurrent) {
            this.permits = new Semaphore(Math.max(1, maxConcurrent), true);
        }

        synchronized void awaitWindow(Rate rate, long waitTimeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + waitTimeoutMs;
            while (true) {
                long now = System.currentTimeMillis();
                while (!stamps.isEmpty() && now - stamps.peekFirst() >= rate.windowMillis()) {
                    stamps.pollFirst();
                }
                if (stamps.size() < rate.count()) {
                    stamps.addLast(now);
                    return;
                }
                long wakeAt = stamps.peekFirst() + rate.windowMillis();
                if (wakeAt > deadline) {
                    throw new BizException(ErrorCode.READING_UPSTREAM_TIMEOUT, "书源频次窗口等待超时");
                }
                wait(Math.max(1, wakeAt - now));
            }
        }
    }
}
