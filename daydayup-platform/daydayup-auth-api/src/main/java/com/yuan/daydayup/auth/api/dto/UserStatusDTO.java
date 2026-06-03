package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {
    @NotNull
    private Integer status;
}
