package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermissionCreateDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    @NotBlank
    private String type;
    private Long parentId;
    private String path;
    private Integer sort;
    private String remark;
}
