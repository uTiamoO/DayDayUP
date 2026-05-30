package com.yuan.daydayup.common.core.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<T> records;
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private Long pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNum, Long pageSize) {
        long safeTotal = total == null ? 0L : total;
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : pageSize;
        long pages = safeTotal == 0 ? 0L : (safeTotal + safePageSize - 1) / safePageSize;
        return new PageResult<>(records, safeTotal, safePageNum, safePageSize, pages);
    }
}
