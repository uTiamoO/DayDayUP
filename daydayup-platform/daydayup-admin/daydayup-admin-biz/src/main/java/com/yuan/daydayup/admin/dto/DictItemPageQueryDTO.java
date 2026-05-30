package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictItemPageQueryDTO extends BasePageQueryDTO {
    private String dictCode;
    private String label;
    private Integer status;
}
