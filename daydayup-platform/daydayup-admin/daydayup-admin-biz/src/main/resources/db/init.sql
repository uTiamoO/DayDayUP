-- ============================================================
-- daydayup_admin 数据库初始化脚本（手动执行，未接入 Flyway）
-- 适用版本：v0.1.0
-- ============================================================
-- 用法：
--   mysql -h 163.192.28.129 -P 23336 -u root -p < init.sql
-- 或在 MySQL 客户端中打开本文件整体执行。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daydayup_admin`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `daydayup_admin`;

-- ============================================================
-- sys_user：用户
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `username`       VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`       VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `nickname`       VARCHAR(64)  NULL     COMMENT '昵称',
    `email`          VARCHAR(128) NULL     COMMENT '邮箱',
    `mobile`         VARCHAR(32)  NULL     COMMENT '手机号',
    `avatar`         VARCHAR(256) NULL     COMMENT '头像 URL',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `last_login_at`  DATETIME     NULL     COMMENT '最近一次登录时间',
    `last_login_ip`  VARCHAR(64)  NULL     COMMENT '最近一次登录 IP',
    `remark`         VARCHAR(256) NULL     COMMENT '备注',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_mobile` (`mobile`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ============================================================
-- sys_role：角色
-- ============================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `code`           VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `name`           VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `sort`           INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`         VARCHAR(256) NULL     COMMENT '备注',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

-- ============================================================
-- sys_user_role：用户-角色关联
-- ============================================================
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `user_id`        BIGINT       NOT NULL COMMENT '用户 ID',
    `role_id`        BIGINT       NOT NULL COMMENT '角色 ID',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- ============================================================
-- sys_permission：权限定义（IGNORE_TABLES，平台级，所有租户共享）
-- ============================================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `code`           VARCHAR(128) NOT NULL COMMENT '权限标识（如 admin:user:create）',
    `name`           VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `type`           VARCHAR(16)  NOT NULL COMMENT '类型：MENU / BUTTON / API',
    `parent_id`      BIGINT       NULL     COMMENT '父权限 ID（树形）',
    `path`           VARCHAR(256) NULL     COMMENT '资源路径（API 模式用 URL）',
    `sort`           INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`         VARCHAR(256) NULL     COMMENT '备注',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限定义';

-- ============================================================
-- sys_role_permission：角色-权限关联（IGNORE_TABLES）
-- ============================================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `role_id`        BIGINT       NOT NULL COMMENT '角色 ID',
    `permission_id`  BIGINT       NOT NULL COMMENT '权限 ID',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_role` (`role_id`),
    KEY `idx_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- ============================================================
-- sys_menu：菜单（IGNORE_TABLES，平台级）
-- ============================================================
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`               BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `parent_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单 ID（0 表示根）',
    `code`             VARCHAR(64)  NOT NULL COMMENT '菜单编码',
    `name`             VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `path`             VARCHAR(256) NULL     COMMENT '前端路由 path',
    `component`        VARCHAR(256) NULL     COMMENT '前端组件路径',
    `icon`             VARCHAR(64)  NULL     COMMENT '图标',
    `type`             VARCHAR(16)  NOT NULL COMMENT '类型：DIRECTORY / MENU / BUTTON',
    `permission_code`  VARCHAR(128) NULL     COMMENT '关联权限编码',
    `sort`             INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`          TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：0=隐藏，1=可见',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `create_time`      DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`        BIGINT       NULL     COMMENT '创建人',
    `update_by`        BIGINT       NULL     COMMENT '更新人',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单';

-- ============================================================
-- sys_dict：字典类型（IGNORE_TABLES）
-- ============================================================
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `code`           VARCHAR(64)  NOT NULL COMMENT '字典编码',
    `name`           VARCHAR(64)  NOT NULL COMMENT '字典名称',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`         VARCHAR(256) NULL     COMMENT '备注',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型';

-- ============================================================
-- sys_dict_item：字典项（IGNORE_TABLES）
-- ============================================================
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
    `id`             BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    `dict_code`      VARCHAR(64)  NOT NULL COMMENT '所属字典编码',
    `value`          VARCHAR(64)  NOT NULL COMMENT '值',
    `label`          VARCHAR(64)  NOT NULL COMMENT '显示文本',
    `sort`           INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=停用，1=启用',
    `remark`         VARCHAR(256) NULL     COMMENT '备注',
    `create_time`    DATETIME     NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL COMMENT '更新时间',
    `create_by`      BIGINT       NULL     COMMENT '创建人',
    `update_by`      BIGINT       NULL     COMMENT '更新人',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_value` (`dict_code`, `value`),
    KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项';

