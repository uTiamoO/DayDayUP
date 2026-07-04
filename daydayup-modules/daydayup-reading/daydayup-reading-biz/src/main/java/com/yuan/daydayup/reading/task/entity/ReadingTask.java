package com.yuan.daydayup.reading.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 阅读应用内任务。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_task")
public class ReadingTask extends BaseEntity {

    /** 任务类型：source_import/source_compile/work_discovery/toc_sync/content_fetch/content_sanitize */
    private String taskType;

    /** 同任务类型下的业务幂等键 */
    private String bizKey;

    /** JSON 参数 */
    private String payload;

    /** pending/running/succeeded/failed/partial_succeeded/cancelled */
    private String taskStatus;

    /** 当前重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 下次可执行时间 */
    private LocalDateTime nextRunAt;

    /** 锁持有者 */
    private String lockedBy;

    /** 锁定时间 */
    private LocalDateTime lockedAt;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;
}
