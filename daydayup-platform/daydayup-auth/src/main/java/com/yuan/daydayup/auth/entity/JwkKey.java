package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * JWK 密钥存储：支持密钥轮转与多实例共享
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_jwk_key")
public class JwkKey extends BaseEntity {

    private String kid;

    private String algorithm;

    private String publicKey;

    private String privateKeyEncrypted;

    private String status;

    private LocalDateTime activatedAt;

    private LocalDateTime expiresAt;
}
