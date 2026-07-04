package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 目录同步结果（spec §5.3 目录抓取与章节资产化链路）。
 */
@Data
@Schema(description = "目录同步结果")
public class TocSyncResultVO {

    @Schema(description = "作品 ID")
    private Long workId;

    @Schema(description = "来源书源 ID")
    private Long sourceId;

    @Schema(description = "该来源是否主来源（主来源才建统一章节）")
    private boolean primarySource;

    @Schema(description = "抓取到的章节条目数")
    private int tocEntries;

    @Schema(description = "重建的统一章节数（仅主来源 > 0）")
    private int chaptersBuilt;

    @Schema(description = "重建的来源章节绑定数")
    private int bindingsBuilt;

    @Schema(description = "整链路耗时（毫秒）")
    private Long elapsedMs;
}
