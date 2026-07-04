package com.yuan.daydayup.reading.source.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 书源定义（source-center 的外部书源资产）
 *
 * <p>对应 spec 3.2.1 SourceDefinition。保存导入的原始 Legado 书源内容与管理属性，
 * 是编译（→ RuleModel）与运行时的上游。</p>
 *
 * <p>幂等键为 {@code bookSourceUrl}（Legado 天然唯一标识）；重导入时 {@code status}
 * 与 {@code priority} 为用户可修改字段，予以保留。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_source_definition")
public class SourceDefinition extends BaseEntity {

    /** 书源名称（Legado bookSourceName） */
    private String name;

    /** 站点名 / 分组（Legado bookSourceGroup） */
    private String siteName;

    /** 书源基准 URL，Legado 天然唯一标识（导入幂等键） */
    private String bookSourceUrl;

    /** 类型：0-文字 1-听书 2-漫画（第一期仅编译 0） */
    private Integer bookSourceType;

    /** 来源格式：legado（v1 唯一） */
    private String originType;

    /** 导入来源路径（文件名等） */
    private String originPath;

    /** 启用状态：0-禁用 1-启用（用户可改，重导入保留） */
    private Integer status;

    /** 优先级（对应 Legado weight，用户可改，重导入保留） */
    private Integer priority;

    /** 标签（逗号分隔） */
    private String tags;

    /** 最近编译等级：full/degraded/rejected（后续切片写入） */
    private String compileGrade;

    /** 原始规则内容 SHA-256，用于重导入判断是否变更 */
    private String fingerprint;

    /** 最近导入时间 */
    private LocalDateTime importedAt;

    /** 原始书源 JSON（保留，供编译与回放） */
    private String rawContent;
}
