package com.yuan.daydayup.reading.compiler.model;

/**
 * 编译等级（rulemodel §4 health.grade / spec §4.6）。
 */
public enum CompileGrade {
    /** 全部规则语义均映射到原生能力 */
    FULL,
    /** 部分规则降级为受控 script，或裁剪非关键 pipeline，核心链路可用 */
    DEGRADED,
    /** 核心 pipeline 无法编译（如依赖 webView 浏览器内核），书源不可用 */
    REJECTED
}
