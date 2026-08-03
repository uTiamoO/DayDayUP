-- DayDayUP Reading schema additions for slice 6 task/reprocessing.
--
-- Usage:
--   mysql -h <host> -P <port> -u root -p < sql/daydayup_reading.sql
--
-- Notes:
--   1. Run as a privileged account when bootstrapping a new environment.
--   2. The application account is expected to already exist; this script grants
--      DML permissions only and does not create users or store passwords.
--   3. If the application account is not named `ddup`, adjust the GRANT target
--      before executing.

CREATE DATABASE IF NOT EXISTS `daydayup_reading`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `daydayup_reading`;

CREATE TABLE IF NOT EXISTS `reading_task` (
  `id` BIGINT NOT NULL COMMENT '雪花主键',
  `task_type` VARCHAR(64) NOT NULL COMMENT '任务类型：source_import/source_compile/work_discovery/toc_sync/content_fetch/content_sanitize',
  `biz_key` VARCHAR(255) NOT NULL COMMENT '同类型业务幂等键',
  `payload` JSON NULL COMMENT '任务执行参数 JSON',
  `task_status` VARCHAR(32) NOT NULL COMMENT 'pending/running/succeeded/failed/partial_succeeded/cancelled',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
  `max_retry` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `next_run_at` DATETIME NULL COMMENT '下次可执行时间',
  `locked_by` VARCHAR(128) NULL COMMENT '锁持有者',
  `locked_at` DATETIME NULL COMMENT '锁定时间',
  `error_message` VARCHAR(2000) NULL COMMENT '错误信息',
  `started_at` DATETIME NULL COMMENT '开始时间',
  `finished_at` DATETIME NULL COMMENT '结束时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT NULL COMMENT '创建人',
  `update_by` BIGINT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_active` (`task_type`, `biz_key`, `task_status`, `deleted`),
  KEY `idx_task_acquire` (`task_status`, `next_run_at`, `locked_at`, `deleted`),
  KEY `idx_task_page` (`task_type`, `task_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读任务表';

-- Application user permissions.
-- Keep password management outside repository. If user/host differs, replace `ddup`@`%` accordingly.
GRANT SELECT, INSERT, UPDATE, DELETE ON `daydayup_reading`.* TO `ddup`@`%`;
FLUSH PRIVILEGES;
