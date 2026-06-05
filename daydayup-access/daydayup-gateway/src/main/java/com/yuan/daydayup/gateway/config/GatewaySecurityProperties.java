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

    /** 跳过 JWT 校验的路径（Ant 风格）。仅放行无需登录态的端点，logout/revoke 等必须经鉴权 */
    private List<String> permitPaths = List.of(
            "/auth/oauth2/token",
            "/auth/oauth2/jwks",
            "/auth/.well-known/openid-configuration",
            "/*/v3/api-docs/**",
            "/actuator/**"
    );
}
