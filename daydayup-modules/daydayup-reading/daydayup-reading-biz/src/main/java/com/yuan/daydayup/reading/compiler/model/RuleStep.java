package com.yuan.daydayup.reading.compiler.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 一条规则字符串拆解出的单个步骤（rulemodel §4 Step 联合类型）。
 *
 * <p>为便于 JSON 序列化持久化，采用带 {@link #type} 判别的扁平结构，未用到的字段为空。</p>
 */
@Data
public class RuleStep {

    public enum StepType {
        /** 选择器抽取 */
        SELECTOR,
        /** 正则替换（##pat / ##pat##rep / ###firstOnly） */
        REPLACE,
        /** 组合器（&& || %%） */
        COMBINATOR,
        /** 受控脚本（@js: / <js>） */
        SCRIPT,
        /** 原生后处理（脚本收敛而来：aesDecrypt/base64Decode/... rulemodel §5.1） */
        POST_PROCESSOR,
        /** 变量存入（@put） */
        VAR_PUT,
        /** 变量取出（@get） */
        VAR_GET
    }

    private StepType type;

    // ── SELECTOR ──
    private ExtractEngine engine;
    private String selector;
    private String attribute;
    private Integer index;

    // ── REPLACE ──
    private String pattern;
    private String replacement;
    private Boolean firstOnly;

    // ── COMBINATOR ──
    private String op;               // concat | or | merge
    private List<RuleChain> branches;

    // ── SCRIPT ──
    private String scriptBody;

    // ── POST_PROCESSOR（原生） ──
    private String postProcessor;    // aesDecrypt | base64Decode | hexDecode | md5 | absoluteUrl ...
    private Map<String, String> params;

    // ── VAR ──
    private String varKey;

    public static RuleStep selector(ExtractEngine engine, String selector, String attribute, Integer index) {
        RuleStep s = new RuleStep();
        s.type = StepType.SELECTOR;
        s.engine = engine;
        s.selector = selector;
        s.attribute = attribute;
        s.index = index;
        return s;
    }

    public static RuleStep replace(String pattern, String replacement, boolean firstOnly) {
        RuleStep s = new RuleStep();
        s.type = StepType.REPLACE;
        s.pattern = pattern;
        s.replacement = replacement;
        s.firstOnly = firstOnly;
        return s;
    }

    public static RuleStep script(String body) {
        RuleStep s = new RuleStep();
        s.type = StepType.SCRIPT;
        s.scriptBody = body;
        return s;
    }

    public static RuleStep postProcessor(String name, Map<String, String> params) {
        RuleStep s = new RuleStep();
        s.type = StepType.POST_PROCESSOR;
        s.postProcessor = name;
        s.params = params;
        return s;
    }

    public static RuleStep template(String template) {
        return selector(ExtractEngine.TEMPLATE, template, null, null);
    }
}
