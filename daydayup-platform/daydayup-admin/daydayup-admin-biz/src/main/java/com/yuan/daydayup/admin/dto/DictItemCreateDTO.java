package com.yuan.daydayup.admin.dto;

import com.yuan.daydayup.common.core.dto.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictItemCreateDTO extends BaseRequestDTO {
    @NotBlank
    private String dictCode;
    @NotBlank
    private String value;
    @NotBlank
    private String label;
    private Integer sort;
    private String remark;
}
