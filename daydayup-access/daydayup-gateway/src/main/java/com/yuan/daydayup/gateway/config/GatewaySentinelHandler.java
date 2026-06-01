package com.yuan.daydayup.gateway.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.result.R;
import jakarta.annotation.PostConstruct;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sentinel 限流自定义响应
 *
 * <p>被限流 / 熔断 / 参数限流时，统一返回 {@link R} 结构，避免默认的纯文本提示。</p>
 */
@Component
public class GatewaySentinelHandler implements BlockRequestHandler {

    @PostConstruct
    public void init() {
        WebFluxCallbackManager.setBlockHandler(this);
    }

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        String message = switch (ex) {
            case FlowException flowException -> "请求过于频繁，请稍后再试";
            case DegradeException degradeException -> "服务降级中";
            case ParamFlowException paramFlowException -> "热点参数限流";
            default -> "请求被网关拦截：" + ex.getClass().getSimpleName();
        };

        R<Void> body = R.fail(ErrorCode.SERVICE_UNAVAILABLE.getCode(), message);
        return ServerResponse.status(ErrorCode.SERVICE_UNAVAILABLE.getCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }
}
