package com.yuan.daydayup.reading.runtime.engine;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON 响应抽取（jayway JSONPath）。执行 RuleModel 中 engine=JSONPATH 的 SELECTOR 步骤、
 * TEMPLATE、原生 POST_PROCESSOR、REPLACE。用于 responseType=json 的书源。
 */
public class JsonExtractor {

    /** 缺失路径返回 null，不抛异常 */
    private static final Configuration CONF = Configuration.builder()
            .options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    /** 解析文档 */
    public Object parse(String response) {
        return CONF.jsonProvider().parse(response);
    }

    /** 列表定位：返回每个列表项（供字段抽取的作用域） */
    public List<Object> selectList(Object doc, RuleChain listChain) {
        List<Object> result = new ArrayList<>();
        if (listChain == null) {
            return result;
        }
        for (RuleStep step : listChain.getSteps()) {
            if (step.getType() == RuleStep.StepType.SELECTOR && step.getSelector() != null) {
                Object read = readPath(doc, step.getSelector());
                if (read instanceof List<?> list) {
                    result.addAll(list);
                } else if (read != null) {
                    result.add(read);
                }
            }
        }
        return result;
    }

    /** 字段抽取：作用域 scope（列表项或整文档）执行 chain 得到字符串 */
    public String extractField(Object scope, RuleChain chain) {
        return extractField(scope, chain, null);
    }

    /** 字段抽取（带脚本执行回调）：作用域 scope 执行 chain 得到字符串 */
    public String extractField(Object scope, RuleChain chain, ScriptExecutor js) {
        if (chain == null) {
            return null;
        }
        String value = null;
        for (RuleStep step : chain.getSteps()) {
            switch (step.getType()) {
                case SELECTOR -> {
                    if (step.getEngine() == com.yuan.daydayup.reading.compiler.model.ExtractEngine.TEMPLATE) {
                        value = TemplateRenderer.render(step.getSelector(), Map.of(),
                                expr -> stringify(readPath(scope, expr)));
                    } else {
                        value = stringify(readPath(scope, step.getSelector()));
                    }
                }
                case REPLACE -> value = applyReplace(value, step);
                case POST_PROCESSOR -> value = NativePostProcessors.apply(step, value);
                case SCRIPT -> {
                    if (js == null) {
                        throw new UnsupportedOperationException("ScriptStep 需绑定 ScriptExecutor（GraalJS）");
                    }
                    value = js.run(step.getScriptBody(), value);
                }
                default -> {
                    // COMBINATOR/VAR 暂不在 JSON 抽取子片处理
                }
            }
        }
        return value;
    }

    private Object readPath(Object scope, String path) {
        if (path == null || !path.startsWith("$")) {
            return path;
        }
        try {
            return JsonPath.using(CONF).parse(scope).read(path);
        } catch (Exception e) {
            return null;
        }
    }

    static String applyReplace(String value, RuleStep step) {
        if (value == null || step.getPattern() == null) {
            return value;
        }
        String replacement = step.getReplacement() == null ? "" : step.getReplacement();
        return Boolean.TRUE.equals(step.getFirstOnly())
                ? value.replaceFirst(step.getPattern(), replacement)
                : value.replaceAll(step.getPattern(), replacement);
    }

    static String stringify(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof List<?> list) {
            return list.isEmpty() ? null : stringify(list.get(0));
        }
        return String.valueOf(v);
    }
}
