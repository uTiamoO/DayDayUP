package com.yuan.daydayup.reading.compiler.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个动作（search / detail / toc / content / explore）的规则集合（rulemodel §4 ActionRule）。
 */
@Data
public class ActionRule {

    /** 响应类型：html / json / xml */
    private String responseType;

    /** 请求构造规格（search/explore 有独立入口 URL 时才有；detail/toc/content 用上游产出的 URL） */
    private RequestSpec request;

    /** 列表定位规则（bookList / chapterList，列表类 action 才有） */
    private RuleChain list;

    /** 字段抽取规则：fieldName -> RuleChain */
    private Map<String, RuleChain> fields = new LinkedHashMap<>();

    public void putField(String name, RuleChain chain) {
        fields.put(name, chain);
    }
}
