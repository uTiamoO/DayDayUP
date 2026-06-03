package com.yuan.daydayup.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;

/**
 * 网关全局访问日志过滤器
 *
 * <p>记录每个请求的：HTTP 方法、路径、状态码、耗时（ms）、客户端 IP、traceId。
 * 用于快速排查问题，配合 Zipkin 实现全链路追踪。</p>
 *
 * <p>优先级设为 {@link Ordered#LOWEST_PRECEDENCE}，确保在所有业务过滤器之后执行，
 * 记录的是最终的响应状态码。</p>
 */
@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant start = Instant.now();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = resolveClientIp(request);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
            long costMs = Instant.now().toEpochMilli() - start.toEpochMilli();
            String traceId = request.getHeaders().getFirst("X-Trace-Id");

            log.info("[GW] {} {} {} {}ms ip={} trace={}",
                    method, path, statusCode, costMs, clientIp,
                    traceId != null ? traceId : "-");
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
