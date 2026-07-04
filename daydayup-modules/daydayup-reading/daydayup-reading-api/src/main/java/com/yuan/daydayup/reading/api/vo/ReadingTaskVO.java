package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阅读任务详情/摘要。
 */
@Data
@Schema(description = "阅读任务")
public class ReadingTaskVO {

    @Schema(description = "任务 ID")
    private Long id;

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "业务幂等键")
    private String bizKey;

    @Schema(description = "任务参数 JSON")
    private String payload;

    @Schema(description = "任务状态")
    private String taskStatus;

    @Schema(description = "当前重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "下次可执行时间")
    private LocalDateTime nextRunAt;

    @Schema(description = "锁持有者")
    private String lockedBy;

    @Schema(description = "锁定时间")
    private LocalDateTime lockedAt;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
