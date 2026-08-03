package com.yuan.daydayup.reading.observability;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 阅读中台业务观测性埋点薄封装（切片7）。
 *
 * <p>集中持有 {@link MeterRegistry}，向各业务埋点位暴露语义化记录方法，统一指标命名与
 * 「静默降级」策略：埋点是主流程的只读旁路，任何埋点异常都只记 debug 日志、绝不外抛，
 * 从而保证观测性缺失不会影响抓取/编译/净化/任务的可用性。</p>
 *
 * <p>指标命名遵循 Micrometer 约定（点分小写、计时基准单位为秒、维度用 tag 表达）。
 * tag value 一律为有界枚举，禁止把 URL / host / chapterId / 异常 message 等无界值作为 tag。</p>
 */
@Slf4j
@Component
public class ReadingMetrics {

    /** 单次源站抓取耗时与结果（Timer，tag: source, result） */
    static final String FETCH_DURATION = "reading.fetch.duration";
    /** 编译分级计数（Counter，tag: grade） */
    static final String COMPILE_TOTAL = "reading.compile.total";
    /** 净化结果计数（Counter，tag: result） */
    static final String SANITIZE_TOTAL = "reading.sanitize.total";
    /** 净化质量分分布（DistributionSummary，0~100） */
    static final String SANITIZE_QUALITY = "reading.sanitize.quality";
    /** 任务执行结果计数（Counter，tag: type, result） */
    static final String TASK_TOTAL = "reading.task.total";

    private final MeterRegistry registry;

    public ReadingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一次源站抓取。
     *
     * @param sourceKey 书源稳定标识（用作 source tag，集合有界；空则归一为 unknown）
     * @param result    抓取结果枚举：success / timeout / blocked / failed
     * @param durationNanos 抓取耗时（纳秒）
     */
    public void recordFetch(String sourceKey, String result, long durationNanos) {
        try {
            Timer.builder(FETCH_DURATION)
                    .tag("source", tagValue(sourceKey))
                    .tag("result", tagValue(result))
                    .register(registry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            log.debug("[metrics] recordFetch 失败 source={} result={}", sourceKey, result, e);
        }
    }

    /**
     * 记录一次编译分级。
     *
     * @param grade full / degraded / rejected / failed
     */
    public void recordCompile(String grade) {
        try {
            registry.counter(COMPILE_TOTAL, "grade", tagValue(grade)).increment();
        } catch (Exception e) {
            log.debug("[metrics] recordCompile 失败 grade={}", grade, e);
        }
    }

    /**
     * 记录一次净化结果与质量分。
     *
     * @param result       accepted / degraded / rejected / failed
     * @param qualityScore 质量分（0~100），可为 null（如净化异常时无评分）
     */
    public void recordSanitize(String result, Integer qualityScore) {
        try {
            registry.counter(SANITIZE_TOTAL, "result", tagValue(result)).increment();
            if (qualityScore != null) {
                DistributionSummary.builder(SANITIZE_QUALITY)
                        .register(registry)
                        .record(qualityScore);
            }
        } catch (Exception e) {
            log.debug("[metrics] recordSanitize 失败 result={} quality={}", result, qualityScore, e);
        }
    }

    /**
     * 记录一次任务执行结果。
     *
     * @param type   任务类型（有界枚举，6 类）
     * @param result succeeded / partial / retry / failed
     */
    public void recordTask(String type, String result) {
        try {
            registry.counter(TASK_TOTAL, "type", tagValue(type), "result", tagValue(result)).increment();
        } catch (Exception e) {
            log.debug("[metrics] recordTask 失败 type={} result={}", type, result, e);
        }
    }

    /** tag value 兜底：空值归一为 unknown，避免 null tag 触发 registry 异常。 */
    private static String tagValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
