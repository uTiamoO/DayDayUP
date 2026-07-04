package com.yuan.daydayup.reading.runtime.engine;

import com.yuan.daydayup.reading.compiler.model.ActionRule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽取执行引擎（parse-runtime 的响应抽取部分，离线）。
 *
 * <p>给定一个动作规则 {@link ActionRule} 和响应文本，按 responseType 分派 HTML/JSON 抽取器，
 * 列表定位后逐项抽取字段，产出记录列表。ScriptStep 经可选的 {@link ScriptExecutor}
 * 回调执行（在线链路绑定 GraalJS；离线/未绑定时遇 ScriptStep 失败）。</p>
 */
@Component
public class RuleExecutor {

    private final HtmlExtractor htmlExtractor = new HtmlExtractor();
    private final JsonExtractor jsonExtractor = new JsonExtractor();

    /**
     * 执行列表类动作（search/toc）：定位列表并逐项抽取字段。
     *
     * @return 每个列表项一个 {字段名 -> 值} 的有序 Map
     */
    public List<Map<String, String>> extractList(String responseType, String response, ActionRule action) {
        return extractList(responseType, response, action, null);
    }

    /** 执行列表类动作（带 ScriptStep 执行回调，供在线链路传入 GraalJS） */
    public List<Map<String, String>> extractList(String responseType, String response, ActionRule action,
                                                 ScriptExecutor js) {
        if (action == null || response == null) {
            return List.of();
        }
        return "json".equalsIgnoreCase(responseType)
                ? extractListJson(response, action, js)
                : extractListHtml(response, action, js);
    }

    /**
     * 执行单对象类动作（detail/content）：整个响应为一个作用域抽取字段。
     */
    public Map<String, String> extractObject(String responseType, String response, ActionRule action) {
        return extractObject(responseType, response, action, null);
    }

    /** 执行单对象类动作（带 ScriptStep 执行回调） */
    public Map<String, String> extractObject(String responseType, String response, ActionRule action,
                                             ScriptExecutor js) {
        if (action == null || response == null) {
            return Map.of();
        }
        Map<String, String> record = new LinkedHashMap<>();
        if ("json".equalsIgnoreCase(responseType)) {
            Object doc = jsonExtractor.parse(response);
            action.getFields().forEach((name, chain) -> record.put(name, jsonExtractor.extractField(doc, chain, js)));
        } else {
            Document doc = Jsoup.parse(response);
            action.getFields().forEach((name, chain) -> record.put(name, htmlExtractor.extractField(doc, chain, js)));
        }
        return record;
    }

    private List<Map<String, String>> extractListJson(String response, ActionRule action, ScriptExecutor js) {
        Object doc = jsonExtractor.parse(response);
        List<Object> items = jsonExtractor.selectList(doc, action.getList());
        List<Map<String, String>> records = new ArrayList<>(items.size());
        for (Object item : items) {
            Map<String, String> record = new LinkedHashMap<>();
            action.getFields().forEach((name, chain) -> record.put(name, jsonExtractor.extractField(item, chain, js)));
            records.add(record);
        }
        return records;
    }

    private List<Map<String, String>> extractListHtml(String response, ActionRule action, ScriptExecutor js) {
        Document doc = Jsoup.parse(response);
        Elements items = htmlExtractor.selectList(doc, action.getList());
        List<Map<String, String>> records = new ArrayList<>(items.size());
        for (Element item : items) {
            Map<String, String> record = new LinkedHashMap<>();
            action.getFields().forEach((name, chain) -> record.put(name, htmlExtractor.extractField(item, chain, js)));
            records.add(record);
        }
        return records;
    }
}
