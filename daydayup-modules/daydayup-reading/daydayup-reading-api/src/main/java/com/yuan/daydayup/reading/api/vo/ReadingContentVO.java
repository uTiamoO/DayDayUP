package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一阅读 API 正文响应。
 */
@Data
@Schema(description = "统一阅读章节正文")
public class ReadingContentVO {

    @Schema(description = "统一章节 ID")
    private Long chapterId;

    @Schema(description = "来源书源 ID")
    private Long sourceId;

    @Schema(description = "快照 ID")
    private Long contentSnapshotId;

    @Schema(description = "返回正文版本：raw / normalized / sanitized")
    private String contentVersion;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "快照状态")
    private String contentStatus;

    @Schema(description = "质量评分 0-100")
    private Integer qualityScore;

    @Schema(description = "净化运行记录 ID")
    private Long sanitizationRunId;

    @Schema(description = "是否本次触发抓取或净化")
    private boolean freshlyFetched;
}
