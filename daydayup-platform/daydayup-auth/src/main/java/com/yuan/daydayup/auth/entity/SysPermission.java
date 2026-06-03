package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 权限定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private Long parentId;
    private String path;
    private Integer sort;
    private Integer status;
    private String remark;
}
