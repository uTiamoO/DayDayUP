package com.yuan.daydayup.reading.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 章节正文快照（content-repository，对应 spec 3.2.7 ChapterContentSnapshot）。
 *
 * <p>一次抓取+处理得到的正文，三层保留：{@code rawContent}（原始）/{@code normalizedContent}
 * （标准化）/{@code sanitizedContent}（净化）。按 {@code (chapterId, sourceId)} 幂等，
 * {@code contentHash} 判重与变更。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_chapter_content_snapshot")
public class ChapterContentSnapshot extends BaseEntity {

    /** 关联统一章节 id */
    private Long chapterId;

    /** 来源书源 id */
    private Long sourceId;

    /** 抓取所用编译产物 id（回放） */
    private Long compiledRuleId;

    /** 原始正文（来源抽取所得） */
    private String rawContent;

    /** 标准化正文（去标签/规整段落；可空，可由 raw 重放） */
    private String normalizedContent;

    /** 净化正文（切片 4 子片 2 写入） */
    private String sanitizedContent;

    /** raw 正文 SHA-256，判重与变更 */
    private String contentHash;

    /** 状态：raw_only / normalized / sanitized / empty */
    private String contentStatus;

    /** 净化 Pipeline 版本 */
    private String sanitizationPipelineVersion;

    /** 抓取时间 */
    private LocalDateTime fetchedAt;

    /** 处理（标准化/净化）时间 */
    private LocalDateTime processedAt;
}
