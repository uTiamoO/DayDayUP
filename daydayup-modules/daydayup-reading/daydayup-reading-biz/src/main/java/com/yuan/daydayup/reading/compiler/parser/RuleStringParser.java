package com.yuan.daydayup.reading.compiler.parser;

import com.yuan.daydayup.reading.compiler.model.ExtractEngine;
import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则字符串拆解器（rulemodel §7）：把一条 Legado 规则字符串忠实拆解为 {@link RuleChain}。
 *
 * <p>分层 tokenizer（优先级从外到内）：</p>
 * <ol>
 *   <li>整段 {@code <js>}/{@code @js:} 脚本；尾部 {@code @js:} 后处理（尝试原生收敛）</li>
 *   <li>顶层组合器 {@code && || %%}（括号感知，跳过 {{}}、[]）</li>
 *   <li>模板 {{}} 混排</li>
 *   <li>引擎前缀识别 + jsoup 方言（{@code .名称.索引@属性}、裸属性）</li>
 *   <li>{@code ##} 正则替换</li>
 * </ol>
 *
 * <p>宽容解析：不因全角笔误等拒绝，保留原文，警告由编译器统一产出。无 Spring 依赖，可直接 new。</p>
 */
@Component
public class RuleStringParser {

    private static final Set<String> BARE_ATTRIBUTES = Set.of(
            "text", "textNodes", "ownText", "html", "all", "href", "src", "content", "value");

    private static final Pattern TRAILING_INDEX = Pattern.compile("\\.(-?\\d+)$");

    public RuleChain parse(String raw) {
        RuleChain chain = new RuleChain(raw);
        if (raw == null || raw.trim().isEmpty()) {
            return chain;
        }
        String rule = raw.trim();

        // 1a. 整段脚本
        if (rule.startsWith("<js>") && rule.contains("</js>")) {
            appendScriptOrNative(chain, rule.substring(4, rule.lastIndexOf("</js>")));
            return chain;
        }
        if (rule.startsWith("@js:")) {
            appendScriptOrNative(chain, rule.substring(4));
            return chain;
        }

        // 1b. 尾部 @js: 后处理
        String script = null;
        int jsIdx = indexOfTopLevel(rule, "@js:");
        if (jsIdx >= 0) {
            script = rule.substring(jsIdx + 4);
            rule = rule.substring(0, jsIdx).trim();
        }

        // 2. 顶层组合器
        for (String op : new String[]{"||", "&&", "%%"}) {
            List<String> segs = splitTopLevel(rule, op);
            if (segs.size() > 1) {
                RuleStep comb = new RuleStep();
                comb.setType(RuleStep.StepType.COMBINATOR);
                comb.setOp(switch (op) {
                    case "||" -> "or";
                    case "&&" -> "concat";
                    default -> "merge";
                });
                List<RuleChain> branches = new ArrayList<>();
                for (String seg : segs) {
                    branches.add(parse(seg.trim()));
                }
                comb.setBranches(branches);
                chain.add(comb);
                if (script != null) {
                    appendScriptOrNative(chain, script);
                }
                return chain;
            }
        }

        // 3. 模板
        if (rule.contains("{{") && !hasEnginePrefix(rule)) {
            chain.add(RuleStep.template(rule));
        } else {
            // 4 + 5. 选择器（含 ## 替换）
            parseSelectorWithReplace(chain, rule);
        }

        if (script != null) {
            appendScriptOrNative(chain, script);
        }
        return chain;
    }

    // ── 选择器 + ## 替换 ───────────────────────────────────────────────

    private void parseSelectorWithReplace(RuleChain chain, String selPart) {
        String s = selPart.trim();
        if (s.isEmpty()) {
            return;
        }
        int idx = s.indexOf("##");
        if (idx < 0) {
            addSelectorStep(chain, s);
            return;
        }
        String selector = s.substring(0, idx);
        String rest = s.substring(idx + 2);
        boolean firstOnly = false;
        if (rest.endsWith("###")) {
            firstOnly = true;
            rest = rest.substring(0, rest.length() - 3);
        }
        String pattern;
        String replacement;
        int r = rest.indexOf("##");
        if (r < 0) {
            pattern = rest;
            replacement = "";
        } else {
            pattern = rest.substring(0, r);
            replacement = rest.substring(r + 2);
        }
        addSelectorStep(chain, selector);
        chain.add(RuleStep.replace(pattern, replacement, firstOnly));
    }

    private void addSelectorStep(RuleChain chain, String selRaw) {
        String s = selRaw.trim();
        if (s.isEmpty()) {
            return;
        }
        ExtractEngine engine;
        String body = s;
        if (s.startsWith("@css:") || s.startsWith("@CSS:")) {
            engine = ExtractEngine.CSS;
            body = s.substring(5);
        } else if (s.startsWith("@json:")) {
            engine = ExtractEngine.JSONPATH;
            body = s.substring(6);
        } else if (s.startsWith("$.") || s.startsWith("$[")) {
            engine = ExtractEngine.JSONPATH;
        } else if (s.startsWith("@XPath:") || s.startsWith("@xpath:")) {
            engine = ExtractEngine.XPATH;
            body = s.substring(7);
        } else if (s.startsWith("//")) {
            engine = ExtractEngine.XPATH;
        } else if (s.startsWith("@regex:")) {
            engine = ExtractEngine.REGEX;
            body = s.substring(7);
        } else {
            engine = ExtractEngine.JSOUP;
        }

        if (engine == ExtractEngine.JSOUP) {
            addJsoupSelector(chain, body);
        } else {
            chain.add(RuleStep.selector(engine, body, null, null));
        }
    }

    /** jsoup 方言：拆出尾部 {@code @属性}、索引 {@code .N}、裸属性 */
    private void addJsoupSelector(RuleChain chain, String body) {
        String attribute = null;
        String selector = body;
        int at = lastTopLevelAt(body);
        if (at >= 0) {
            attribute = body.substring(at + 1);
            selector = body.substring(0, at);
        } else if (BARE_ATTRIBUTES.contains(body.trim())) {
            attribute = body.trim();
            selector = "";
        }

        Integer index = null;
        Matcher m = TRAILING_INDEX.matcher(selector);
        if (m.find()) {
            index = Integer.parseInt(m.group(1));
            selector = selector.substring(0, m.start());
        }
        chain.add(RuleStep.selector(ExtractEngine.JSOUP, selector, attribute, index));
    }

    private void appendScriptOrNative(RuleChain chain, String script) {
        NativeConverter.tryConvert(script)
                .ifPresentOrElse(chain::add, () -> chain.add(RuleStep.script(script.trim())));
    }

    // ── 括号感知扫描助手 ───────────────────────────────────────────────

    /** 顶层查找子串（跳过 [] () {} 内部），返回首个匹配下标或 -1 */
    private static int indexOfTopLevel(String s, String needle) {
        int depth = 0;
        for (int i = 0; i <= s.length() - needle.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '(' || c == '{') {
                depth++;
            } else if (c == ']' || c == ')' || c == '}') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && s.startsWith(needle, i)) {
                return i;
            }
        }
        return -1;
    }

    /** 顶层最后一个 {@code @}（跳过 [] () {} 内部），返回下标或 -1 */
    private static int lastTopLevelAt(String s) {
        int depth = 0;
        int last = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '(' || c == '{') {
                depth++;
            } else if (c == ']' || c == ')' || c == '}') {
                if (depth > 0) {
                    depth--;
                }
            } else if (c == '@' && depth == 0) {
                last = i;
            }
        }
        return last;
    }

    /** 按顶层二字符操作符切分（跳过 [] () {} 内部） */
    private static List<String> splitTopLevel(String s, String op) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '(' || c == '{') {
                depth++;
            } else if (c == ']' || c == ')' || c == '}') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && s.startsWith(op, i)) {
                parts.add(s.substring(start, i));
                i += op.length() - 1;
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return parts;
    }

    private static boolean hasEnginePrefix(String s) {
        return s.startsWith("@css:") || s.startsWith("@CSS:") || s.startsWith("@json:")
                || s.startsWith("@XPath:") || s.startsWith("@xpath:") || s.startsWith("$.")
                || s.startsWith("//") || s.startsWith("@regex:");
    }
}
