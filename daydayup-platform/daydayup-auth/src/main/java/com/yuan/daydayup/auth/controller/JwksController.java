package com.yuan.daydayup.auth.controller;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWKs 公钥端点
 *
 * <p>网关、其他服务用此端点上的公钥校验 JWT 签名。</p>
 */
@RestController
@Tag(name = "JWK 公钥", description = "对外暴露 JWT 验签公钥")
public class JwksController {

    private static final JWKSelector ALL_KEYS = new JWKSelector(new JWKMatcher.Builder().build());

    private final JWKSource<SecurityContext> jwkSource;

    public JwksController(JWKSource<SecurityContext> jwkSource) {
        this.jwkSource = jwkSource;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "获取 JWK 公钥集合", description = "网关与下游服务用此公钥校验 JWT 签名")
    @SecurityRequirements
    public Map<String, Object> jwks() throws Exception {
        JWKSet jwkSet = new JWKSet(jwkSource.get(ALL_KEYS, null));
        return jwkSet.toJSONObject(true);
    }
}
