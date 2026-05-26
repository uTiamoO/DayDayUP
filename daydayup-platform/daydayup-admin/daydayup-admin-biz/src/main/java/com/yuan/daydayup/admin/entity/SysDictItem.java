package com.yuan.daydayup.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典项（平台级）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    private String dictCode;

    private String value;

    private String label;

    private Integer sort;

    private Integer status;

    private String remark;
}
