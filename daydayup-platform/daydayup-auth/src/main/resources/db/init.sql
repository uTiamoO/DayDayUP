-- ============================================================
-- daydayup_auth 数据库初始化脚本（手动执行，未接入 Flyway）
-- 适用版本：v0.1.0
-- ============================================================
-- 用法：
--   mysql -h 163.192.28.129 -P 23336 -u root -p < init.sql
-- 或在 MySQL 客户端中打开本文件整体执行。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daydayup_auth`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `daydayup_auth`;

-- ============================================================
-- auth_refresh_token：刷新令牌（支持 /oauth2/refresh 与吊销）
-- ============================================================
DROP TABLE IF EXISTS `auth_refresh_token`;
CREATE TABLE `auth_refresh_token` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `user_id`       BIGINT       NOT NULL COMMENT '关联用户 ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名（冗余，便于审计）',
    `token_hash`    VARCHAR(128) NOT NULL COMMENT 'Refresh Token 的 SHA-256（不存原文）',
    `expires_at`    DATETIME     NOT NULL COMMENT '过期时间',
    `revoked`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已吊销：0=否，1=是',
    `revoked_at`    DATETIME     NULL     COMMENT '吊销时间',
    `client_ip`     VARCHAR(64)  NULL     COMMENT '签发时客户端 IP',
    `user_agent`    VARCHAR(512) NULL     COMMENT '签发时 User-Agent',
    `last_used_at`  DATETIME     NULL     COMMENT '最近一次使用时间',
    `create_time`   DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`     BIGINT       NULL     COMMENT '创建人',
    `update_by`     BIGINT       NULL     COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌';

-- ============================================================
-- auth_login_history：登录历史 / 异常登录检测
-- ============================================================
DROP TABLE IF EXISTS `auth_login_history`;
CREATE TABLE `auth_login_history` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `user_id`         BIGINT       NULL     COMMENT '关联用户 ID（登录失败时可为空）',
    `username`        VARCHAR(64)  NOT NULL COMMENT '尝试登录的用户名',
    `login_at`        DATETIME     NOT NULL COMMENT '登录尝试时间',
    `client_ip`       VARCHAR(64)  NULL     COMMENT '客户端 IP',
    `user_agent`      VARCHAR(512) NULL     COMMENT 'User-Agent',
    `success`         TINYINT      NOT NULL COMMENT '是否成功：0=失败，1=成功',
    `failure_reason`  VARCHAR(128) NULL     COMMENT '失败原因（如 BAD_CREDENTIALS / USER_LOCKED）',
    `create_time`     DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`       BIGINT       NULL     COMMENT '创建人',
    `update_by`       BIGINT       NULL     COMMENT '更新人',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_at` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录历史';

