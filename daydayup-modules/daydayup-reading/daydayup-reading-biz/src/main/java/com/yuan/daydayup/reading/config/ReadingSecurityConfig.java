package com.yuan.daydayup.reading.config;

import com.yuan.daydayup.common.security.filter.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 阅读中台安全配置。
 *
 * <p>覆盖 common-security 的默认 {@code SecurityFilterChain}（默认「除健康/文档外全部要求认证」），
 * 额外放行内网前缀 {@code /api/v1/internal/**}：这些接口由网关保证不对外路由，
 * 依赖网络隔离而非用户 JWT（对应 spec §6.1、PRD O10）。其余仍要求认证（fail-secure）。</p>
 */
@Configuration(proxyBeanMethods = false)
public class ReadingSecurityConfig {

    @Bean
    public SecurityFilterChain readingSecurityFilterChain(HttpSecurity http,
                                                          HeaderAuthenticationFilter headerAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // 内网治理 / 定向接口：网关不对外暴露
                                "/api/v1/internal/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
