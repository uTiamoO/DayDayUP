package com.yuan.daydayup.reading.source.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.SourceImportResultVO;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParserRegistry;
import com.yuan.daydayup.reading.source.service.SourceImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 书源导入服务实现。
 *
 * <p>v1 仅支持标准 Legado 格式：一个 JSON 文件可为单个书源对象或书源对象数组，
 * 以 {@code bookSourceUrl} 为幂等键。缺少该字段的条目（如香色/自定义中台格式）
 * 视为范围外，计入失败并跳过——与「v1 只做 Legado」范围一致。</p>
 */
@Slf4j
@Service
public class SourceImportServiceImpl implements SourceImportService {

    /** 失败明细上限，避免结果体过大 */
    private static final int MAX_ERROR_DETAILS = 50;

    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization", "cookie", "x-token", "token", "qttoken", "deviceid", "device-id",
            "client-device", "client_device", "x-device-id");

    private final SourceDefinitionMapper mapper;
    private final ObjectMapper objectMapper;
    private final NativeSourceParserRegistry nativeParserRegistry;

    /** 默认导入目录（相对进程工作目录），可被入参覆盖 */
    @Value("${reading.source.import-dir:docs/书源}")
    private String defaultImportDir;

    public SourceImportServiceImpl(SourceDefinitionMapper mapper, ObjectMapper objectMapper,
                                   NativeSourceParserRegistry nativeParserRegistry) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.nativeParserRegistry = nativeParserRegistry;
    }

    @Override
    public SourceImportResultVO importFromDirectory(String dir) {
        String target = StringUtils.hasText(dir) ? dir : defaultImportDir;
        Path root = Path.of(target);
        if (!Files.isDirectory(root)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "导入目录不存在或不是目录: " + root.toAbsolutePath());
        }

        SourceImportResultVO result = new SourceImportResultVO();
        List<Path> files = listJsonFiles(root);
        log.info("[source-import] 扫描目录 {}，发现 {} 个 JSON 文件", root.toAbsolutePath(), files.size());

        for (Path file : files) {
            try {
                importFile(file, result);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                addError(result, file.getFileName() + ": " + e.getMessage());
                log.warn("[source-import] 文件导入失败 {}", file, e);
            }
        }
        log.info("[source-import] 完成：total={} inserted={} updated={} skipped={} failed={}",
                result.getTotal(), result.getInserted(), result.getUpdated(), result.getSkipped(), result.getFailed());
        return result;
    }

    private List<Path> listJsonFiles(Path root) {
        try (Stream<Path> walk = Files.walk(root, 3)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .toList();
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "扫描目录失败: " + e.getMessage());
        }
    }

    private void importFile(Path file, SourceImportResultVO result) throws Exception {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(content);
        String origin = file.getFileName().toString();

        if (root.isArray()) {
            for (JsonNode node : root) {
                upsertOne(normalizeSourceNode(node), origin, result);
            }
        } else if (root.isObject()) {
            for (JsonNode node : expandSourceNodes(root)) {
                upsertOne(normalizeSourceNode(node), origin, result);
            }
        } else {
            result.setFailed(result.getFailed() + 1);
            addError(result, origin + ": 非法 JSON 根节点（非对象/数组）");
        }
    }

    private void upsertOne(JsonNode node, String origin, SourceImportResultVO result) {
        result.setTotal(result.getTotal() + 1);
        String url = normalizeSourceUrl(text(node, "bookSourceUrl"));
        if (!StringUtils.hasText(url)) {
            result.setFailed(result.getFailed() + 1);
            addError(result, origin + ": 缺少 bookSourceUrl/sourceUrl（无法确定书源唯一键）");
            return;
        }

        try {
            String raw = node.toString();
            String fingerprint = sha256(raw);
            // O9：查询须包含软删记录，否则同 URL 软删书源重导入会走 insert 撞唯一键
            SourceDefinition existing = mapper.selectByUrlIncludeDeleted(url);
            NativeSourceParser nativeParser = nativeParserRegistry.findByBaseUrl(url).orElse(null);

            if (existing == null) {
                mapper.insert(buildNew(node, url, raw, fingerprint, origin, nativeParser));
                result.setInserted(result.getInserted() + 1);
                return;
            }
            boolean wasDeleted = existing.getDeleted() != null && existing.getDeleted() == 1;
            if (wasDeleted) {
                // 恢复语义：软删视为放弃旧的用户编辑，重置 status/priority 为书源自身值
                mapper.restoreById(existing.getId());
                existing.setDeleted(0);
                existing.setStatus(boolOrDefault(node, "enabled", true) ? 1 : 0);
                existing.setPriority(intOrDefault(node, "weight", 0));
                applyRuleUpdate(existing, node, raw, fingerprint, origin, nativeParser);
                mapper.updateById(existing);
                result.setUpdated(result.getUpdated() + 1);
            } else if (fingerprint.equals(existing.getFingerprint())
                    && !requiresNativePromotion(existing, nativeParser)) {
                result.setSkipped(result.getSkipped() + 1);
            } else {
                applyRuleUpdate(existing, node, raw, fingerprint, origin, nativeParser);
                mapper.updateById(existing);
                result.setUpdated(result.getUpdated() + 1);
            }
        } catch (Exception e) {
            result.setFailed(result.getFailed() + 1);
            addError(result, origin + " [" + url + "]: " + e.getMessage());
            log.warn("[source-import] 书源落库失败 url={}", url, e);
        }
    }

    private List<JsonNode> expandSourceNodes(JsonNode root) {
        if (isSourceLike(root)) {
            return List.of(root);
        }
        List<JsonNode> nodes = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            JsonNode value = fields.next().getValue();
            if (value != null && value.isObject() && isSourceLike(value)) {
                nodes.add(value);
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(root);
        }
        return nodes;
    }

    private JsonNode normalizeSourceNode(JsonNode node) {
        if (node.hasNonNull("bookSourceUrl")) {
            return sanitizeSensitiveHeaders(node.deepCopy());
        }
        if (!node.hasNonNull("sourceUrl")) {
            return node;
        }
        com.fasterxml.jackson.databind.node.ObjectNode normalized = objectMapper.createObjectNode();
        normalized.setAll((com.fasterxml.jackson.databind.node.ObjectNode) node.deepCopy());
        normalized.put("bookSourceName", textOrDefault(node, "sourceName", text(node, "sourceUrl")));
        normalized.put("bookSourceUrl", text(node, "sourceUrl"));
        normalized.put("bookSourceType", mapSourceType(node));
        normalized.put("enabled", boolOrDefault(node, "enable", true));
        normalized.put("weight", intOrDefault(node, "weight", 0));
        if (node.has("httpHeaders")) {
            normalized.set("httpHeaders", sanitizeHeaderNode(node.get("httpHeaders")));
        }
        return normalized;
    }

    private static boolean isSourceLike(JsonNode node) {
        return node != null && node.isObject()
                && (node.hasNonNull("bookSourceUrl") || node.hasNonNull("sourceUrl"));
    }

    private JsonNode sanitizeSensitiveHeaders(JsonNode node) {
        if (node.has("httpHeaders")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).set("httpHeaders", sanitizeHeaderNode(node.get("httpHeaders")));
        }
        if (node.hasNonNull("header")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("header", redactSensitiveHeaderText(node.get("header").asText()));
        }
        return node;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode sanitizeHeaderNode(JsonNode headers) {
        com.fasterxml.jackson.databind.node.ObjectNode sanitized = objectMapper.createObjectNode();
        if (headers == null || !headers.isObject()) {
            return sanitized;
        }
        headers.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (SENSITIVE_HEADER_NAMES.contains(key.toLowerCase())) {
                sanitized.put(key, "<redacted>");
            } else {
                sanitized.set(key, entry.getValue());
            }
        });
        return sanitized;
    }

    private static int mapSourceType(JsonNode node) {
        JsonNode v = node.get("sourceType");
        if (v == null || v.isNull()) {
            return 0;
        }
        String text = v.asText();
        return switch (text.toLowerCase()) {
            case "audio" -> 1;
            case "comic", "manga" -> 2;
            default -> 0;
        };
    }

    private static String redactSensitiveHeaderText(String header) {
        if (!StringUtils.hasText(header)) {
            return header;
        }
        return header
                .replaceAll("(?i)(authorization\\s*['\"]?\\s*[:=]\\s*['\"])[^'\"\\r\\n,}]+", "$1<redacted>")
                .replaceAll("(?i)(cookie\\s*['\"]?\\s*[:=]\\s*['\"])[^'\"\\r\\n,}]+", "$1<redacted>")
                .replaceAll("(?i)((?:qt)?token\\s*['\"]?\\s*[:=]\\s*['\"])[^'\"\\r\\n,}]+", "$1<redacted>")
                .replaceAll("(?i)((?:device[-_]?id|client[-_]?device)\\s*['\"]?\\s*[:=]\\s*['\"])[^'\"\\r\\n,}]+", "$1<redacted>");
    }

    /** 新增：初始化全部字段，status/priority 取自书源自身 enabled/weight */
    private SourceDefinition buildNew(JsonNode node, String url, String raw, String fingerprint, String origin,
                                      NativeSourceParser nativeParser) {
        SourceDefinition entity = new SourceDefinition();
        entity.setBookSourceUrl(url);
        entity.setName(textOrDefault(node, "bookSourceName", url));
        entity.setSiteName(text(node, "bookSourceGroup"));
        entity.setBookSourceType(intOrDefault(node, "bookSourceType", 0));
        entity.setOriginType(textOrDefault(node, "originType", "legado"));
        entity.setOriginPath(origin);
        entity.setStatus(boolOrDefault(node, "enabled", true) ? 1 : 0);
        entity.setPriority(intOrDefault(node, "weight", 0));
        entity.setTags(text(node, "bookSourceGroup"));
        entity.setRawContent(raw);
        entity.setFingerprint(fingerprint);
        entity.setImportedAt(LocalDateTime.now());
        applySourceKind(entity, origin, nativeParser);
        return entity;
    }

    /** 更新：只覆盖规则相关字段，保留用户可能改过的 status / priority */
    private void applyRuleUpdate(SourceDefinition entity, JsonNode node, String raw, String fingerprint, String origin,
                                 NativeSourceParser nativeParser) {
        entity.setName(textOrDefault(node, "bookSourceName", entity.getName()));
        entity.setSiteName(text(node, "bookSourceGroup"));
        entity.setBookSourceType(intOrDefault(node, "bookSourceType", 0));
        entity.setOriginType(textOrDefault(node, "originType", "legado"));
        entity.setOriginPath(origin);
        entity.setTags(text(node, "bookSourceGroup"));
        entity.setRawContent(raw);
        entity.setFingerprint(fingerprint);
        entity.setImportedAt(LocalDateTime.now());
        applySourceKind(entity, origin, nativeParser);
    }

    private static void applySourceKind(SourceDefinition entity, String origin, NativeSourceParser nativeParser) {
        if (nativeParser == null) {
            entity.setOriginPath(origin);
            entity.setCompileGrade(null);
            return;
        }
        entity.setOriginType("native");
        entity.setOriginPath("native:" + nativeParser.sourceKey());
        entity.setTags(nativeParser.sourceKey());
        entity.setCompileGrade("full");
    }

    private static boolean requiresNativePromotion(SourceDefinition existing, NativeSourceParser nativeParser) {
        if (nativeParser == null) {
            return false;
        }
        String key = nativeParser.sourceKey();
        return !"native".equalsIgnoreCase(existing.getOriginType())
                || !key.equals(existing.getTags())
                || !("native:" + key).equals(existing.getOriginPath())
                || !"full".equalsIgnoreCase(existing.getCompileGrade());
    }

    // ── JSON 取值助手 ───────────────────────────────────────────────

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static String textOrDefault(JsonNode node, String field, String def) {
        String v = text(node, field);
        return StringUtils.hasText(v) ? v : def;
    }

    /** 仅规整纯站点根 URL 的尾斜杠；带路径的 base URL 保持原语义。 */
    private static String normalizeSourceUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String normalized = url.trim();
        int commentIndex = normalized.indexOf("##");
        if (commentIndex >= 0) {
            normalized = normalized.substring(0, commentIndex).trim();
        }
        return normalized.replaceFirst("^(https?://[^/?#]+)/$", "$1");
    }

    private static int intOrDefault(JsonNode node, String field, int def) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return def;
        }
        if (v.isNumber()) {
            return v.asInt();
        }
        try {
            return Integer.parseInt(v.asText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean boolOrDefault(JsonNode node, String field, boolean def) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asBoolean(def) : def;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "计算指纹失败: " + e.getMessage());
        }
    }

    private static void addError(SourceImportResultVO result, String msg) {
        List<String> errors = result.getErrors();
        if (errors == null) {
            errors = new ArrayList<>();
            result.setErrors(errors);
        }
        if (errors.size() < MAX_ERROR_DETAILS) {
            errors.add(msg);
        }
    }
}
