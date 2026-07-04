package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容发现 / 作品入库结果（spec §5.2 内容发现与作品入库链路）。
 */
@Data
@Schema(description = "作品入库结果")
public class WorkDiscoveryResultVO {

    @Schema(description = "候选总数")
    private int total;

    @Schema(description = "新建作品数")
    private int worksCreated;

    @Schema(description = "命中既有作品数（保守归并）")
    private int worksMatched;

    @Schema(description = "新建来源绑定数")
    private int bindingsCreated;

    @Schema(description = "更新来源绑定数")
    private int bindingsUpdated;

    @Schema(description = "跳过数（标题/URL 缺失）")
    private int skipped;

    @Schema(description = "涉及的作品摘要")
    private List<WorkBriefVO> works = new ArrayList<>();
}
