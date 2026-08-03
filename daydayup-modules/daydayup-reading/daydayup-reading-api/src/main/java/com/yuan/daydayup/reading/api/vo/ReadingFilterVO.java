package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 阅读筛选元数据。
 */
@Data
@Schema(description = "阅读筛选元数据")
public class ReadingFilterVO {

    @Schema(description = "筛选编码")
    private String code;

    @Schema(description = "展示名称")
    private String name;

    @Schema(description = "数量")
    private long count;

    public static ReadingFilterVO of(String code, String name, long count) {
        ReadingFilterVO vo = new ReadingFilterVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setCount(count);
        return vo;
    }
}
