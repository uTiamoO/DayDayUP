package com.yuan.daydayup.reading.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 阅读 API 分页返回体。
 */
@Data
@Schema(description = "阅读 API 分页结果")
public class ReadingPageVO<T> {

    @Schema(description = "当前页数据")
    private List<T> list = new ArrayList<>();

    @Schema(description = "总数")
    private long total;

    @Schema(description = "页码，从 1 开始")
    private int page;

    @Schema(description = "每页条数")
    private int pageSize;

    @Schema(description = "是否还有下一页")
    private boolean hasNext;

    public static <T> ReadingPageVO<T> of(List<T> list, long total, int page, int pageSize) {
        ReadingPageVO<T> vo = new ReadingPageVO<>();
        vo.setList(list == null ? new ArrayList<>() : list);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setHasNext((long) page * pageSize < total);
        return vo;
    }
}
