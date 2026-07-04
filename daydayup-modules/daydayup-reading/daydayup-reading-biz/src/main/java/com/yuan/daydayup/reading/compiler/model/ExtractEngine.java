package com.yuan.daydayup.reading.compiler.model;

/**
 * 抽取引擎类型（对应 Legado 规则的引擎前缀，rulemodel §3.1）。
 */
public enum ExtractEngine {
    /** Legado jsoup 方言（默认，无前缀） */
    JSOUP,
    /** 标准 CSS（@css:） */
    CSS,
    /** JSONPath（@json: 或裸 $.） */
    JSONPATH,
    /** XPath（@XPath: 或裸 //） */
    XPATH,
    /** 正则 */
    REGEX,
    /** 模板（含 {{}} 变量 / 内嵌抽取的混排文本） */
    TEMPLATE
}
