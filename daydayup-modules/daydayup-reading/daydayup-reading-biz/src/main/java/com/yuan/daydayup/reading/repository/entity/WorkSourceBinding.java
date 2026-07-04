package com.yuan.daydayup.reading.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作品-书源绑定（content-repository，对应 spec 3.2.4 WorkSourceBinding）。
 *
 * <p>统一作品与具体书源作品的映射。{@code (sourceId, sourceBookUrl)} 为该源内幂等键。
 * {@code isPrimarySource} 决定章节资产化基准来源（切片 3 子片 2）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_work_source_binding")
public class WorkSourceBinding extends BaseEntity {

    /** 关联统一作品 id */
    private Long workId;

    /** 关联书源 id */
    private Long sourceId;

    /** 来源书籍详情 URL（该源内幂等键） */
    private String sourceBookUrl;

    /** 来源书籍标题（原样） */
    private String sourceBookName;

    /** 来源作者名（原样） */
    private String sourceAuthorName;

    /** 匹配置信度 0-100 */
    private Integer matchConfidence;

    /** 是否主来源：1-是（章节资产化基准） */
    private Integer isPrimarySource;

    /** 绑定状态：active / unbound */
    private String bindingStatus;
}
