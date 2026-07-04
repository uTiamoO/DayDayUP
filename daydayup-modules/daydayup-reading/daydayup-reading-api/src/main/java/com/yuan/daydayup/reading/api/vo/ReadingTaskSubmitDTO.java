package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 阅读任务提交请求。
 */
@Data
@Schema(description = "阅读任务提交请求")
public class ReadingTaskSubmitDTO {

    @Schema(description = "任务类型：source_import/source_compile/work_discovery/toc_sync/content_fetch/content_sanitize", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "业务幂等键；为空时按任务类型与 payload 自动生成")
    private String bizKey;

    @Schema(description = "任务执行参数 JSON 对象")
    private Map<String, Object> payload;

    @Schema(description = "最大重试次数；为空使用服务默认值")
    private Integer maxRetry;

    @Schema(description = "下次可执行时间；为空表示立即可执行")
    private LocalDateTime nextRunAt;
}
