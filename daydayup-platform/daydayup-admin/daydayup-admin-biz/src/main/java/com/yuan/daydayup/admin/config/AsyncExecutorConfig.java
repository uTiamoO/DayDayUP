package com.yuan.daydayup.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 *
 * <p>为操作日志落库等异步任务提供独立线程池，避免使用 Spring 默认的
 * SimpleAsyncTaskExecutor（每次创建新线程，高并发下会 OOM）。</p>
 */
@Slf4j
@Configuration
public class AsyncExecutorConfig {

    /**
     * 操作日志异步线程池
     *
     * <p>核心 5 线程，最大 20 线程，队列 1000。
     * 队列满时丢弃任务并打 WARN 日志——操作日志属于可降级数据，
     * 丢弃优于用 CallerRunsPolicy 反过来阻塞业务请求线程；
     * 若日志不可丢失，应改为落 MQ 异步消费。</p>
     */
    @Bean("logExecutor")
    public Executor logExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("log-async-");
        executor.setRejectedExecutionHandler((r, exec) ->
                log.warn("操作日志线程池队列已满（active={}, poolSize={}），丢弃本次日志任务",
                        exec.getActiveCount(), exec.getPoolSize()));
        executor.initialize();
        return executor;
    }
}
