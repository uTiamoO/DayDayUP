package com.yuan.daydayup.gateway.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 网关全局访问日志过滤器
 *
 * <p>记录每个请求的：HTTP 方法、路径、状态码、耗时（ms）、客户端 IP、traceId。
 * 用 {@code doFinally} 确保无论请求成功、失败还是被取消都会落日志
 * （此前用 {@code then} 时，链路抛异常的请求会漏记）。</p>
 *
 * <p>优先级设为 {@link Ordered#LOWEST_PRECEDENCE}，确保在所有业务过滤器之后执行，
 * 记录的是最终的响应状态码。</p>
 */
@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private final ObjectProvider<Tracer> tracerProvider;

    public AccessLogFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startNanos = System.nanoTime();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = resolveClientIp(request);

        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;

            log.info("[GW] {} {} {} {}ms ip={} trace={}",
                    method, path, statusCode, costMs, clientIp, resolveTraceId());
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 解析客户端 IP。
     *
     * <p>优先取 X-Forwarded-For 首段（经可信反向代理时即真实客户端 IP）。
     * 注意：若网关直接暴露公网，XFF 可被客户端伪造，此值仅用于日志、不可用于鉴权决策。</p>
     */
    private String resolveClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * 取当前链路 traceId（由 micrometer-tracing 提供）。
     * reactive 上下文中若未传播到当前线程则取不到，降级为 "-"。
     */
    private String resolveTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return "-";
        }
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : "-";
    }
}
