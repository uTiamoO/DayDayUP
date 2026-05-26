package com.yuan.daydayup.common.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.log.aspect.OperLogAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 操作日志自动装配
 */
@EnableAsync
@Configuration(proxyBeanMethods = false)
public class OperLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperLogAspect operLogAspect(ApplicationEventPublisher eventPublisher,
                                       ObjectMapper objectMapper) {
        return new OperLogAspect(eventPublisher, objectMapper);
    }
}
