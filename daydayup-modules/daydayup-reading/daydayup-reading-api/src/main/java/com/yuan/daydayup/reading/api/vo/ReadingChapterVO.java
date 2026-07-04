package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一阅读 API 章节摘要。
 */
@Data
@Schema(description = "统一阅读章节摘要")
public class ReadingChapterVO {

    @Schema(description = "统一章节 ID")
    private Long chapterId;

    @Schema(description = "作品 ID")
    private Long workId;

    @Schema(description = "章节标题")
    private String chapterTitle;

    @Schema(description = "章节序号")
    private Integer chapterIndex;

    @Schema(description = "卷名")
    private String volumeName;

    @Schema(description = "是否 VIP 章节")
    private boolean vipChapter;

    @Schema(description = "章节状态")
    private String chapterStatus;

    @Schema(description = "请求来源是否已有章节绑定")
    private boolean sourceAvailable;

    @Schema(description = "请求来源 ID")
    private Long sourceId;

    @Schema(description = "来源章节标题")
    private String sourceChapterTitle;
}
