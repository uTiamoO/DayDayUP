package com.yuan.daydayup.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 网关安全配置项
 */
@Data
@ConfigurationProperties(prefix = "daydayup.gateway")
public class GatewaySecurityProperties {

    private static final List<String> DEFAULT_CORS_ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
    );

    /** 跳过 JWT 校验的路径（Ant 风格）。仅放行无需登录态的端点，logout/revoke 等必须经鉴权 */
    private List<String> permitPaths = List.of(
            "/auth/oauth2/token",
            "/auth/oauth2/jwks",
            "/auth/.well-known/openid-configuration",
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/session/login-required",
            "/auth/session/login",
            "/*/v3/api-docs/**",
            "/actuator/health"
    );

    /** 网关跨域配置。生产环境应通过 Nacos 精确覆盖来源。 */
    private Cors cors = new Cors();

    public List<String> resolveCorsAllowedOriginPatterns() {
        if (cors.getAllowedOriginPatterns() != null && !cors.getAllowedOriginPatterns().isEmpty()) {
            return cors.getAllowedOriginPatterns();
        }
        if (cors.getAllowedOrigins() != null && !cors.getAllowedOrigins().isEmpty()) {
            return cors.getAllowedOrigins();
        }
        return DEFAULT_CORS_ALLOWED_ORIGIN_PATTERNS;
    }

    @Data
    public static class Cors {

        /** 推荐使用 allowed-origin-patterns，支持端口通配等模式匹配。 */
        private List<String> allowedOriginPatterns = List.of();

        /** 兼容旧配置名 allowed-origins，内部同样作为 patterns 使用。 */
        private List<String> allowedOrigins = List.of();
    }
}
