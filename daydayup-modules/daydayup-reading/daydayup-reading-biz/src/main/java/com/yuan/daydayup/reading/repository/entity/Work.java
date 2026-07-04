package com.yuan.daydayup.reading.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 统一作品（content-repository，对应 spec 3.2.3 Work）。
 *
 * <p>平台内部作品资产，与来源书籍解耦；多个书源可通过 {@link WorkSourceBinding} 绑定到同一 Work。
 * {@code matchKey} 为保守归并键（规整标题+作者），用于 find-or-create。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_work")
public class Work extends BaseEntity {

    /** 作品标题（展示用，取自主来源） */
    private String title;

    /** 作者名 */
    private String authorName;

    /** 分类 */
    private String categoryName;

    /** 封面 URL */
    private String coverUrl;

    /** 简介 */
    private String description;

    /** 完结状态：serial / completed / unknown */
    private String completionStatus;

    /** 字数 */
    private Long wordCount;

    /** 最新章节标题 */
    private String latestChapterTitle;

    /** 最新章节更新时间 */
    private LocalDateTime latestChapterUpdatedAt;

    /** 归并状态：single_source / merged / suspect */
    private String aggregationStatus;

    /** 保守归并键：规整(标题)规整(作者) */
    private String matchKey;

    /** 已绑定来源数 */
    private Integer sourceCount;
}
