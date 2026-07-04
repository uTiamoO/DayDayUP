package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作品摘要（入库结果 / 列表用）。
 */
@Data
@Schema(description = "作品摘要")
public class WorkBriefVO {

    @Schema(description = "作品 ID（workId）")
    private Long workId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "归并状态：single_source / merged / suspect")
    private String aggregationStatus;

    @Schema(description = "已绑定来源数")
    private Integer sourceCount;
}
