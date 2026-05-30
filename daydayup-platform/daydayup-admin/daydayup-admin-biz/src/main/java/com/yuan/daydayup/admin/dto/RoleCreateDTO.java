package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleCreateDTO extends BaseRequestDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private Integer sort;
    private String remark;
}
