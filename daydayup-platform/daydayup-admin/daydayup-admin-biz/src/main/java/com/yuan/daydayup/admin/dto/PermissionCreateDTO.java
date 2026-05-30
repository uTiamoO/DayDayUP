package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionCreateDTO extends BaseRequestDTO {
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
