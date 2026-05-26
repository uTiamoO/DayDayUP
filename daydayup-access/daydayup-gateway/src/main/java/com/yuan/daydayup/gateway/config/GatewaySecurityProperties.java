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

    /** 跳过 JWT 校验的路径（Ant 风格） */
    private List<String> permitPaths = List.of(
            "/auth/**",
            "/*/v3/api-docs/**",
            "/actuator/**"
    );
}
