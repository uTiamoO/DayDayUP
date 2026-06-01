package com.yuan.daydayup.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.constant.CommonConstants;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.gateway.config.GatewaySecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 网关认证全局过滤器
 *
 * <p>职责：</p>
 * <ol>
 *   <li>白名单路径直接放行</li>
 *   <li>提取 Authorization 头 → 校验 JWT → 解析 claim</li>
 *   <li>把用户上下文写入下游请求头（X-User-Id、X-User-Name、authorities）</li>
 *   <li>失败时返回统一 {@link R} JSON</li>
 * </ol>
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ReactiveJwtDecoder jwtDecoder;
    private final GatewaySecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthGlobalFilter(ReactiveJwtDecoder jwtDecoder,
                            GatewaySecurityProperties properties,
                            ObjectMapper objectMapper,
                            ReactiveStringRedisTemplate redisTemplate) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单路径（登录 / 刷新 / 公钥 / 文档等）直接放行，不校验 JWT
        if (isPermitPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 取 Authorization 头并校验 Bearer 前缀，缺失则按「未登录」处理
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return reject(exchange, ErrorCode.UNAUTHORIZED);
        }

        String token = authorization.substring(SecurityConstants.BEARER_PREFIX.length()).trim();

        // 3. 异步验签：decode 内部完成签名与时间（exp/nbf）校验，任何失败都会进入 onErrorResume
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // 4. 验签通过后查黑名单：命中说明该 token 已被登出 / 吊销，拒绝放行
                    String jti = jwt.getId();
                    if (jti != null) {
                        String blacklistKey = SecurityConstants.TOKEN_BLACKLIST_KEY_PREFIX + jti;
                        return redisTemplate.hasKey(blacklistKey)
                                .flatMap(isBlacklisted -> {
                                    if (Boolean.TRUE.equals(isBlacklisted)) {
                                        return reject(exchange, ErrorCode.TOKEN_BLACKLISTED);
                                    }
                                    // 5. 全部校验通过：把用户上下文写入下游请求头后放行
                                    return chain.filter(mutateExchange(exchange, jwt));
                                });
                    }
                    // 无 jti 的 token 无法做黑名单校验，仅写入上下文后放行
                    return chain.filter(mutateExchange(exchange, jwt));
                })
                // 6. 验签异常（签名错误 / 已过期 / 格式非法）统一返回「令牌无效」
                .onErrorResume(ex -> {
                    log.warn("JWT 校验失败：{}", ex.getMessage());
                    return reject(exchange, ErrorCode.TOKEN_INVALID);
                });
    }

    private boolean isPermitPath(String path) {
        for (String pattern : properties.getPermitPaths()) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, Jwt jwt) {
        Long userId = readLong(jwt, SecurityConstants.CLAIM_USER_ID);
        String username = jwt.getClaimAsString(SecurityConstants.CLAIM_USERNAME);
        Collection<String> authorities = readAuthorities(jwt);

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        if (userId != null) {
            builder.header(CommonConstants.HEADER_USER_ID, String.valueOf(userId));
        }
        if (username != null) {
            builder.header(CommonConstants.HEADER_USER_NAME, URLEncoder.encode(username, StandardCharsets.UTF_8));
        }
        if (authorities != null && !authorities.isEmpty()) {
            builder.header(SecurityConstants.CLAIM_AUTHORITIES, String.join(",", authorities));
        }
        return exchange.mutate().request(builder.build()).build();
    }

    private static Long readLong(Jwt jwt, String claim) {
        Object value = jwt.getClaim(claim);
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> readAuthorities(Jwt jwt) {
        Object value = jwt.getClaim(SecurityConstants.CLAIM_AUTHORITIES);
        if (value instanceof Collection<?> c) {
            return c.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        if (value instanceof String s && !s.isBlank()) {
            return List.of(s.split(","));
        }
        return List.of();
    }

    private Mono<Void> reject(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(R.fail(errorCode));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException ex) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
