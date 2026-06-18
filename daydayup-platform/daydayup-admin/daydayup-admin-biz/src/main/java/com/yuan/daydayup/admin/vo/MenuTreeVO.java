package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 菜单树节点：用于菜单树（管理）与当前用户动态菜单。 */
@Data
@Builder
public class MenuTreeVO {
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
    private List<MenuTreeVO> children;
}
