package com.yuan.daydayup.reading.source.nativeparser;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 原生书源解析器注册表：按 {@code sourceKey} 聚合所有 {@link NativeSourceParser} bean。
 *
 * <p>runtime 通过 {@code SourceDefinition} 的 native key 查表分发：命中则走自研解析主线，
 * 未命中回落 legacy RuleModel 路径。</p>
 */
@Component
public class NativeSourceParserRegistry {

    private final Map<String, NativeSourceParser> parsers = new LinkedHashMap<>();

    public NativeSourceParserRegistry(List<NativeSourceParser> parserBeans) {
        for (NativeSourceParser parser : parserBeans) {
            parsers.put(parser.sourceKey(), parser);
        }
    }

    /** 按 native key 查找解析器。 */
    public Optional<NativeSourceParser> find(String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parsers.get(sourceKey.trim()));
    }

    /** 按书源 baseUrl 查找解析器（SourceDefinition.bookSourceUrl → parser）。 */
    public Optional<NativeSourceParser> findByBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return Optional.empty();
        }
        String normalized = stripTrailingSlash(baseUrl.trim());
        return parsers.values().stream()
                .filter(p -> stripTrailingSlash(p.baseUrl()).equalsIgnoreCase(normalized))
                .findFirst();
    }

    public boolean supports(String sourceKey) {
        return find(sourceKey).isPresent();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
