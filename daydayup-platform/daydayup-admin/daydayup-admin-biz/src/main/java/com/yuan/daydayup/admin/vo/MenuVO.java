package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MenuVO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String path;
    private String component;
    private String icon;
    private String type;
    private String permissionCode;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
