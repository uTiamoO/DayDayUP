package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionVO {
    private Long id;
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String path;
    private Integer sort;
    private Integer status;
    private String remark;
}
