package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BasePageQueryDTO extends BaseRequestDTO {
    @Min(1)
    private Long pageNum = 1L;
    @Min(1)
    @Max(100)
    private Long pageSize = 10L;

    public Long normalizedPageNum() {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }
    public Long normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }
}
