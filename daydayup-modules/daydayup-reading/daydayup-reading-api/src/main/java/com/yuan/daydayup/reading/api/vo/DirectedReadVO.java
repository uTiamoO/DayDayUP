package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 书源定向读取结果（内网调试 / 灰度接口 {@code /api/v1/internal/source-reading}）。
 *
 * <p>列表类动作（search）填 {@link #records}；单对象动作（detail）填 {@link #record}。</p>
 */
@Data
@Schema(description = "书源定向读取结果")
public class DirectedReadVO {

    @Schema(description = "书源 ID")
    private Long sourceId;

    @Schema(description = "书源名称")
    private String sourceName;

    @Schema(description = "动作：search / detail")
    private String action;

    @Schema(description = "实际请求的源站 URL")
    private String requestUrl;

    @Schema(description = "整链路耗时（毫秒）")
    private Long elapsedMs;

    @Schema(description = "列表类动作结果（每项 = 字段名 -> 值）")
    private List<Map<String, String>> records;

    @Schema(description = "单对象动作结果（字段名 -> 值）")
    private Map<String, String> record;
}
