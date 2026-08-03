package com.yuan.daydayup.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsSecurityWebFilterTest {

    @Test
    void shouldUseConfiguredCorsAllowedOriginPatterns() {
        GatewaySecurityProperties gatewaySecurityProperties = new GatewaySecurityProperties();
        gatewaySecurityProperties.getCors().setAllowedOriginPatterns(List.of("https://admin.example.com"));
        CorsSecurityWebFilter corsSecurityWebFilter = new CorsSecurityWebFilter(gatewaySecurityProperties);
        CorsConfiguration corsConfiguration = corsSecurityWebFilter.createCorsConfiguration();

        assertThat(corsConfiguration.getAllowedOriginPatterns()).containsExactly("https://admin.example.com");
        assertThat(corsConfiguration.getAllowedMethods()).contains(HttpMethod.GET.name(), HttpMethod.OPTIONS.name());
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
    }

    @Test
    void shouldKeepLocalhostAsDefaultCorsAllowedOriginPattern() {
        CorsSecurityWebFilter corsSecurityWebFilter = new CorsSecurityWebFilter(new GatewaySecurityProperties());
        CorsConfiguration corsConfiguration = corsSecurityWebFilter.createCorsConfiguration();

        assertThat(corsConfiguration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:*", "http://127.0.0.1:*");
    }
}
