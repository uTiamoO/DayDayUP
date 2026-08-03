package com.yuan.daydayup.reading.observability;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ReadingMetrics} 单测：验证各 record 方法向 registry 写入预期 meter 与 tag，
 * 以及无界/异常入参下的静默降级（绝不外抛）。
 */
class ReadingMetricsTest {

    private SimpleMeterRegistry registry;
    private ReadingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ReadingMetrics(registry);
    }

    @Test
    void recordFetchWritesTimerWithSourceAndResultTags() {
        metrics.recordFetch("source-1", "success", 1_000_000L);

        Timer timer = registry.find("reading.fetch.duration")
                .tag("source", "source-1").tag("result", "success").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordCompileIncrementsGradeCounter() {
        metrics.recordCompile("full");
        metrics.recordCompile("full");
        metrics.recordCompile("rejected");

        assertEquals(2, registry.find("reading.compile.total").tag("grade", "full").counter().count(), 0.0);
        assertEquals(1, registry.find("reading.compile.total").tag("grade", "rejected").counter().count(), 0.0);
    }

    @Test
    void recordSanitizeCountsResultAndRecordsQualityWhenPresent() {
        metrics.recordSanitize("accepted", 88);

        assertEquals(1, registry.find("reading.sanitize.total").tag("result", "accepted").counter().count(), 0.0);
        DistributionSummary quality = registry.find("reading.sanitize.quality").summary();
        assertNotNull(quality);
        assertEquals(1, quality.count());
        assertEquals(88.0, quality.totalAmount(), 0.0);
    }

    @Test
    void recordSanitizeSkipsQualityWhenScoreNull() {
        metrics.recordSanitize("failed", null);

        assertEquals(1, registry.find("reading.sanitize.total").tag("result", "failed").counter().count(), 0.0);
        assertNull(registry.find("reading.sanitize.quality").summary());
    }

    @Test
    void recordTaskIncrementsTypeResultCounter() {
        metrics.recordTask("content_fetch", "succeeded");

        assertEquals(1, registry.find("reading.task.total")
                .tag("type", "content_fetch").tag("result", "succeeded").counter().count(), 0.0);
    }

    @Test
    void nullTagValuesFallBackToUnknownAndDoNotThrow() {
        assertDoesNotThrow(() -> {
            metrics.recordFetch(null, null, 1L);
            metrics.recordCompile(null);
            metrics.recordSanitize(null, null);
            metrics.recordTask(null, null);
        });
        assertNotNull(registry.find("reading.fetch.duration").tag("source", "unknown").timer());
        assertNotNull(registry.find("reading.compile.total").tag("grade", "unknown").counter());
    }

    @Test
    void recordSilentlyDegradesWhenRegistryThrows() {
        // 传入一个 record 时抛异常的 registry，验证埋点异常被吞（不影响主流程）
        MeterRegistry faulty = new SimpleMeterRegistry() {
            @Override
            public io.micrometer.core.instrument.Counter counter(String name, String... tags) {
                throw new IllegalStateException("registry boom");
            }
        };
        ReadingMetrics faultyMetrics = new ReadingMetrics(faulty);
        assertDoesNotThrow(() -> faultyMetrics.recordCompile("full"));
        assertDoesNotThrow(() -> faultyMetrics.recordTask("toc_sync", "failed"));
    }
}
