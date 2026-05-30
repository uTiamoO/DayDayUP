package com.yuan.daydayup.common.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseStatusDTO extends BaseRequestDTO {
    @NotNull
    private Integer status;
}
