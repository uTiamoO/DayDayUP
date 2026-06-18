package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private Integer sort;
    private String remark;
}
