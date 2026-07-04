package com.yuan.daydayup.reading.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节-书源绑定（content-repository，对应 spec 3.2.6 ChapterSourceBinding）。
 *
 * <p>统一章节与具体书源章节的映射。非主来源仅保留 per-source 目录（{@code chapterId} 可空，
 * 待惰性对齐），主来源建 Chapter 时同建绑定并回填 {@code chapterId}。
 * {@code (sourceId, workId, sourceChapterOrder)} 为幂等键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_chapter_source_binding")
public class ChapterSourceBinding extends BaseEntity {

    /** 关联统一作品 id */
    private Long workId;

    /** 关联书源 id */
    private Long sourceId;

    /** 关联统一章节 id（非主来源惰性对齐前可为空） */
    private Long chapterId;

    /** 来源章节正文 URL */
    private String sourceChapterUrl;

    /** 来源章节标题（原样） */
    private String sourceChapterTitle;

    /** 来源目录内章节序（从 0 起） */
    private Integer sourceChapterOrder;

    /** 对齐置信度 0-100 */
    private Integer bindingConfidence;
}
