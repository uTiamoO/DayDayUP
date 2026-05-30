package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MenuUpdateDTO extends BaseRequestDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private String type;
    private String permissionCode;
    private Integer sort;
    private Integer visible;
}
