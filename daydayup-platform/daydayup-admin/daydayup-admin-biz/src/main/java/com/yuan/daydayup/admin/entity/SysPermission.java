package com.yuan.daydayup.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限定义（平台级，所有租户共享同一套权限码）
 */
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
