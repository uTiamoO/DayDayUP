package com.yuan.daydayup.reading.source.nativeparser.maoyan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

/** 猫眼看书 JSON API 原生解析器。 */
@Component
public class MaoyanSourceParser implements NativeSourceParser {

    public static final String BASE_URL = "http://api.jmlldsc.com";

    private static final Pattern NOVEL_PATH = Pattern.compile("/novel/(\\d+)(?:[/?]|$)");
    private static final String AES_KEY = "f041c49714d39908";
    private static final String AES_IV = "0123456789abcdef";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private final ObjectMapper objectMapper;
    private final MaoyanSourceProperties properties;
    private final String baseUrl;

    @Autowired
    public MaoyanSourceParser(ObjectMapper objectMapper, MaoyanSourceProperties properties) {
        this(objectMapper, properties, BASE_URL);
    }

    /** 测试专用：允许把 canonical host 替换为 MockWebServer，生产 Spring 使用双参数构造。 */
    public MaoyanSourceParser(ObjectMapper objectMapper, MaoyanSourceProperties properties, String baseUrl) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    @Override
    public String sourceKey() {
        return "maoyankanshu";
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public Set<String> allowedHosts() {
        return Set.of(URI.create(baseUrl).getHost());
    }

    @Override
    public NativeFetchPlan searchPlan(String keyword, int page) {
        if (!StringUtils.hasText(keyword)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "搜索关键词不能为空");
        }
        String encoded = URLEncoder.encode(keyword.strip(), StandardCharsets.UTF_8).replace("+", "%20");
        return get(baseUrl + "/search?keyword=" + encoded + "&type=2&page=" + Math.max(1, page));
    }

    @Override
    public List<Map<String, String>> parseSearch(Document doc) {
        return parseSearch(doc, doc.body().text());
    }

    @Override
    public List<Map<String, String>> parseSearch(Document doc, String rawBody) {
        JsonNode data = requiredArray(json(rawBody, "search"), "data", "search");
        List<Map<String, String>> records = new ArrayList<>();
        for (JsonNode item : data) {
            String novelId = text(item, "novelId");
            if (!StringUtils.hasText(novelId)) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            put(record, "name", text(item, "novelName"));
            put(record, "author", text(item, "authorName"));
            put(record, "bookUrl", "/novel/" + novelId + "?isSearch=1");
            put(record, "kind", item.path("categoryNames").path(0).path("className").asText(null));
            put(record, "coverUrl", text(item, "cover"));
            put(record, "intro", text(item, "summary"));
            put(record, "wordCount", text(item, "wordNum"));
            records.add(record);
        }
        return records;
    }

    @Override
    public NativeFetchPlan detailPlan(String bookUrl) {
        return get(baseUrl + "/novel/" + novelId(bookUrl) + "?isSearch=0");
    }

    @Override
    public Map<String, String> parseDetail(Document doc, String rawBody) {
        JsonNode data = requiredObject(json(rawBody, "detail"), "data", "detail");
        Map<String, String> record = new LinkedHashMap<>();
        put(record, "name", text(data, "novelName"));
        put(record, "author", text(data, "authorName"));
        put(record, "coverUrl", text(data, "cover"));
        put(record, "intro", text(data, "summary"));
        put(record, "kind", data.path("categoryNames").path(0).path("className").asText(null));
        put(record, "wordCount", text(data, "wordNum"));
        put(record, "lastChapter", data.path("lastChapter").path("chapterName").asText(null));
        put(record, "updateTime", text(data, "lastUpdatedAt"));
        String complete = text(data, "isComplete");
        put(record, "status", "1".equals(complete) ? "completed" : "ongoing");
        String novelId = text(data, "novelId");
        if (StringUtils.hasText(novelId)) {
            put(record, "tocUrl", "/novel/" + novelId + "/chapters");
        }
        return record;
    }

