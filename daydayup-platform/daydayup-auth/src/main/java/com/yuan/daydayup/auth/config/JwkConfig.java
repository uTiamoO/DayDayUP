package com.yuan.daydayup.auth.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.yuan.daydayup.auth.entity.JwkKey;
import com.yuan.daydayup.auth.mapper.JwkKeyMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * JWT 签发 / 校验配置
 *
 * <p>从数据库表 {@code auth_jwk_key} 加载或初始化 RSA 密钥，支持多实例共享。</p>
 *
 * <p>首次启动时自动生成 RSA 2048 密钥对并持久化；后续启动从库中加载，
 * 保证所有认证中心实例使用同一密钥，网关验签不会因实例不同而失败。</p>
 *
 * <p>私钥采用 AES-GCM 加密后入库，密钥由 {@code daydayup.auth.jwk.master-password}
 * 经 HMAC-SHA256（HKDF-Extract 模式）派生。生产环境建议替换为 KMS / Vault 方案。</p>
 *
 * <p>密钥轮换：创建新 ACTIVE 密钥后，旧密钥改为 RETIRING / RETIRED，
 * 在当前单 ACTIVE 密钥架构下，轮换会导致旧 JWT 验签失败——
 * 若需无缝轮换，需将 {@link #jwkSource} 改为加载所有 ACTIVE + RETIRING 密钥。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class JwkConfig {

    private final JwkKeyMapper jwkKeyMapper;
    private final StringRedisTemplate redisTemplate;

    /** 私钥加密主密码（用于 AES-GCM 加密），生产环境建议替换为 KMS / Vault 方案 */
    private final String masterPassword;

    /** 当前活跃的 RSA 密钥，@PostConstruct 期间从数据库加载或首次初始化写入 */
    private RSAKey rsaKey;

    public JwkConfig(JwkKeyMapper jwkKeyMapper,
                     StringRedisTemplate redisTemplate,
                     @Value("${daydayup.auth.jwk.master-password:daydayup-jwk-default}") String masterPassword) {
        this.jwkKeyMapper = jwkKeyMapper;
        this.redisTemplate = redisTemplate;
        this.masterPassword = masterPassword;
    }

    /**
     * 启动时加载或初始化密钥。
     *
     * <p>多实例并发初始化时，通过 Redis 分布式锁保证仅一个实例执行密钥生成，
     * 其余实例自旋等待后从库中加载。</p>
     */
    @PostConstruct
    public void init() {
        warnIfDefaultMasterPassword();
        loadActiveKey();
    }

    @Bean
    public RSAKey rsaKey() {
        return rsaKey;
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
     */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (JOSEException e) {
            throw new IllegalStateException("构造 JwtDecoder 失败", e);
        }
    }

    // ==================== 密钥加载 / 初始化 ====================

    /**
     * 从数据库加载 ACTIVE 密钥；若不存在则通过 Redis 分布式锁互斥生成并入库。
     *
     * <p>多实例并发初始化时，通过 Redis 锁保证仅一个实例执行密钥生成，
     * 其余实例自旋等待锁释放后直接从库中加载。</p>
     */
    private void loadActiveKey() {
        // 快速路径：已有 ACTIVE 密钥
        JwkKey record = findActiveKey();
        if (record != null) {
            rsaKey = restoreRsaKey(record);
            log.info("从数据库加载 JWK 密钥: kid={}", record.getKid());
            return;
        }

        // 需要生成密钥：通过 Redis 分布式锁互斥
        String lockKey = "daydayup:auth:jwk-lock";
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 获取锁后再次检查（double-check）
                record = findActiveKey();
                if (record != null) {
                    rsaKey = restoreRsaKey(record);
                    log.info("从数据库加载 JWK 密钥: kid={}", record.getKid());
                    return;
                }
                log.info("未找到 ACTIVE JWK 密钥，开始生成新密钥...");
                JwkKey newRecord = createNewJwkKey();
                jwkKeyMapper.insert(newRecord);
                rsaKey = restoreRsaKey(newRecord);
                log.info("新 JWK 密钥已持久化: kid={}", newRecord.getKid());
            } finally {
                releaseLock(lockKey, lockValue);
            }
        } else {
            // 另一个实例正在生成密钥，自旋等待。
            // 总时长与锁 TTL 对齐，避免持锁实例尚未完成就提前超时导致本实例启动失败。
            log.info("等待其他实例完成 JWK 密钥初始化...");
            for (int i = 0; i < LOCK_TTL_SECONDS * 2; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("等待 JWK 初始化被中断", e);
                }
                record = findActiveKey();
                if (record != null) {
                    rsaKey = restoreRsaKey(record);
                    log.info("已加载并发实例创建的密钥: kid={}", record.getKid());
                    return;
                }
            }
            throw new IllegalStateException("等待 JWK 密钥初始化超时（" + LOCK_TTL_SECONDS + " 秒）");
        }
    }

    /** JWK 初始化分布式锁 TTL（秒），需覆盖 RSA 生成 + 入库耗时；自旋等待总时长与之对齐 */
    private static final int LOCK_TTL_SECONDS = 30;

    /** 私钥加密主密码默认值（与构造器 @Value 默认一致），用于启动时检测是否未配置 */
    private static final String DEFAULT_MASTER_PASSWORD = "daydayup-jwk-default";

    /** 仅当锁仍由本实例持有时才删除，避免 GC 停顿导致锁过期后误删其他实例的锁 */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private void releaseLock(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(
                    new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockValue);
        } catch (Exception e) {
            log.warn("释放 JWK 初始化锁失败: {}", e.getMessage());
        }
    }

    private void warnIfDefaultMasterPassword() {
        if (DEFAULT_MASTER_PASSWORD.equals(masterPassword)) {
            log.error("JWK master-password 仍为默认值，私钥加密形同虚设！"
                    + "生产环境必须通过环境变量 JWK_MASTER_PASSWORD 注入高熵随机值（建议 >= 32 字节）。");
        }
    }

    /**
     * 查询数据库中的 ACTIVE 密钥。
     */
    private JwkKey findActiveKey() {
        return jwkKeyMapper.selectOne(
                new LambdaQueryWrapper<JwkKey>()
                        .eq(JwkKey::getStatus, "ACTIVE")
                        .orderByAsc(JwkKey::getActivatedAt)
                        .last("LIMIT 1"));
    }

    /**
     * 生成新密钥并构建待入库的 JwkKey 实体（不执行 insert）。
     */
    private JwkKey createNewJwkKey() {
        RSAKey generated = generateRsa();
        String kid = generated.getKeyID();

        String publicKeyPem;
        byte[] privateKeyPkcs8;
        try {
            publicKeyPem = toPem("PUBLIC KEY",
                    ((RSAPublicKey) generated.toRSAPublicKey()).getEncoded());
            privateKeyPkcs8 = ((RSAPrivateKey) generated.toRSAPrivateKey()).getEncoded();
        } catch (JOSEException e) {
            throw new IllegalStateException("RSA 密钥编码失败", e);
        }

        String privateKeyEncrypted = encrypt(privateKeyPkcs8, kid);
        LocalDateTime now = LocalDateTime.now();

        JwkKey entity = new JwkKey();
        entity.setKid(kid);
        entity.setAlgorithm("RS256");
        entity.setPublicKey(publicKeyPem);
        entity.setPrivateKeyEncrypted(privateKeyEncrypted);
        entity.setStatus("ACTIVE");
        entity.setActivatedAt(now);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        return entity;
    }

    /**
     * 从数据库记录还原 RSAKey 对象。
     */
    private RSAKey restoreRsaKey(JwkKey record) {
        try {
            RSAPublicKey publicKey = parsePublicKey(record.getPublicKey());
            RSAPrivateKey privateKey = parsePrivateKey(
                    decrypt(record.getPrivateKeyEncrypted(), record.getKid()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(record.getKid())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("还原 JWK 密钥失败: kid=" + record.getKid(), e);
        }
    }

    // ==================== RSA 密钥生成 ====================

    private static RSAKey generateRsa() {
        try {
            java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            java.security.KeyPair kp = gen.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();
            return new RSAKey.Builder(pub)
                    .privateKey(priv)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("生成 RSA 密钥对失败", ex);
        }
    }

    // ==================== PEM 编解码 ====================

    private static String toPem(String type, byte[] der) {
        String b64 = Base64.getEncoder().encodeToString(der);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < b64.length(); i += 64) {
            sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
        }
        sb.append("-----END ").append(type).append("-----");
        return sb.toString();
    }

    private static RSAPublicKey parsePublicKey(String pem) throws Exception {
        byte[] der = pemToDer(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    /**
     * 从 PKCS8 编码字节还原 RSA 私钥（decrypt 直接返回原始编码，无需 PEM 包装）。
     */
    private static RSAPrivateKey parsePrivateKey(byte[] pkcs8Bytes) throws Exception {
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
    }

    private static byte[] pemToDer(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }

    // ==================== AES-GCM 加解密 ====================

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final byte[] SALT = "daydayup-jwk".getBytes(StandardCharsets.UTF_8);

    /**
     * AES-256-GCM 加密，IV 随机生成并拼接在密文前。
     */
    private String encrypt(byte[] plaintext, String kid) {
        try {
            SecretKeySpec keySpec = deriveKey(kid);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(kid.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /**
     * AES-256-GCM 解密。
     */
    private byte[] decrypt(String ciphertextBase64, String kid) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertextBase64);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            SecretKeySpec keySpec = deriveKey(kid);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(kid.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(combined, GCM_IV_LENGTH, combined.length - GCM_IV_LENGTH);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 解密失败: kid=" + kid, e);
        }
    }

    /**
     * 从主密码 + kid 使用 HMAC-SHA256（HKDF-Extract 模式）派生 AES-256 密钥。
     *
     * <p>SALT 作为 HMAC 密钥，输入作为消息，保证同库多实例能解密同一密文。</p>
     */
    private SecretKeySpec deriveKey(String kid) {
        try {
            String input = masterPassword + ":" + kid;
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(SALT, "HmacSHA256"));
            byte[] keyBytes = hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AES 密钥派生失败", e);
        }
    }
}
