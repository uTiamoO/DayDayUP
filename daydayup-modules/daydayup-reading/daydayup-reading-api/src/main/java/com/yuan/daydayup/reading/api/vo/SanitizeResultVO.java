package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 净化结果（切片 4 子片 2）。
 */
@Data
@Schema(description = "正文净化结果")
public class SanitizeResultVO {

    @Schema(description = "章节 ID")
    private Long chapterId;

    @Schema(description = "来源书源 ID")
    private Long sourceId;

    @Schema(description = "净化运行记录 ID")
    private Long sanitizationRunId;

    @Schema(description = "Pipeline 版本")
    private String pipelineVersion;

    @Schema(description = "结果等级：accepted / degraded / rejected")
    private String runStatus;

    @Schema(description = "质量评分 0-100")
    private int qualityScore;

    @Schema(description = "删除片段数")
    private int removedCount;

    @Schema(description = "替换词数")
    private int replacedCount;

    @Schema(description = "净化后正文")
    private String sanitizedContent;

    @Schema(description = "执行 trace")
    private List<String> trace;
}
