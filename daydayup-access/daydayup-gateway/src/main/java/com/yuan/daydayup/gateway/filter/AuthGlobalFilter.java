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
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
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

        // 1.5 剥离客户端可能伪造的内部信任头，防止下游身份冒充
        final ServerWebExchange cleanedExchange = exchange.mutate().request(
                request.mutate()
                        .headers(h -> {
                            h.remove(CommonConstants.HEADER_USER_ID);
                            h.remove(CommonConstants.HEADER_USER_NAME);
                            h.remove(SecurityConstants.CLAIM_AUTHORITIES);
                        }).build()
        ).build();
        ServerHttpRequest cleanedRequest = cleanedExchange.getRequest();

        // 2. API Key 认证（服务间调用 / 定时任务 / 第三方集成）
        //    优先于 JWT，因为 API Key 是长期有效的，不需要 Bearer 前缀
        String apiKey = cleanedRequest.getHeaders().getFirst("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return authenticateByApiKey(cleanedExchange, chain, apiKey);
        }

        // 3. 取 Authorization 头并校验 Bearer 前缀，缺失则按「未登录」处理
        String authorization = cleanedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return reject(cleanedExchange, ErrorCode.UNAUTHORIZED);
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
                                        return reject(cleanedExchange, ErrorCode.TOKEN_BLACKLISTED);
                                    }
                                    // 5. 权限版本校验：若 token 签发后用户权限被变更，拒绝该 token（实现权限变更即时生效）
                                    return checkPermissionVersion(cleanedExchange, jwt)
                                            .flatMap(isValid -> {
                                                if (Boolean.FALSE.equals(isValid)) {
                                                    return reject(cleanedExchange, ErrorCode.TOKEN_INVALID);
                                                }
                                                // 6. 全部校验通过：把用户上下文写入下游请求头后放行
                                                return chain.filter(mutateExchange(cleanedExchange, jwt));
                                            });
                                });
                    }
                    // 无 jti 的 token 无法做黑名单校验，仅写入上下文后放行
                    return chain.filter(mutateExchange(cleanedExchange, jwt));
                })
                // 6. 验签异常（签名错误 / 已过期 / 格式非法）统一返回「令牌无效」
                .onErrorResume(ex -> {
                    log.warn("JWT 校验失败：{}", ex.getMessage());
                    return reject(cleanedExchange, ErrorCode.TOKEN_INVALID);
                });
    }

    /**
     * 权限版本校验：对比 JWT 的签发时间（iat）与 Redis 中该用户的权限降级时间。
     *
     * <p>当 admin 服务降级用户权限（禁用、角色移除等）时，会在 Redis 标记降级时间。
     * 如果 token 的 iat 早于降级时间，说明该 token 携带的是旧（更高）权限，予以拒绝。
     * 权限新增时不标记，旧 token 仍有效（旧权限是新权限的子集，无越权风险）。</p>
     *
     * @return true=权限版本有效，false=需要重新登录
     */
    private Mono<Boolean> checkPermissionVersion(ServerWebExchange exchange, Jwt jwt) {
        Long userId = readLong(jwt, SecurityConstants.CLAIM_USER_ID);
        if (userId == null) {
            return Mono.just(true);
        }
        java.time.Instant issuedAt = jwt.getIssuedAt();
        if (issuedAt == null) {
            return Mono.just(true);
        }
        String key = "daydayup:auth:perm-changed:" + userId;
        return redisTemplate.opsForValue().get(key)
                .map(changedAtStr -> {
                    try {
                        long changedAtEpoch = Long.parseLong(changedAtStr);
                        // token 签发时间 < 权限变更时间 → 旧 token，拒绝
                        return issuedAt.getEpochSecond() >= changedAtEpoch;
                    }
                    catch (NumberFormatException e) {
                        return true; // 解析失败放行，不阻断业务
                    }
                })
                .defaultIfEmpty(true); // Redis 无记录表示未变更，放行
    }

    /**
     * API Key 认证：从 Redis 查询 API Key 对应的用户上下文，写入请求头后放行。
     *
     * <p>API Key 在 Redis 中的格式：daydayup:apikey:{apiKey} → JSON {userId, username, authorities}
     * 适用于服务间调用、定时任务、第三方集成等无需用户交互的场景。</p>
     */
    private Mono<Void> authenticateByApiKey(ServerWebExchange exchange, GatewayFilterChain chain, String apiKey) {
        String key = "daydayup:apikey:" + hashApiKey(apiKey);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> info = objectMapper.readValue(json, java.util.Map.class);
                        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                        Object userId = info.get("userId");
                        if (userId != null) {
                            builder.header(CommonConstants.HEADER_USER_ID, String.valueOf(userId));
                        }
                        Object username = info.get("username");
                        if (username != null) {
                            builder.header(CommonConstants.HEADER_USER_NAME,
                                    URLEncoder.encode(String.valueOf(username), StandardCharsets.UTF_8));
                        }
                        Object authorities = info.get("authorities");
                        if (authorities instanceof java.util.List<?> list && !list.isEmpty()) {
                            builder.header(SecurityConstants.CLAIM_AUTHORITIES,
                                    list.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(",")));
                        }
                        return chain.filter(exchange.mutate().request(builder.build()).build());
                    }
                    catch (Exception e) {
                        log.warn("API Key 解析失败: {}", e.getMessage());
                        return reject(exchange, ErrorCode.UNAUTHORIZED);
                    }
                })
                .switchIfEmpty(reject(exchange, ErrorCode.UNAUTHORIZED));
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
        String username = jwt.getSubject();
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
            }
            catch (NumberFormatException ignored) {
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

    /**
     * 对 API Key 做 SHA-256 哈希（与 ApiKeyService 保持一致）
     */
    private static String hashApiKey(String apiKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(R.fail(errorCode));
            DataBuffer buffer = response.bufferFactory().wrap(body);
            return response.writeWith(Mono.just(buffer));
        }
        catch (JsonProcessingException ex) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
