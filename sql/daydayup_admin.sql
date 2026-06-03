-- ============================================================
-- DayDayUP 后台管理库 DDL + 种子数据
-- 数据库: daydayup_admin
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daydayup_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `daydayup_admin`;

-- -----------------------------------------------------------
-- 1. 菜单
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花）',
    `parent_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单 ID',
    `code`            VARCHAR(64)  NOT NULL COMMENT '菜单编码',
    `name`            VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `path`            VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    `component`       VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    `icon`            VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    `type`            VARCHAR(20)  NOT NULL DEFAULT 'menu' COMMENT '类型：dir / menu / button',
    `permission_code` VARCHAR(128) DEFAULT NULL COMMENT '关联权限码',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：0=隐藏，1=显示',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT       DEFAULT NULL,
    `update_by`       BIGINT       DEFAULT NULL,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- -----------------------------------------------------------
-- 2. 字典类型
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id`          BIGINT      NOT NULL COMMENT '主键（雪花）',
    `code`        VARCHAR(64) NOT NULL COMMENT '字典编码',
    `name`        VARCHAR(64) NOT NULL COMMENT '字典名称',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT      DEFAULT NULL,
    `update_by`   BIGINT      DEFAULT NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型';

-- -----------------------------------------------------------
-- 3. 字典项
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
    `id`          BIGINT       NOT NULL COMMENT '主键（雪花）',
    `dict_code`   VARCHAR(64)  NOT NULL COMMENT '所属字典编码',
    `value`       VARCHAR(128) NOT NULL COMMENT '字典值',
    `label`       VARCHAR(128) NOT NULL COMMENT '字典标签',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典项';

-- -----------------------------------------------------------
-- 4. 操作日志
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_oper_log` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花）',
    `user_id`        BIGINT       DEFAULT NULL COMMENT '操作人 ID',
    `username`       VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名',
    `module`         VARCHAR(64)  DEFAULT NULL COMMENT '操作模块',
    `operation`      VARCHAR(128) DEFAULT NULL COMMENT '操作描述',
    `method`         VARCHAR(255) DEFAULT NULL COMMENT '方法签名',
    `request_url`    VARCHAR(512) DEFAULT NULL COMMENT '请求 URI',
    `request_method` VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
    `request_ip`     VARCHAR(45)  DEFAULT NULL COMMENT '客户端 IP',
    `request_params` TEXT         DEFAULT NULL COMMENT '请求参数（JSON）',
    `response_body`  TEXT         DEFAULT NULL COMMENT '响应结果（JSON）',
    `success`        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：0=失败，1=成功',
    `error_msg`      TEXT         DEFAULT NULL COMMENT '异常信息',
    `cost_ms`        BIGINT       DEFAULT NULL COMMENT '耗时（毫秒）',
    `oper_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`      BIGINT       DEFAULT NULL,
    `update_by`      BIGINT       DEFAULT NULL,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ============================================================
-- 种子数据
-- ============================================================

-- 基础字典
INSERT INTO `sys_dict` (`id`, `code`, `name`, `status`, `create_by`) VALUES
(1, 'sys_user_status',  '用户状态',   1, 1),
(2, 'sys_role_status',  '角色状态',   1, 1),
(3, 'sys_menu_type',    '菜单类型',   1, 1),
(4, 'sys_common_status', '通用状态',  1, 1)
ON DUPLICATE KEY UPDATE `code` = `code`;

INSERT INTO `sys_dict_item` (`id`, `dict_code`, `value`, `label`, `sort`, `status`, `create_by`) VALUES
(1, 'sys_user_status',   '0', '停用', 1, 1, 1),
(2, 'sys_user_status',   '1', '启用', 2, 1, 1),
(3, 'sys_role_status',   '0', '停用', 1, 1, 1),
(4, 'sys_role_status',   '1', '启用', 2, 1, 1),
(5, 'sys_menu_type',     'dir',    '目录',   1, 1, 1),
(6, 'sys_menu_type',     'menu',   '菜单',   2, 1, 1),
(7, 'sys_menu_type',     'button', '按钮',   3, 1, 1),
(8, 'sys_common_status', '0', '停用', 1, 1, 1),
(9, 'sys_common_status', '1', '启用', 2, 1, 1)
ON DUPLICATE KEY UPDATE `dict_code` = `dict_code`;