    @Override
    public NativeFetchPlan tocPlan(String tocUrl) {
        return get(baseUrl + "/novel/" + novelId(tocUrl) + "/chapters");
    }

    @Override
    public List<Map<String, String>> parseToc(Document doc) {
        return parseToc(doc, doc.body().text());
    }

    @Override
    public List<Map<String, String>> parseToc(Document doc, String rawBody) {
        JsonNode data = requiredObject(json(rawBody, "toc"), "data", "toc");
        JsonNode list = requiredArray(data, "list", "toc");
        List<Map<String, String>> records = new ArrayList<>();
        for (JsonNode item : list) {
            Map<String, String> record = new LinkedHashMap<>();
            put(record, "chapterName", text(item, "chapterName"));
            String encryptedPath = text(item, "path");
            if (StringUtils.hasText(encryptedPath)) {
                put(record, "chapterUrl", decryptChapterUrl(encryptedPath));
            }
            put(record, "updateTime", text(item, "updatedAt"));
            put(record, "wordCount", text(item, "wordNum"));
            records.add(record);
        }
        return records;
    }

    @Override
    public NativeFetchPlan contentPlan(String contentUrl) {
        return get(contentUrl);
    }

    @Override
    public Map<String, String> parseContent(Document doc) {
        return parseContent(doc, doc.body().text());
    }

    @Override
    public Map<String, String> parseContent(Document doc, String rawBody) {
        JsonNode content = json(rawBody, "content").get("content");
        if (content == null || !content.isTextual()) {
            throw runtimeFailure("content", "响应缺少正文节点");
        }
        return Map.of("content", content.asText());
    }

    private NativeFetchPlan get(String url) {
        return NativeFetchPlan.builder()
                .url(url)
                .method("GET")
                .charset("UTF-8")
                .headers(headers())
                .build();
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", "okhttp/4.9.2");
        put(headers, "client-device", properties.getClientDevice());
        put(headers, "client-brand", properties.getClientBrand());
        put(headers, "client-version", properties.getClientVersion());
        put(headers, "client-channel", properties.getClientChannel());
        put(headers, "client-name", properties.getClientName());
        put(headers, "client-source", properties.getClientSource());
        put(headers, "alias-name", properties.getAliasName());
        put(headers, "Authorization", properties.getAuthorization());
        return headers;
    }

    private JsonNode json(String rawBody, String action) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                    "猫眼看书 " + action + " JSON 解析失败");
        }
    }

    private static String novelId(String sourceRef) {
        if (!StringUtils.hasText(sourceRef)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "猫眼看书作品引用不能为空");
        }
        String normalized = sourceRef.strip();
        if (normalized.matches("\\d+")) {
            return normalized;
        }
        Matcher matcher = NOVEL_PATH.matcher(normalized);
        if (!matcher.find()) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "猫眼看书作品引用无效");
        }
        return matcher.group(1);
    }

    private String decryptChapterUrl(String encryptedPath) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8)));
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(encryptedPath.strip()));
            String url = new String(plain, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(url)) {
                throw new IllegalArgumentException("empty url");
            }
            URI uri = URI.create(url);
            String expectedHost = URI.create(baseUrl).getHost();
            if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(expectedHost)) {
                throw new IllegalArgumentException("unexpected host");
            }
            return url;
        } catch (Exception e) {
            throw new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED, "猫眼看书章节地址解密失败");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static JsonNode requiredObject(JsonNode parent, String field, String action) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isObject()) {
            throw runtimeFailure(action, "响应结构不符合预期");
        }
        return value;
    }

    private static JsonNode requiredArray(JsonNode parent, String field, String action) {
        JsonNode value = parent.get(field);
        if (value == null || !value.isArray()) {
            throw runtimeFailure(action, "响应结构不符合预期");
        }
        return value;
    }

    private static BizException runtimeFailure(String action, String reason) {
        return new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                "猫眼看书 " + action + " " + reason);
    }

    private static void put(Map<String, String> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private static String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        String normalized = value.strip();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
