package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 阅读任务 worker drain 结果。
 */
@Data
@Schema(description = "阅读任务 worker drain 结果")
public class ReadingTaskDrainVO {

    @Schema(description = "本次请求最多执行任务数")
    private int limit;

    @Schema(description = "实际抢占任务数")
    private int acquired;

    @Schema(description = "成功数")
    private int succeeded;

    @Schema(description = "部分成功数")
    private int partialSucceeded;

    @Schema(description = "失败并待重试数")
    private int retryScheduled;

    @Schema(description = "最终失败数")
    private int failed;

    @Schema(description = "执行过的任务 ID")
    private List<Long> taskIds = new ArrayList<>();
}
