package com.yuan.daydayup.common.feign.config;

import com.yuan.daydayup.common.feign.decoder.BizFeignErrorDecoder;
import com.yuan.daydayup.common.feign.interceptor.FeignRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 自动装配
 */
@Configuration(proxyBeanMethods = false)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FeignRequestInterceptor feignRequestInterceptor() {
        return new FeignRequestInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public BizFeignErrorDecoder bizFeignErrorDecoder() {
        return new BizFeignErrorDecoder();
    }
}
