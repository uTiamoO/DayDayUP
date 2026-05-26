package com.yuan.daydayup.common.mq.config;

import com.yuan.daydayup.common.mq.publisher.DomainEventPublisher;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * common-mq 自动装配
 *
 * <p>当容器内存在 {@link RocketMQTemplate} 时才注册领域事件发布器，
 * 避免没有 RocketMQ 中间件的环境（如 gateway / monitor）启动失败。</p>
 */
@AutoConfiguration(after = RocketMQAutoConfiguration.class)
@ConditionalOnClass(RocketMQTemplate.class)
public class MqAutoConfiguration {

    @Bean
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnMissingBean
    public DomainEventPublisher domainEventPublisher(RocketMQTemplate rocketMQTemplate) {
        return new DomainEventPublisher(rocketMQTemplate);
    }
}
