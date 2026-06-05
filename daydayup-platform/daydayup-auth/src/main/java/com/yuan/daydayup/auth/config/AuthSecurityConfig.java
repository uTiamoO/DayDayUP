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
 * 本 Chain 处理其余端点：/login（登录页公开）、/api/**（需 Bearer 认证）、
 * /swagger-ui/**（公开）。</p>
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
            // CSRF：对浏览器表单登录（/login）启用防护；对无状态 REST 端点（/api/**，Bearer JWT 认证）忽略
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(authorize -> authorize
                // 公开端点：登录页、接口文档、健康检查、错误转发
                .requestMatchers("/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**", "/error").permitAll()
                // 其余一律要求已认证（fail-secure）：漏标 @PreAuthorize 的端点至少需登录态，不再默认裸奔
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin
                .loginPage("/login")
                .permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
