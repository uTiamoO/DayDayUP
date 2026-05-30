package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionPageQueryDTO extends BasePageQueryDTO {
    private String name;
    private String type;
    private Integer status;
}
