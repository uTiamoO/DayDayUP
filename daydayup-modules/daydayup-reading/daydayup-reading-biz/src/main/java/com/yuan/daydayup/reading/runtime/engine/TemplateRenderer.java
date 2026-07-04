package com.yuan.daydayup.reading.runtime.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染（rulemodel §3.4）：解析 {@code {{...}}} 占位。
 *
 * <ul>
 *   <li>{@code {{key}} / {{page}} / {{baseUrl}}} 等 → 变量表</li>
 *   <li>{@code {{$.x}}}（以 $ 开头）→ 上下文抽取解析器（如 JSONPath）</li>
 * </ul>
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(.+?)}}");

    private TemplateRenderer() {
    }

    /**
     * @param template     含 {{}} 的模板
     * @param variables    内置变量（key/page/baseUrl…）
     * @param exprResolver 上下文抽取解析器（$ 开头表达式 → 值），可为 null
     */
    public static String render(String template, Map<String, String> variables, Function<String, String> exprResolver) {
        if (template == null || !template.contains("{{")) {
            return template;
        }
        Map<String, String> vars = variables == null ? Map.of() : variables;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String expr = m.group(1).trim();
            String value;
            if (expr.startsWith("$") && exprResolver != null) {
                value = exprResolver.apply(expr);
            } else {
                value = vars.get(expr);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Map<String, String> vars(String keyword, int page) {
        Map<String, String> vars = new LinkedHashMap<>();
        if (keyword != null) {
            vars.put("key", keyword);
        }
        vars.put("page", String.valueOf(page));
        return vars;
    }
}
