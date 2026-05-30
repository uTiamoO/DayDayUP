package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseStatusDTO extends BaseRequestDTO {
    @NotNull
    private Integer status;
}