-- ============================================================
-- auth_jwk_key：JWK 密钥存储（支持轮转与多实例共享）
-- ============================================================
DROP TABLE IF EXISTS `auth_jwk_key`;
CREATE TABLE `auth_jwk_key` (
    `id`                     BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    `kid`                    VARCHAR(64)   NOT NULL COMMENT 'Key ID，JWT header 中的 kid 字段',
    `algorithm`              VARCHAR(16)   NOT NULL COMMENT '签名算法，例如 RS256',
    `public_key`             TEXT          NOT NULL COMMENT 'PEM 格式公钥',
    `private_key_encrypted`  TEXT          NOT NULL COMMENT '加密后的私钥（建议 AES-GCM）',
    `status`                 VARCHAR(16)   NOT NULL COMMENT '状态：ACTIVE / RETIRING / RETIRED',
    `activated_at`           DATETIME      NOT NULL COMMENT '启用时间',
    `expires_at`             DATETIME      NULL     COMMENT '过期/退役时间',
    `create_time`            DATETIME      NOT NULL COMMENT '创建时间',
    `update_time`            DATETIME      NOT NULL COMMENT '更新时间',
    `create_by`              BIGINT        NULL     COMMENT '创建人',
    `update_by`              BIGINT        NULL     COMMENT '更新人',
    `deleted`                TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kid` (`kid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWK 密钥存储';

-- ============================================================
-- SAS 标准表（Spring Authorization Server）
-- ============================================================

CREATE TABLE IF NOT EXISTS `oauth2_registered_client` (
    `id`                            VARCHAR(100)  NOT NULL,
    `client_id`                     VARCHAR(100)  NOT NULL,
    `client_id_issued_at`           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `client_secret`                 VARCHAR(200)  DEFAULT NULL,
    `client_secret_expires_at`      TIMESTAMP     DEFAULT NULL,
    `client_name`                   VARCHAR(200)  NOT NULL,
    `client_authentication_methods` VARCHAR(1000) NOT NULL,
    `authorization_grant_types`     VARCHAR(1000) NOT NULL,
    `redirect_uris`                 VARCHAR(1000) DEFAULT NULL,
    `post_logout_redirect_uris`     VARCHAR(1000) DEFAULT NULL,
    `scopes`                        VARCHAR(1000) NOT NULL,
    `client_settings`               TEXT          NOT NULL,
    `token_settings`                TEXT          NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 客户端注册';

CREATE TABLE IF NOT EXISTS `oauth2_authorization` (
    `id`                            VARCHAR(100)  NOT NULL,
    `registered_client_id`          VARCHAR(100)  NOT NULL,
    `principal_name`                VARCHAR(200)  NOT NULL,
    `authorization_grant_type`      VARCHAR(100)  NOT NULL,
    `authorized_scopes`             VARCHAR(1000) DEFAULT NULL,
    `attributes`                    TEXT          DEFAULT NULL,
    `state`                         VARCHAR(500)  DEFAULT NULL,
    `authorization_code_value`      TEXT          DEFAULT NULL,
    `authorization_code_issued_at`  TIMESTAMP     DEFAULT NULL,
    `authorization_code_expires_at` TIMESTAMP     DEFAULT NULL,
    `authorization_code_metadata`   TEXT          DEFAULT NULL,
    `access_token_value`            TEXT          DEFAULT NULL,
    `access_token_issued_at`        TIMESTAMP     DEFAULT NULL,
    `access_token_expires_at`       TIMESTAMP     DEFAULT NULL,
    `access_token_metadata`         TEXT          DEFAULT NULL,
    `access_token_type`             VARCHAR(100)  DEFAULT NULL,
    `access_token_scopes`           VARCHAR(1000) DEFAULT NULL,
    `oidc_id_token_value`           TEXT          DEFAULT NULL,
    `oidc_id_token_issued_at`       TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_expires_at`      TIMESTAMP     DEFAULT NULL,
    `oidc_id_token_metadata`        TEXT          DEFAULT NULL,
    `refresh_token_value`           TEXT          DEFAULT NULL,
    `refresh_token_issued_at`       TIMESTAMP     DEFAULT NULL,
    `refresh_token_expires_at`      TIMESTAMP     DEFAULT NULL,
    `refresh_token_metadata`        TEXT          DEFAULT NULL,
    `user_code_value`               TEXT          DEFAULT NULL,
    `user_code_issued_at`           TIMESTAMP     DEFAULT NULL,
    `user_code_expires_at`          TIMESTAMP     DEFAULT NULL,
    `user_code_metadata`            TEXT          DEFAULT NULL,
    `device_code_value`             TEXT          DEFAULT NULL,
    `device_code_issued_at`         TIMESTAMP     DEFAULT NULL,
    `device_code_expires_at`        TIMESTAMP     DEFAULT NULL,
    `device_code_metadata`          TEXT          DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2 授权记录';

CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent` (
    `registered_client_id` VARCHAR(100)  NOT NULL,
    `principal_name`       VARCHAR(200)  NOT NULL,
    `authorities`          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户授权同意记录';

-- ============================================================
-- 种子 OAuth2 客户端
-- ============================================================

INSERT IGNORE INTO `oauth2_registered_client`
(`id`, `client_id`, `client_secret`, `client_name`,
 `client_authentication_methods`, `authorization_grant_types`,
 `redirect_uris`, `scopes`, `client_settings`, `token_settings`)
VALUES
('admin-web', 'admin-web', NULL, 'DayDayUP 管理后台',
 'none', 'authorization_code,refresh_token,password',
 'http://localhost:5173/callback',
 'openid,profile,admin:*',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}',
 '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.refresh-token-time-to-live":["java.time.Duration",604800.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"}}');
