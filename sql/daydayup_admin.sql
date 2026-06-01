-- ============================================================
-- DayDayUP 后台管理库 DDL + 种子数据
-- 数据库: daydayup_admin
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daydayup_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `daydayup_admin`;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT       NOT NULL COMMENT '主键（雪花）',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `mobile`        VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`        VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `last_login_at` DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    `last_login_ip` VARCHAR(45)  DEFAULT NULL COMMENT '最近登录 IP',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_mobile` (`mobile`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 角色表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT      NOT NULL COMMENT '主键（雪花）',
    `code`        VARCHAR(64) NOT NULL COMMENT '角色编码',
    `name`        VARCHAR(64) NOT NULL COMMENT '角色名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT      DEFAULT NULL,
    `update_by`   BIGINT      DEFAULT NULL,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- -----------------------------------------------------------
-- 3. 用户-角色关联
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`          BIGINT   NOT NULL COMMENT '主键（雪花）',
    `user_id`     BIGINT   NOT NULL COMMENT '用户 ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT   DEFAULT NULL,
    `update_by`   BIGINT   DEFAULT NULL,
    `deleted`     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联';

-- -----------------------------------------------------------
-- 4. 权限定义
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       NOT NULL COMMENT '主键（雪花）',
    `code`        VARCHAR(128) NOT NULL COMMENT '权限码，如 admin:*',
    `name`        VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `type`        VARCHAR(20)  NOT NULL DEFAULT 'api' COMMENT '类型：api / menu / button',
    `parent_id`   BIGINT       DEFAULT 0 COMMENT '父权限 ID',
    `path`        VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限定义';

-- -----------------------------------------------------------
-- 5. 角色-权限关联
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT   NOT NULL COMMENT '主键（雪花）',
    `role_id`       BIGINT   NOT NULL COMMENT '角色 ID',
    `permission_id` BIGINT   NOT NULL COMMENT '权限 ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT   DEFAULT NULL,
    `update_by`     BIGINT   DEFAULT NULL,
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联';

-- -----------------------------------------------------------
-- 6. 菜单
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
-- 7. 字典类型
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
-- 8. 字典项
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
-- 9. 操作日志
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

-- 管理员用户（密码: admin123，BCrypt 加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `create_by`)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 1, 1)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- 角色
INSERT INTO `sys_role` (`id`, `code`, `name`, `sort`, `status`, `create_by`) VALUES
(1, 'admin', '超级管理员', 1, 1, 1),
(2, 'user',  '普通用户',   2, 1, 1)
ON DUPLICATE KEY UPDATE `code` = `code`;

-- 用户-角色关联
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_by`) VALUES
(1, 1, 1, 1)
ON DUPLICATE KEY UPDATE `user_id` = `user_id`;

-- 权限定义
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `sort`, `status`, `create_by`) VALUES
(1,  'admin:*',     '管理后台全部权限', 'api', 1,  1, 1),
(2,  'game:*',      '游戏服务全部权限', 'api', 2,  1, 1),
(3,  'game:play',   '游戏参与',         'api', 3,  1, 1),
(4,  'social:*',    '社交服务全部权限', 'api', 4,  1, 1),
(5,  'social:read', '社交只读',         'api', 5,  1, 1),
(6,  'user:read',   '用户查询',         'api', 6,  1, 1),
(7,  'user:write',  '用户管理',         'api', 7,  1, 1),
(8,  'dict:read',   '字典查询',         'api', 8,  1, 1),
(9,  'dict:write',  '字典管理',         'api', 9,  1, 1),
(10, 'menu:read',   '菜单查询',         'api', 10, 1, 1),
(11, 'menu:write',  '菜单管理',         'api', 11, 1, 1),
(12, 'role:read',   '角色查询',         'api', 12, 1, 1),
(13, 'role:write',  '角色管理',         'api', 13, 1, 1)
ON DUPLICATE KEY UPDATE `code` = `code`;

-- 角色-权限关联（admin 拥有所有权限）
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_by`) VALUES
(1,  1, 1,  1),
(2,  1, 2,  1),
(3,  1, 3,  1),
(4,  1, 4,  1),
(5,  1, 5,  1),
(6,  1, 6,  1),
(7,  1, 7,  1),
(8,  1, 8,  1),
(9,  1, 9,  1),
(10, 1, 10, 1),
(11, 1, 11, 1),
(12, 1, 12, 1),
(13, 1, 13, 1),
-- user 角色只有基础权限
(14, 2, 3,  1),
(15, 2, 5,  1),
(16, 2, 6,  1),
(17, 2, 8,  1),
(18, 2, 10, 1)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

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
