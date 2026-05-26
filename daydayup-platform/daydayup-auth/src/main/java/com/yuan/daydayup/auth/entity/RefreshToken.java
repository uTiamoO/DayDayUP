package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 刷新令牌：支持 /oauth2/refresh 与吊销
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_refresh_token")
public class RefreshToken extends BaseEntity {

    private Long userId;

    private String username;

    private String tokenHash;

    private LocalDateTime expiresAt;

    private Integer revoked;

    private LocalDateTime revokedAt;

    private String clientIp;

    private String userAgent;

    private LocalDateTime lastUsedAt;
}
