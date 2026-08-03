package com.yuan.daydayup.reading.source.nativeparser;

import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 原生书源解析器（v3.0 自研主线，替代直接执行外部 RuleModel）。
 *
 * <p>职责边界：把「站点动作 + 入参」翻译成 {@link NativeFetchPlan}（抓取交给统一 HttpFetcher），
 * 并把抓回的 HTML 解析成下游可消费的记录。记录字段名沿用 Legado ruleSearch/ruleBookInfo/
 * ruleToc/ruleContent 方言（name/author/bookUrl/kind/coverUrl/intro/lastChapter/chapterName/
 * chapterUrl/content），以复用既有 {@code SearchCandidates}/{@code TocEntry}/{@code ContentFetch}
 * 装配链路，不引入新的入库通路。</p>
 *
 * <p>每个具体实现对应一个站点（如 69shuba），由 {@link NativeSourceParserRegistry} 按
 * {@code sourceKey} 注册与查找。解析器不持有 HTTP 客户端，出站语义与限速/SSRF 由 runtime 统一保证。</p>
 */
public interface NativeSourceParser {

    /** 解析器标识，与 {@code SourceDefinition.tags} 中的 native key 对应（如 {@code 69shuba}）。 */
    String sourceKey();

    /** 站点基准 URL，用于相对 URL 规范化与 SSRF 同域锚点。 */
    String baseUrl();

    /**
     * 精确允许的出站 host。空集合沿用 runtime 的同注册域规则；凭据型来源应覆盖为 canonical host。
     */
    default Set<String> allowedHosts() {
        return Set.of();
    }

    /** 构造搜索抓取计划（可能是 GBK POST 表单）。 */
    NativeFetchPlan searchPlan(String keyword, int page);

    /** 从搜索响应解析候选列表；命中挑战由上层短路，不会进到这里。 */
    List<Map<String, String>> parseSearch(Document doc);

    /**
     * 从搜索响应解析候选列表，并保留未经 Jsoup 改写的原始响应体供 JSON parser 使用。
     * HTML parser 无需改造，默认继续使用既有 {@link Document} 合同。
     */
    default List<Map<String, String>> parseSearch(Document doc, String rawBody) {
        return parseSearch(doc);
    }

    /** 构造详情抓取计划。 */
    NativeFetchPlan detailPlan(String bookUrl);

    /** 从详情响应解析单本元数据（含 tocUrl 供后续目录抓取）。rawHtml 用于 JS 内联字段（如 tags）正则提取。 */
    Map<String, String> parseDetail(Document doc, String rawHtml);

    /** 构造目录抓取计划。 */
    NativeFetchPlan tocPlan(String tocUrl);

    /** 从目录响应解析章节列表（DOM 顺序即章节序）。 */
    List<Map<String, String>> parseToc(Document doc);

    /** JSON parser 可覆盖此方法读取原始响应体；HTML parser 默认保持原行为。 */
    default List<Map<String, String>> parseToc(Document doc, String rawBody) {
        return parseToc(doc);
    }

    /**
     * 从当前目录页提取下一页 URL。默认无分页；返回相对 URL 时由 runtime 按当前响应 URL 解析。
     */
    default String nextTocUrl(Document doc) {
        return null;
    }

    /** 构造正文抓取计划。 */
    NativeFetchPlan contentPlan(String contentUrl);

    /** 从正文响应解析并做源特定清洗，输出 {@code content} 字段。 */
    Map<String, String> parseContent(Document doc);

    /** JSON parser 可覆盖此方法读取原始响应体；HTML parser 默认保持原行为。 */
    default Map<String, String> parseContent(Document doc, String rawBody) {
        return parseContent(doc);
    }

    /**
     * 从当前正文页提取下一页 URL。必须只返回同一章节的分页链接，不得把“下一章”当成分页。
     */
    default String nextContentUrl(Document doc) {
        return null;
    }
}
