package com.yuan.daydayup.reading.compiler;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yuan.daydayup.reading.compiler.model.ActionRule;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RequestSpec;
import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.model.RuleStep;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 规则编译器（rulemodel §2 拆解流程）：把一份 Legado 书源 JSON 编译为内部 {@link RuleModel}，
 * 并同步产出编译体检（rulemodel §5.2）。忠实机械翻译，不重推语义。
 */
@Component
public class RuleCompiler {

    /** 编译器版本，规则/算法变更时递增，供重编译判断 */
    public static final String COMPILER_VERSION = "1.1";

    /** Legado action -> (源规则组字段, 列表字段名) */
    private static final Map<String, String[]> ACTION_MAP = Map.of(
            "search", new String[]{"ruleSearch", "bookList"},
            "detail", new String[]{"ruleBookInfo", null},
            "toc", new String[]{"ruleToc", "chapterList"},
            "content", new String[]{"ruleContent", null},
            "explore", new String[]{"ruleExplore", "bookList"}
    );

    /** Legado action -> 独立入口 URL 字段（detail/toc/content 的 URL 由上游抽取产出，无独立入口） */
    private static final Map<String, String> ACTION_URL_MAP = Map.of(
            "search", "searchUrl",
            "explore", "exploreUrl"
    );

    /** 宽松 JSON：Legado 的 header / URL 选项常用单引号、裸键名、换行 */
    private static final ObjectMapper LENIENT = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    private final RuleStringParser parser;

    public RuleCompiler(RuleStringParser parser) {
        this.parser = parser;
    }

    public RuleModel compile(JsonNode legado) {
        RuleModel model = new RuleModel();
        buildIdentity(model, legado);
        buildHttp(model, legado);

        for (Map.Entry<String, String[]> e : ACTION_MAP.entrySet()) {
            JsonNode group = legado.get(e.getValue()[0]);
            if (group != null && group.isObject() && !group.isEmpty()) {
                ActionRule action = buildAction(group, e.getValue()[1]);
                buildRequest(action, e.getKey(), legado, model.getHealth());
                model.putAction(e.getKey(), action);
            }
        }

        healthCheck(model, legado);
        return model;
    }

    private void buildIdentity(RuleModel model, JsonNode legado) {
        RuleModel.Identity id = model.getIdentity();
        id.setName(text(legado, "bookSourceName"));
        String url = stripComment(text(legado, "bookSourceUrl"));
        id.setKey(url);
        id.setBaseUrl(url);
        id.setGroup(text(legado, "bookSourceGroup"));
        id.setBookType(switch (legado.path("bookSourceType").asInt(0)) {
            case 1 -> "audio";
            case 2 -> "comic";
            default -> "text";
        });
        id.setEnabled(legado.path("enabled").asBoolean(true));
    }

