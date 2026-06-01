package com.yuan.daydayup.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JWT 签发 / 校验配置
 *
 * <p>使用进程内生成的 RSA 密钥对（仅供开发/演示）。
 * 生产环境应：</p>
 * <ul>
 *   <li>把私钥持久化在 Nacos 配置 / KMS / Vault 中</li>
 *   <li>JwkSource 改为从远程加载，并支持密钥轮换</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class JwkConfig {

    @Bean
    public RSAKey rsaKey() {
        return generateRsa();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 校验本认证中心签发的 JWT，供 logout 等需要解析「可信」token 的场景使用。
     *
     * <p>注意：当前 RSA 密钥为进程内随机生成（见类注释）。多实例部署时各实例公钥不同，
     * 跨实例验签会失败——生产环境改为共享/持久化密钥后方可依赖。</p>
     */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            throw new IllegalStateException("构造 JwtDecoder 失败", e);
        }
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 RSA 密钥对失败", ex);
        }
    }
}
