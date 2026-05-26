package com.yuan.daydayup.auth.controller;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWKs 公钥端点
 *
 * <p>网关、其他服务用此端点上的公钥校验 JWT 签名。</p>
 */
@RestController
public class JwksController {

    private static final JWKSelector ALL_KEYS = new JWKSelector(new JWKMatcher.Builder().build());

    private final JWKSource<SecurityContext> jwkSource;

    public JwksController(JWKSource<SecurityContext> jwkSource) {
        this.jwkSource = jwkSource;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() throws Exception {
        JWKSet jwkSet = new JWKSet(jwkSource.get(ALL_KEYS, null));
        return jwkSet.toJSONObject(true);
    }
}
