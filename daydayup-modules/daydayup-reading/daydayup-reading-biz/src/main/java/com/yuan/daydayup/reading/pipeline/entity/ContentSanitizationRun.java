package com.yuan.daydayup.reading.pipeline.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 净化运行记录（content-pipeline，对应 spec 3.2.8 ContentSanitizationRun）。
 *
 * <p>一次净化 Pipeline 运行的可解释归档：pipeline 版本、执行 trace、删除片段/替换词摘要、
 * 质量评分与最终等级（archive-run 阶段落库）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_content_sanitization_run")
public class ContentSanitizationRun extends BaseEntity {

    /** 关联正文快照 id */
    private Long contentSnapshotId;

    /** 净化 Pipeline 版本 */
    private String pipelineDefinitionVersion;

    /** 各段执行 trace（JSON） */
    private String pipelineExecutionTrace;

    /** 删除片段摘要（JSON 数组） */
    private String removedSegments;

    /** 替换词摘要（JSON 数组） */
    private String replacedTerms;

    /** 质量评分 0-100 */
    private Integer qualityScore;

    /** 结果等级：accepted / degraded / rejected / failed */
    private String runStatus;

    /** 运行时间 */
    private LocalDateTime runAt;
}
