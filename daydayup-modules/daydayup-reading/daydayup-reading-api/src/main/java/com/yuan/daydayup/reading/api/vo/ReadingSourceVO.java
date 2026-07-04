package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 阅读来源摘要。只暴露平台字段，不暴露源站明文 URL。
 */
@Data
@Schema(description = "阅读来源摘要")
public class ReadingSourceVO {

    @Schema(description = "来源书源 ID")
    private Long sourceId;

    @Schema(description = "书源名称")
    private String sourceName;

    @Schema(description = "站点/分组名称")
    private String siteName;

    @Schema(description = "启用状态：0-禁用 1-启用")
    private Integer status;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "是否主来源")
    private boolean primarySource;

    @Schema(description = "绑定状态：active / unbound")
    private String bindingStatus;

    @Schema(description = "来源内书名")
    private String sourceBookName;

    @Schema(description = "来源内作者")
    private String sourceAuthorName;

    @Schema(description = "匹配置信度 0-100")
    private Integer matchConfidence;
}
