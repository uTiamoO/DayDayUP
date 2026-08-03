package com.yuan.daydayup.reading.compiler;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yuan.daydayup.reading.compiler.model.ActionRule;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RequestSpec;
import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.model.RuleStep;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    /** 项目自有书源格式 action -> source block 字段 */
    private static final Map<String, String> NATIVE_ACTION_MAP = Map.of(
            "search", "searchBook",
            "detail", "bookDetail",
            "toc", "chapterList",
            "content", "chapterContent"
    );

    /** 项目自有书源格式 action -> 列表字段 */
    private static final Map<String, String> NATIVE_LIST_FIELD_MAP = Map.of(
            "search", "list",
            "toc", "list"
    );

    /** 项目自有书源格式字段名 -> RuleModel 字段名 */
    private static final Map<String, String> NATIVE_FIELD_NAME_MAP = Map.ofEntries(
            Map.entry("bookName", "name"),
            Map.entry("title", "name"),
            Map.entry("author", "author"),
            Map.entry("desc", "intro"),
            Map.entry("cover", "cover"),
            Map.entry("cat", "kind"),
            Map.entry("status", "status"),
            Map.entry("lastChapterTitle", "latestChapter"),
            Map.entry("detailUrl", "bookUrl"),
            Map.entry("url", "chapterUrl"),
            Map.entry("content", "content")
    );

    /** 项目自有格式中不属于抽取规则的配置字段 */
    private static final List<String> NATIVE_ACTION_CONFIG_FIELDS = List.of(
            "actionID", "parserID", "host", "validConfig", "responseFormatType", "requestInfo", "moreKeys"
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

    public RuleModel compile(JsonNode source) {
        JsonNode legado = normalizeSource(source);
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

    private JsonNode normalizeSource(JsonNode source) {
        if (!isNativeFormat(source)) {
            return source;
        }
        ObjectNode legado = LENIENT.createObjectNode();
        legado.put("bookSourceName", textOrDefault(source, "sourceName", text(source, "sourceUrl")));
        legado.put("bookSourceUrl", text(source, "sourceUrl"));
        legado.put("bookSourceGroup", text(source, "sourceName"));
        legado.put("bookSourceType", nativeBookType(source.path("sourceType").asText("text")));
        legado.put("enabled", source.path("enable").asInt(1) == 1);
        legado.put("weight", source.path("weight").asText("0"));
        ObjectNode header = LENIENT.createObjectNode();
        JsonNode httpHeaders = source.get("httpHeaders");
        if (httpHeaders != null && httpHeaders.isObject()) {
            httpHeaders.fields().forEachRemaining(f -> header.put(f.getKey(), sanitizeHeader(f.getKey(), f.getValue().asText())));
        }
        legado.put("header", header.toString());

        for (Map.Entry<String, String> entry : NATIVE_ACTION_MAP.entrySet()) {
            JsonNode nativeAction = source.get(entry.getValue());
            if (nativeAction == null || !nativeAction.isObject()) {
                continue;
            }
            ObjectNode action = LENIENT.createObjectNode();
            String listField = NATIVE_LIST_FIELD_MAP.get(entry.getKey());
            if (listField != null && nativeAction.hasNonNull(listField)) {
                String legadoListField = "toc".equals(entry.getKey()) ? "chapterList" : "bookList";
                action.put(legadoListField, nativeAction.get(listField).asText());
            }
            nativeAction.fields().forEachRemaining(f -> {
                if (NATIVE_ACTION_CONFIG_FIELDS.contains(f.getKey()) || f.getKey().equals(listField)) {
                    return;
                }
                String targetName = NATIVE_FIELD_NAME_MAP.get(f.getKey());
                if (targetName != null && f.getValue() != null && !f.getValue().isNull()) {
                    action.put(targetName, f.getValue().asText());
                }
            });
            legado.set(ACTION_MAP.get(entry.getKey())[0], action);
            RequestSpec request = nativeRequest(nativeAction, entry.getKey(), source);
            if (request != null && StringUtils.hasText(request.getUrlTemplate())) {
                action.set("__request", LENIENT.valueToTree(request));
            }
        }
        return legado;
    }

    private boolean isNativeFormat(JsonNode source) {
        return source != null && source.has("sourceUrl") && (source.has("searchBook") || source.has("bookDetail"));
    }

    private static int nativeBookType(String sourceType) {
        if ("audio".equalsIgnoreCase(sourceType)) {
            return 1;
        }
        if ("comic".equalsIgnoreCase(sourceType)) {
            return 2;
        }
        return 0;
    }

    private static String textOrDefault(JsonNode node, String field, String defaultValue) {
        String value = text(node, field);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static String sanitizeHeader(String key, String value) {
        String lower = key == null ? "" : key.toLowerCase();
        if (lower.contains("authorization") || lower.contains("cookie") || lower.contains("token")
                || lower.contains("device")) {
            return "<redacted>";
        }
        return value;
    }

    private RequestSpec nativeRequest(JsonNode nativeAction, String actionName, JsonNode source) {
        RequestSpec spec = new RequestSpec();
        String host = textOrDefault(nativeAction, "host", text(source, "sourceUrl"));
        String requestInfo = text(nativeAction, "requestInfo");
        if (!StringUtils.hasText(requestInfo)) {
            return spec;
        }
        if ("search".equals(actionName)) {
            spec.setUrlTemplate((StringUtils.hasText(host) ? host : "") + "/search?keyword={{key}}&type=2&page={{page}}");
        } else if ("detail".equals(actionName)) {
            spec.setUrlTemplate(resolveNativeTemplate(host, requestInfo, "/novel/{{detailUrl}}?isSearch=0"));
        } else if ("toc".equals(actionName)) {
            spec.setUrlTemplate(resolveNativeTemplate(host, requestInfo, "/novel/{{detailUrl}}/chapters"));
        } else if ("content".equals(actionName)) {
            spec.setUrlTemplate("{{chapterUrl}}");
        }
        JsonNode httpHeaders = source.get("httpHeaders");
        if (httpHeaders != null && httpHeaders.isObject()) {
            httpHeaders.fields().forEachRemaining(f -> spec.getHeaders().put(f.getKey(), sanitizeHeader(f.getKey(), f.getValue().asText())));
        }
        return spec;
    }

    private static String resolveNativeTemplate(String host, String requestInfo, String fallbackPath) {
        String base = StringUtils.hasText(host) ? host : "";
        String template = fallbackPath;
        String marker = "url: '";
        int start = requestInfo.indexOf(marker);
        if (start >= 0) {
            int valueStart = start + marker.length();
            int valueEnd = requestInfo.indexOf("'", valueStart);
            if (valueEnd > valueStart) {
                template = requestInfo.substring(valueStart, valueEnd)
                        .replace("' + encodeURIComponent(params.keyWord) + '", "{{key}}")
                        .replace("' + (params.pageIndex || 1)", "{{page}}")
                        .replace("' + nid + '", "{{detailUrl}}")
                        .replace("' + nid", "{{detailUrl}}");
            }
        }
        if (template.startsWith("http://") || template.startsWith("https://") || template.startsWith("{{")) {
            return template;
        }
        return base + template;
    }

    private ActionRule buildAction(JsonNode group, String listField) {
        ActionRule action = new ActionRule();
        String inferSample = null;

        if (listField != null && group.hasNonNull(listField)) {
            String raw = group.get(listField).asText();
            action.setList(parser.parse(raw));
            inferSample = raw;
        }
        if (group.has("__request")) {
            try {
                action.setRequest(LENIENT.treeToValue(group.get("__request"), RequestSpec.class));
            } catch (Exception ignored) {
                // Invalid native request metadata is handled later as a missing request.
            }
        }
        var it = group.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> f = it.next();
            if ("__request".equals(f.getKey())) {
                continue;
            }
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
        if (action.getRequest() != null) {
            return;
        }
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
