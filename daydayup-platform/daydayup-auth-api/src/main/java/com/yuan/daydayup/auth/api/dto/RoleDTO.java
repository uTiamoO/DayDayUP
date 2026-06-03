package com.yuan.daydayup.auth.api.dto;

import lombok.Data;

@Data
public class RoleDTO {
    private Long id;
    private String code;
    private String name;
    private Integer status;
}
