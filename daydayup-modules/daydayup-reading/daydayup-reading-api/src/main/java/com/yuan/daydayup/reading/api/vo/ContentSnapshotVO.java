package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 章节正文快照结果（切片 4 抓取/查询）。
 */
@Data
@Schema(description = "章节正文快照")
public class ContentSnapshotVO {

    @Schema(description = "快照 ID")
    private Long snapshotId;

    @Schema(description = "统一章节 ID")
    private Long chapterId;

    @Schema(description = "来源书源 ID")
    private Long sourceId;

    @Schema(description = "状态：raw_only / normalized / sanitized / empty")
    private String contentStatus;

    @Schema(description = "标准化正文（默认返回层；净化后返回 sanitized 由 API 层决定）")
    private String content;

    @Schema(description = "标准化正文字符数")
    private int length;

    @Schema(description = "是否本次新抓（false=命中既有快照）")
    private boolean freshlyFetched;
}
