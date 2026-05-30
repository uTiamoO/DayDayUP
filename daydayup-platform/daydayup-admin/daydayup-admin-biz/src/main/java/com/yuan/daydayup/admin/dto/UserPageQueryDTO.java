package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQueryDTO extends BasePageQueryDTO {
    private String username;
    private String nickname;
    private Integer status;
}
