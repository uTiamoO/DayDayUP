package com.yuan.daydayup.reading.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一章节（content-repository，对应 spec 3.2.5 Chapter）。
 *
 * <p>基于主来源（{@code isPrimarySource}）目录建立，{@code chapterIndex} 即主来源目录序。
 * 第一期不做全局跨源章节对齐（spec §5.3）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_chapter")
public class Chapter extends BaseEntity {

    /** 关联统一作品 id */
    private Long workId;

    /** 章节标题 */
    private String chapterTitle;

    /** 章节序号（从 0 起，主来源目录序） */
    private Integer chapterIndex;

    /** 卷名 */
    private String volumeName;

    /** 是否 VIP 章节 */
    private Integer isVipChapter;

    /** 章节状态：active / removed */
    private String chapterStatus;
}