    private ActionRule buildAction(JsonNode group, String listField) {
        ActionRule action = new ActionRule();
        String inferSample = null;

        if (listField != null && group.hasNonNull(listField)) {
            String raw = group.get(listField).asText();
            action.setList(parser.parse(raw));
            inferSample = raw;
        }
        var it = group.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> f = it.next();
            if (f.getKey().equals(listField) || f.getValue() == null || f.getValue().isNull()) {
                continue;
            }
            String raw = f.getValue().asText();
            action.putField(f.getKey(), parser.parse(raw));
            if (inferSample == null) {
                inferSample = raw;
            }
        }
        action.setResponseType(inferResponseType(inferSample));
        return action;
    }

    private String inferResponseType(String sample) {
        if (sample == null) {
            return "html";
        }
        String s = sample.trim();
        return (s.startsWith("$.") || s.startsWith("$[") || s.startsWith("@json:")) ? "json" : "html";
    }

    /** 书源级 HTTP 配置：header（宽松 JSON）+ concurrentRate */
    private void buildHttp(RuleModel model, JsonNode legado) {
        RuleModel.Http http = model.getHttp();
        http.setConcurrentRate(text(legado, "concurrentRate"));

        String header = text(legado, "header");
        if (header == null || header.isBlank()) {
            return;
        }
        String trimmed = header.strip();
        if (trimmed.startsWith("@js:") || trimmed.startsWith("<js>")) {
            model.getHealth().getScriptDeps().add("header: " + abbreviate(trimmed));
            return;
        }
        try {
            JsonNode node = LENIENT.readTree(trimmed);
            node.fields().forEachRemaining(f -> http.getHeaders().put(f.getKey(), f.getValue().asText()));
        } catch (Exception e) {
            model.getHealth().getWarnings().add("header 解析失败，已忽略: " + abbreviate(trimmed));
        }
    }

    /** 动作级请求规格：searchUrl / exploreUrl → RequestSpec（URL 主体 + 逗号后选项 JSON） */
    private void buildRequest(ActionRule action, String actionName, JsonNode legado, RuleModel.Health health) {
        String urlField = ACTION_URL_MAP.get(actionName);
        if (urlField == null) {
            return;
        }
        String raw = text(legado, urlField);
        if (raw == null || raw.isBlank()) {
            return;
        }
        raw = raw.strip();
        if (raw.contains("@js:") || raw.contains("<js>")) {
            health.getScriptDeps().add(urlField + ": " + abbreviate(raw));
            return;
        }

        RequestSpec spec = new RequestSpec();
        int optIdx = raw.indexOf(",{");
        if (optIdx < 0) {
            spec.setUrlTemplate(raw);
        } else {
            spec.setUrlTemplate(raw.substring(0, optIdx).strip());
            try {
                JsonNode opts = LENIENT.readTree(raw.substring(optIdx + 1));
                if (opts.hasNonNull("method")) {
                    spec.setMethod(opts.get("method").asText().toUpperCase());
                }
                if (opts.hasNonNull("body")) {
                    spec.setBody(opts.get("body").asText());
                }
                if (opts.hasNonNull("charset")) {
                    spec.setCharset(opts.get("charset").asText());
                }
                JsonNode headers = opts.get("headers");
                if (headers != null && headers.isObject()) {
                    headers.fields().forEachRemaining(f ->
                            spec.getHeaders().put(f.getKey(), f.getValue().asText()));
                }
            } catch (Exception e) {
                health.getWarnings().add(urlField + " 选项解析失败，仅保留 URL: " + abbreviate(raw));
            }
        }
        action.setRequest(spec);
    }

    // ── 编译体检（rulemodel §5.2） ──────────────────────────────────

    private void healthCheck(RuleModel model, JsonNode legado) {
        RuleModel.Health health = model.getHealth();
        String rawJson = legado.toString();

        // webView / 浏览器内核依赖 → rejected
        if (rawJson.contains("webView") || rawJson.contains("startBrowser")) {
            health.getWebviewDeps().add("检测到 webView / startBrowser 依赖，需浏览器内核");
        }
        // 全角笔误 → 宽容解析 + 警告
        if (rawJson.indexOf('～') >= 0) {
            health.getWarnings().add("检测到全角字符（如 ～），已宽容解析");
        }

        // 遍历所有 RuleChain 收集 script 依赖
        for (ActionRule action : model.getActions().values()) {
            collectScripts(action.getList(), health);
            for (RuleChain chain : action.getFields().values()) {
                collectScripts(chain, health);
            }
        }

        if (!health.getWebviewDeps().isEmpty()) {
            health.setGrade(CompileGrade.REJECTED);
        } else if (!health.getScriptDeps().isEmpty()) {
            health.setGrade(CompileGrade.DEGRADED);
        } else {
            health.setGrade(CompileGrade.FULL);
        }
    }

    private void collectScripts(RuleChain chain, RuleModel.Health health) {
        if (chain == null) {
            return;
        }
        for (RuleStep step : chain.getSteps()) {
            if (step.getType() == RuleStep.StepType.SCRIPT) {
                health.getScriptDeps().add(abbreviate(step.getScriptBody()));
            } else if (step.getType() == RuleStep.StepType.COMBINATOR && step.getBranches() != null) {
                for (RuleChain b : step.getBranches()) {
                    collectScripts(b, health);
                }
            }
        }
    }

    // ── 助手 ───────────────────────────────────────────────────────

    private static String stripComment(String url) {
        if (url == null) {
            return null;
        }
        int i = url.indexOf("##");
        return i >= 0 ? url.substring(0, i) : url;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() > 120 ? t.substring(0, 120) + "…" : t;
    }

    public List<String> supportedActions() {
        return List.copyOf(ACTION_MAP.keySet());
    }
}
