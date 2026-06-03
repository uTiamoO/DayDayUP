-- ============================================================
-- DayDayUP 认证中心库 DDL
-- 数据库: daydayup_auth
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daydayup_auth` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `daydayup_auth`;

-- -----------------------------------------------------------
-- 1. 刷新令牌
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_refresh_token` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `user_id`       BIGINT       NOT NULL COMMENT '用户 ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `token_hash`    VARCHAR(128) NOT NULL COMMENT 'SHA-256(token)',
    `expires_at`    DATETIME     NOT NULL COMMENT '过期时间',
    `revoked`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已吊销：0=有效，1=已吊销',
    `revoked_at`    DATETIME     DEFAULT NULL COMMENT '吊销时间',
    `client_ip`     VARCHAR(45)  DEFAULT NULL COMMENT '签发时客户端 IP',
    `user_agent`    VARCHAR(512) DEFAULT NULL COMMENT '签发时 User-Agent',
    `last_used_at`  DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT       DEFAULT NULL,
    `update_by`     BIGINT       DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌';

-- -----------------------------------------------------------
-- 2. 登录历史
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_login_history` (
    `id`              BIGINT      NOT NULL COMMENT '主键（雪花）',
    `user_id`         BIGINT      NOT NULL COMMENT '用户 ID',
    `username`        VARCHAR(64) NOT NULL COMMENT '用户名',
    `login_at`        DATETIME    NOT NULL COMMENT '登录时间',
    `client_ip`       VARCHAR(45) DEFAULT NULL COMMENT '客户端 IP',
    `user_agent`      VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    `success`         TINYINT     NOT NULL DEFAULT 1 COMMENT '是否成功：0=失败，1=成功',
    `failure_reason`  VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT      DEFAULT NULL,
    `update_by`       BIGINT      DEFAULT NULL,
    `deleted`         TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_at` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录历史';

-- -----------------------------------------------------------
-- 3. JWK 密钥存储
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `auth_jwk_key` (
    `id`                    BIGINT       NOT NULL COMMENT '主键（雪花）',
    `kid`                   VARCHAR(128) NOT NULL COMMENT 'Key ID',
    `algorithm`             VARCHAR(20)  NOT NULL DEFAULT 'RS256' COMMENT '算法',
    `public_key`            TEXT         NOT NULL COMMENT '公钥（PEM）',
    `private_key_encrypted` TEXT         NOT NULL COMMENT '私钥（加密后 PEM）',
    `status`                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / RETIRING / RETIRED',
    `activated_at`          DATETIME     DEFAULT NULL COMMENT '激活时间',
    `expires_at`            DATETIME     DEFAULT NULL COMMENT '过期时间',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`             BIGINT       DEFAULT NULL,
    `update_by`             BIGINT       DEFAULT NULL,
    `deleted`               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kid` (`kid`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='JWK 密钥存储';
