package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BasePageQueryDTO extends BaseRequestDTO {

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    @Min(1)
    private Long pageNum = DEFAULT_PAGE_NUM;
    @Min(1)
    @Max(100)
    private Long pageSize = DEFAULT_PAGE_SIZE;

    public Long normalizedPageNum() {
        return pageNum == null || pageNum < DEFAULT_PAGE_NUM ? DEFAULT_PAGE_NUM : pageNum;
    }
    public Long normalizedPageSize() {
        if (pageSize == null || pageSize < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
