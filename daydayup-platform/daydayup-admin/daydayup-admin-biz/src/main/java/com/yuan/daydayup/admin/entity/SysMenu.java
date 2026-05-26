package com.yuan.daydayup.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单（平台级）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

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
}
