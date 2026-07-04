package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 阅读任务分页查询请求。
 */
@Data
@Schema(description = "阅读任务分页查询请求")
public class ReadingTaskPageQueryDTO {

    @Schema(description = "任务类型")
    private String taskType;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 20;
}
