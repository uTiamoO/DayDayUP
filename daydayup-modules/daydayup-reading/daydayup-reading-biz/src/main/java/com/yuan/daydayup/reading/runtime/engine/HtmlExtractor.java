package com.yuan.daydayup.reading.runtime.engine;

import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleStep;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Set;

/**
 * HTML 响应抽取（jsoup + Legado 方言）。执行 engine=JSOUP 的 SELECTOR 步骤：
 * 类/标签/属性选择、索引 {@code .N}、{@code text.} 文本匹配、取值器 {@code @text/@html/@attr}、
 * 嵌套 {@code @标签}；以及 REPLACE、原生 POST_PROCESSOR。用于 responseType=html 的书源。
 */
public class HtmlExtractor {

    /** 文本取值器 */
    private static final Set<String> TEXT_ATTRS = Set.of("text", "textNodes", "ownText");
    /** 元素属性取值（而非嵌套标签选择） */
    private static final Set<String> ELEMENT_ATTRS = Set.of(
            "href", "src", "content", "value", "title", "alt", "srcset", "poster", "data-src", "data-original");

    /** 列表定位：返回列表项元素 */
    public Elements selectList(Element root, RuleChain listChain) {
        Elements result = new Elements();
        if (listChain == null) {
            return result;
        }
        for (RuleStep step : listChain.getSteps()) {
            if (step.getType() != RuleStep.StepType.SELECTOR) {
                continue;
            }
            Elements base = select(root, step.getSelector());
            base = applyIndex(base, step.getIndex());
            String attr = step.getAttribute();
            if (attr != null && isNestedSelector(attr)) {
                Elements nested = new Elements();
                for (Element e : base) {
                    nested.addAll(e.select(attr));
                }
                base = nested;
            }
            result.addAll(base);
        }
        return result;
    }

    /** 字段抽取：作用域 item 执行 chain 得到字符串 */
    public String extractField(Element item, RuleChain chain) {
        return extractField(item, chain, null);
    }

    /** 字段抽取（带脚本执行回调）：作用域 item 执行 chain 得到字符串 */
    public String extractField(Element item, RuleChain chain, ScriptExecutor js) {
        if (chain == null) {
            return null;
        }
        Element curEl = item;
        String value = null;
        for (RuleStep step : chain.getSteps()) {
            switch (step.getType()) {
                case SELECTOR -> {
                    if (step.getSelector() != null && !step.getSelector().isEmpty()) {
                        Elements sel = applyIndex(select(curEl, step.getSelector()), step.getIndex());
                        curEl = sel.first();
                    }
                    value = applyAttribute(curEl, step.getAttribute());
                }
                case REPLACE -> value = JsonExtractor.applyReplace(value, step);
                case POST_PROCESSOR -> value = NativePostProcessors.apply(step, value);
                case SCRIPT -> {
                    if (js == null) {
                        throw new UnsupportedOperationException("ScriptStep 需绑定 ScriptExecutor（GraalJS）");
                    }
                    if (value == null && curEl != null) {
                        value = curEl.text();
                    }
                    value = js.run(step.getScriptBody(), value);
                }
                default -> {
                    // COMBINATOR/VAR/TEMPLATE(html) 暂不在本子片处理
                }
            }
        }
        if (value == null && curEl != null) {
            value = curEl.text();
        }
        return value;
    }

    // ── 内部 ───────────────────────────────────────────────────────

    private Elements select(Element root, String selector) {
        if (root == null) {
            return new Elements();
        }
        if (selector == null || selector.isEmpty()) {
            Elements self = new Elements();
            self.add(root);
            return self;
        }
        try {
            return root.select(translate(selector));
        } catch (Exception e) {
            return new Elements();
        }
    }

    /** Legado 选择器方言 → jsoup 选择器 */
    private String translate(String selector) {
        String s = selector.replace('～', '~');   // 全角笔误宽容
        if (s.startsWith("text.")) {
            return ":containsOwn(" + s.substring(5) + ")";
        }
        return s;
    }

    private Elements applyIndex(Elements els, Integer index) {
        if (index == null || els.isEmpty()) {
            return els;
        }
        int i = index < 0 ? els.size() + index : index;
        Elements one = new Elements();
        if (i >= 0 && i < els.size()) {
            one.add(els.get(i));
        }
        return one;
    }

    private String applyAttribute(Element el, String attr) {
        if (el == null) {
            return null;
        }
        if (attr == null) {
            return el.text();
        }
        if (TEXT_ATTRS.contains(attr)) {
            return "ownText".equals(attr) ? el.ownText() : el.text();
        }
        if ("html".equals(attr)) {
            return el.html();
        }
        if ("all".equals(attr)) {
            return el.outerHtml();
        }
        if (isNestedSelector(attr)) {
            Element nested = el.select(attr).first();
            return nested != null ? nested.text() : null;
        }
        String v = el.attr(attr);
        return v.isEmpty() ? null : v;
    }

    /** 非取值器、非已知属性 → 视为嵌套标签选择器（如 .list.1@a 的 a） */
    private boolean isNestedSelector(String attr) {
        return !TEXT_ATTRS.contains(attr) && !ELEMENT_ATTRS.contains(attr)
                && !"html".equals(attr) && !"all".equals(attr)
                && attr.matches("[a-zA-Z][a-zA-Z0-9]*");
    }
}
