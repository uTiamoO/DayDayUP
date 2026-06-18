package com.yuan.daydayup.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 通用 SecurityFilterChain（Order=2）。
 *
 * <p>SAS 协议端点由 AuthorizationServerConfig 的 Chain（Order=1）接管。
 * 本 Chain 处理其余端点：自有登录 API、OIDC 会话登录 API、管理 API、接口文档等。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class AuthSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // auth 服务只提供后端 API；自有登录与 OIDC 会话登录都走 JSON 请求。
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(authorize -> authorize
                // 公开端点：自有登录 API、OIDC 会话登录 API、接口文档、健康检查、错误转发
                .requestMatchers("/api/login", "/api/refresh").permitAll()
                .requestMatchers(
                    "/api/session/login-required",
                    "/api/session/login",
                    "/api/session/logout"
                ).permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**", "/error").permitAll()
                // 其余一律要求已认证（fail-secure）：漏标 @PreAuthorize 的端点至少需登录态，不再默认裸奔
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
