package com.yuan.daydayup.common.security.config;

import com.yuan.daydayup.common.security.filter.HeaderAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 资源服务器安全配置（业务服务通用）
 *
 * <p>策略：</p>
 * <ul>
 *   <li>无状态会话（不创建 HttpSession）</li>
 *   <li>禁用 CSRF（无状态接口不需要）</li>
 *   <li>所有请求默认放行 —— 由 {@code @PreAuthorize} 做细粒度方法级鉴权</li>
 *   <li>{@link HeaderAuthenticationFilter} 还原网关透传的用户上下文</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig {

    @Bean
    @ConditionalOnMissingBean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   HeaderAuthenticationFilter headerAuthFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
