package com.yuan.daydayup.common.idempotent.config;

import com.yuan.daydayup.common.idempotent.aspect.IdempotentAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 幂等自动装配
 */
@Configuration(proxyBeanMethods = false)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate) {
        return new IdempotentAspect(redisTemplate);
    }
}
