package com.yuan.daydayup.reading.pipeline.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 净化 Pipeline 运行结果（可解释）。
 */
@Data
public class SanitizationResult {

    /** 净化后正文 */
    private String sanitizedContent;

    /** 质量评分 0-100 */
    private int qualityScore;

    /** 结果等级：accepted / degraded / rejected */
    private String runStatus;

    /** 删除片段摘要（原文行） */
    private List<String> removedSegments = new ArrayList<>();

    /** 替换词摘要（"词→替换" 形式） */
    private List<String> replacedTerms = new ArrayList<>();

    /** 各段执行 trace（阶段名: 说明） */
    private List<String> trace = new ArrayList<>();
}
