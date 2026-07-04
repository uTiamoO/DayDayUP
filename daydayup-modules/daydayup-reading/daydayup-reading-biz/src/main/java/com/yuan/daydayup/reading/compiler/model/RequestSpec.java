package com.yuan.daydayup.reading.compiler.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个动作的请求构造规格（rulemodel §4 requestBuilder）。
 *
 * <p>来自 Legado 的 {@code searchUrl / exploreUrl}：URL 主体可含 {@code {{key}}/{{page}}} 模板，
 * 逗号后可跟一段选项 JSON（method/body/charset/headers）。headers 为动作级，
 * 与书源级 {@link RuleModel.Http#getHeaders()} 合并后生效（动作级优先）。</p>
 */
@Data
public class RequestSpec {

    /** URL 模板（相对 baseUrl 或绝对），含 {{}} 占位 */
    private String urlTemplate;

    /** HTTP 方法，默认 GET */
    private String method = "GET";

    /** 请求体模板（POST 时使用，可含 {{}} 占位） */
    private String body;

    /** 请求/关键词编码字符集（如 gbk），空 = UTF-8 */
    private String charset;

    /** 动作级请求头（覆盖书源级同名头） */
    private Map<String, String> headers = new LinkedHashMap<>();
}
