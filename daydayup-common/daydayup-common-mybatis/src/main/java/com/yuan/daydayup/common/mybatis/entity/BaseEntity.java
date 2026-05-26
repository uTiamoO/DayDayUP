package com.yuan.daydayup.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有领域实体的统一基类
 *
 * <p>提供 6 个标准字段：</p>
 * <ul>
 *   <li>{@code id}：雪花算法主键，{@link IdType#ASSIGN_ID}</li>
 *   <li>{@code createTime / updateTime}：自动填充</li>
 *   <li>{@code createBy / updateBy}：自动填充（来自 {@code UserContextHolder}）</li>
 *   <li>{@code deleted}：逻辑删除标记，1=已删除</li>
 * </ul>
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;
}
