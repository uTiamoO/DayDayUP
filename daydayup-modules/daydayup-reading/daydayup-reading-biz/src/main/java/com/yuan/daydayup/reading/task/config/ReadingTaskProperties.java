package com.yuan.daydayup.reading.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阅读任务配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "reading.task")
public class ReadingTaskProperties {

    /** 自动 worker 默认关闭，避免本地启动后意外抓取。 */
    private boolean workerEnabled = false;

    /** 自动 worker 轮询间隔。 */
    private long workerIntervalMs = 5000L;

    /** running 锁超时时间。 */
    private long lockTimeoutMs = 300000L;

    /** 默认最大重试次数。 */
    private int defaultMaxRetry = 3;

    /** 失败重试退避时间。 */
    private long retryBackoffMs = 30000L;

    /** 自动/手动 drain 默认批大小。 */
    private int batchSize = 10;
}
