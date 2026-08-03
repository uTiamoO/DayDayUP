package com.yuan.daydayup.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.constant.CommonConstants;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.gateway.config.GatewaySecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthGlobalFilterTest {

    private ReactiveJwtDecoder jwtDecoder;
    private ReactiveStringRedisTemplate redisTemplate;
    private ReactiveValueOperations<String, String> valueOperations;
    private AuthGlobalFilter authGlobalFilter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtDecoder = mock(ReactiveJwtDecoder.class);
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        GatewaySecurityProperties gatewaySecurityProperties = new GatewaySecurityProperties();
        authGlobalFilter = new AuthGlobalFilter(
                jwtDecoder,
                gatewaySecurityProperties,
                new ObjectMapper(),
                redisTemplate
        );
    }

    @Test
    void shouldNotSetUnauthorizedResponseBeforeExistingApiKeyAuthenticationFinishes() throws Exception {
        String apiKey = "valid-api-key";
        String redisKey = "daydayup:apikey:" + hashApiKey(apiKey);
        String apiKeyPayload = new ObjectMapper().writeValueAsString(Map.of(
                "userId", 1L,
                "username", "admin",
                "authorities", List.of("admin:user:list")
        ));
        when(valueOperations.get(redisKey)).thenReturn(Mono.just(apiKeyPayload));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin/users")
                        .header("X-API-Key", apiKey)
        );
        GatewayFilterChain chain = downstreamExchange -> {
            HttpStatusCode responseStatusBeforeDownstream = downstreamExchange.getResponse().getStatusCode();
            if (HttpStatus.UNAUTHORIZED.equals(responseStatusBeforeDownstream)) {
                return Mono.error(new AssertionError("API Key 命中 Redis 前不应提前写入 401 响应"));
            }
            assertThat(downstreamExchange.getRequest().getHeaders().getFirst(CommonConstants.HEADER_USER_ID))
                    .isEqualTo("1");
            assertThat(downstreamExchange.getRequest().getHeaders().getFirst(SecurityConstants.CLAIM_AUTHORITIES))
                    .isEqualTo("admin:user:list");
            return Mono.empty();
        };

        StepVerifier.create(authGlobalFilter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldReturnTokenInvalidOnlyWhenJwtDecodeFails() {
        String token = "bad-token";
        when(jwtDecoder.decode(token)).thenReturn(Mono.error(new JwtException("bad jwt")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, SecurityConstants.BEARER_PREFIX + token)
        );

        StepVerifier.create(authGlobalFilter.filter(exchange, downstreamExchange -> Mono.empty()))
                .verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldPropagateDownstreamErrorsAfterJwtAuthenticationSucceeds() {
        String token = "valid-token";
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(token)).thenReturn(Mono.just(jwt));
        RuntimeException downstreamError = new RuntimeException("downstream unavailable");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, SecurityConstants.BEARER_PREFIX + token)
        );

        StepVerifier.create(authGlobalFilter.filter(exchange, downstreamExchange -> Mono.error(downstreamError)))
                .expectErrorSatisfies(error -> assertThat(error).isSameAs(downstreamError))
                .verify();
    }

    private static String hashApiKey(String apiKey) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
