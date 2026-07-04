package com.yuan.daydayup.reading.runtime.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 解析运行时 HTTP 出站配置（{@code reading.http.*}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "reading.http")
public class ReadingHttpProperties {

    /** 连接超时（毫秒） */
    private long connectTimeoutMs = 5000;

    /** 读超时（毫秒） */
    private long readTimeoutMs = 10000;

    /** 最大重定向跳数（每跳重新过 SSRF 校验） */
    private int maxRedirects = 5;

    /**
     * 是否放行私网/回环目标。生产必须 false；仅本机联调 / 单测（MockWebServer 跑在 127.0.0.1）置 true。
     */
    private boolean allowPrivate = false;

    /** 默认 UA（书源自带 User-Agent 时被覆盖） */
    private String userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";

    /** 书源未声明 concurrentRate 时的默认最小请求间隔（毫秒） */
    private long defaultMinIntervalMs = 200;

    /** 单书源并发上限 */
    private int maxConcurrentPerSource = 2;

    /** 限速等待上限（毫秒），超过视为源站过载 */
    private long rateWaitTimeoutMs = 10000;
}
