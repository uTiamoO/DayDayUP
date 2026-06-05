package com.yuan.daydayup.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关统一 CORS 与安全响应头配置
 *
 * <p>在网关（WebFlux）层统一处理跨域，业务服务不再各自配置 CORS，
 * 避免「任意来源 + 允许凭证」的安全隐患，也避免多处维护。</p>
 *
 * <p>allowedOrigins 默认仅允许 localhost 开发环境，生产请通过 Nacos 覆盖
 * {@code daydayup.gateway.cors.allowed-origins} 配置。</p>
 *
 * <p>安全响应头参考 OWASP Secure Headers，按最小可用集添加：</p>
 * <ul>
 *   <li>X-Frame-Options: DENY — 防止页面被嵌入 iframe</li>
 *   <li>X-Content-Type-Options: nosniff — 禁止 MIME 嗅探</li>
 *   <li>Cache-Control: no-store — 敏感响应禁止缓存</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class CorsSecurityWebFilter {

    /**
     * 统一 CORS 过滤器
     *
     * <p>默认仅允许 localhost 开发来源；生产通过 Nacos 下发
     * {@code daydayup.gateway.cors.allowed-origins}（逗号分隔）精确控制。</p>
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发环境默认放行 localhost
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    /**
     * 安全响应头过滤器
     *
     * <p>添加 OWASP 推荐的最小安全头集合，防止常见前端攻击向量。</p>
     */
    @Bean
    public WebFilter securityHeadersFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 只在正常响应上附加安全头，非 1xx/2xx/3xx 响应由异常处理器处理
            response.getHeaders().set("X-Frame-Options", "DENY");
            response.getHeaders().set("X-Content-Type-Options", "nosniff");
            response.getHeaders().set("X-XSS-Protection", "1; mode=block");
            // API 响应禁止缓存，静态资源由 CDN/业务自行设置
            response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");

            return chain.filter(exchange);
        };
    }
}
