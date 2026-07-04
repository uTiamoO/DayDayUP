package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一阅读 API 作品摘要。
 */
@Data
@Schema(description = "统一阅读作品摘要")
public class ReadingWorkVO {

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

    @Schema(description = "完结状态：serial / completed / unknown")
    private String completionStatus;

    @Schema(description = "最新章节标题")
    private String latestChapterTitle;

    @Schema(description = "归并状态：single_source / merged / suspect")
    private String aggregationStatus;

    @Schema(description = "命中来源数")
    private Integer sourceCount;
}
