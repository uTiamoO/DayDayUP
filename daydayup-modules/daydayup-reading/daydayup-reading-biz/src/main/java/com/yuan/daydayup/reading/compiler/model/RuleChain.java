package com.yuan.daydayup.reading.compiler.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条规则字符串的拆解结果（rulemodel §4 RuleChain）。
 *
 * <p>{@link #raw} 保留原始规则供诊断/回放；{@link #steps} 为顺序执行的步骤。</p>
 */
@Data
public class RuleChain {

    /** 原始规则字符串 */
    private String raw;

    /** 顺序步骤 */
    private List<RuleStep> steps = new ArrayList<>();

    public RuleChain() {
    }

    public RuleChain(String raw) {
        this.raw = raw;
    }

    public RuleChain add(RuleStep step) {
        this.steps.add(step);
        return this;
    }

    public boolean hasScriptStep() {
        return steps.stream().anyMatch(s -> s.getType() == RuleStep.StepType.SCRIPT);
    }
}
