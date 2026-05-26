package com.yuan.daydayup.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * 网关 JWT 解码器配置
 *
 * <p>通过认证中心的 JWKs 端点动态拉取公钥（支持密钥轮换）。</p>
 *
 * <p>不引入 spring-boot-starter-oauth2-resource-server，
 * 认证由 {@link com.yuan.daydayup.gateway.filter.AuthGlobalFilter} 自行处理。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewaySecurityConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @org.springframework.beans.factory.annotation.Value("${daydayup.gateway.jwk-set-uri}") String jwkSetUri) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
