package com.yuan.daydayup.common.xxljob.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器自动装配
 *
 * <p>启用条件：{@code daydayup.xxljob.enabled=true}（默认 true）。
 * 业务模块只需在 application.yml 配置 admin-addresses + app-name，
 * 然后用 {@code @XxlJob("handler-name")} 注解定义任务。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(prefix = "daydayup.xxljob", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        log.info("初始化 XXL-JOB 执行器：app={}, admin={}",
                properties.getAppName(), properties.getAdminAddresses());

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setAppname(properties.getAppName());
        executor.setIp(properties.getIp());
        executor.setPort(properties.getPort());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());
        return executor;
    }
}

/**
 * XXL-JOB 执行器配置
 */
@Data
@ConfigurationProperties(prefix = "daydayup.xxljob")
class XxlJobProperties {

    /** 是否启用 */
    private boolean enabled = true;

    /** Admin 地址，逗号分隔 */
    private String adminAddresses = "http://127.0.0.1:9400/xxl-job-admin";

    /** Access token（与 Admin 配置一致） */
    private String accessToken = "";

    /** 执行器 AppName */
    private String appName = "daydayup-executor";

    /** 执行器 IP，默认自动获取 */
    private String ip = "";

    /** 执行器端口，0=自动 */
    private int port = 0;

    /** 执行器日志路径 */
    private String logPath = "logs/xxl-job";

    /** 日志保留天数，-1=永久 */
    private int logRetentionDays = 30;
}
