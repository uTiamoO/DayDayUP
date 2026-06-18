package com.yuan.daydayup.auth.api.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionPageQuery extends BasePageQueryDTO {
    private String name;
    private String type;
    private Integer status;
}
