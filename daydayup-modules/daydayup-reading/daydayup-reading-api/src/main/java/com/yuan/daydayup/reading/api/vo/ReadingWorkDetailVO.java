package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一阅读 API 作品详情。
 */
@Data
@Schema(description = "统一阅读作品详情")
public class ReadingWorkDetailVO {

    @Schema(description = "作品 ID")
    private Long workId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "作者")
    private String authorName;

    @Schema(description = "分类")
    private String categoryName;

    @Schema(description = "封面 URL")
    private String coverUrl;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "完结状态：serial / completed / unknown")
    private String completionStatus;

    @Schema(description = "字数")
    private Long wordCount;

    @Schema(description = "最新章节标题")
    private String latestChapterTitle;

    @Schema(description = "最新章节更新时间")
    private LocalDateTime latestChapterUpdatedAt;

    @Schema(description = "归并状态")
    private String aggregationStatus;

    @Schema(description = "来源数量")
    private Integer sourceCount;

    @Schema(description = "主来源摘要")
    private ReadingSourceVO primarySource;

    @Schema(description = "最新统一章节摘要")
    private ReadingChapterVO latestChapter;
}
