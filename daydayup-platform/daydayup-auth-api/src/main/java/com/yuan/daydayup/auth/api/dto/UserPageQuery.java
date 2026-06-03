package com.yuan.daydayup.auth.api.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends BasePageQueryDTO {
    private String username;
    private Integer status;
}