-- ============================================================
-- sys_oper_log：操作日志
-- ============================================================
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `id`               BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    `user_id`          BIGINT        NULL     COMMENT '操作用户 ID',
    `username`         VARCHAR(64)   NULL     COMMENT '操作用户名',
    `module`           VARCHAR(64)   NULL     COMMENT '所属模块',
    `operation`        VARCHAR(128)  NULL     COMMENT '操作描述',
    `method`           VARCHAR(256)  NULL     COMMENT '方法签名',
    `request_url`      VARCHAR(512)  NULL     COMMENT '请求 URL',
    `request_method`   VARCHAR(16)   NULL     COMMENT '请求方法：GET / POST 等',
    `request_ip`       VARCHAR(64)   NULL     COMMENT '请求 IP',
    `request_params`   TEXT          NULL     COMMENT '请求参数（可截断）',
    `response_body`    TEXT          NULL     COMMENT '响应内容（可截断）',
    `success`          TINYINT       NOT NULL COMMENT '是否成功：0=失败，1=成功',
    `error_msg`        VARCHAR(1024) NULL     COMMENT '失败信息',
    `cost_ms`          BIGINT        NULL     COMMENT '耗时（毫秒）',
    `oper_time`        DATETIME      NOT NULL COMMENT '操作时间',
    `create_time`      DATETIME      NOT NULL COMMENT '创建时间',
    `update_time`      DATETIME      NOT NULL COMMENT '更新时间',
    `create_by`        BIGINT        NULL     COMMENT '创建人',
    `update_by`        BIGINT        NULL     COMMENT '更新人',
    `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_oper_time` (`oper_time`),
    KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================================
-- 初始种子数据
-- ============================================================
-- 密码均为 BCrypt 加密：admin / user
-- 如需重新生成，运行 org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

-- 用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `create_time`, `update_time`)
VALUES
    (1, 'admin', '$2a$10$8q/bE/Rs5syhpAzbufjvL.ct97ZzkkRfnt4fy/Aqa4GDSRKoql8VG', '管理员', 1, NOW(), NOW()),
    (2, 'user',  '$2a$10$2usIZSDTm.ACWa.zXTFP7.TVggpGQegm2Z7tqA.RcbmMZAQnj17f.', '普通用户', 1, NOW(), NOW());

-- 角色
INSERT INTO `sys_role` (`id`, `code`, `name`, `sort`, `status`, `create_time`, `update_time`)
VALUES
    (1, 'super_admin', '超级管理员', 1, 1, NOW(), NOW()),
    (2, 'normal_user', '普通用户',   2, 1, NOW(), NOW());

-- 用户-角色关联
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`, `update_time`)
VALUES
    (1, 1, 1, NOW(), NOW()),
    (2, 2, 2, NOW(), NOW());

-- 权限定义
INSERT INTO `sys_permission` (`id`, `code`, `name`, `type`, `sort`, `status`, `create_time`, `update_time`)
VALUES
    (1, 'admin:*',     '管理后台全部权限', 'API', 1, 1, NOW(), NOW()),
    (2, 'game:*',      '游戏模块全部权限', 'API', 2, 1, NOW(), NOW()),
    (3, 'game:play',   '游戏-游玩',       'API', 3, 1, NOW(), NOW()),
    (4, 'social:*',    '社交模块全部权限', 'API', 4, 1, NOW(), NOW()),
    (5, 'social:read', '社交-只读',       'API', 5, 1, NOW(), NOW());

-- 角色-权限关联：超级管理员 → admin:*, game:*, social:*
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`)
VALUES
    (1, 1, 1, NOW(), NOW()),
    (2, 1, 2, NOW(), NOW()),
    (3, 1, 4, NOW(), NOW());
-- 角色-权限关联：普通用户 → game:play, social:read
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `create_time`, `update_time`)
VALUES
    (4, 2, 3, NOW(), NOW()),
    (5, 2, 5, NOW(), NOW());
