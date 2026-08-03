package com.yuan.daydayup.reading.runtime.service.impl;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.service.NativeSourceReadingService;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.nativeparser.CloudflareChallengeDetector;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 原生书源定向读取实现。
 *
 * <p>流程：native parser 产出 {@link NativeFetchPlan} → 统一 {@link HttpFetcher} 出站
 * （SSRF + per-source 限速 + 超时）→ {@link CloudflareChallengeDetector} 挑战短路
 * → Jsoup 解析 → parser 抽取为 Legado 方言记录 → {@link DirectedReadVO}。</p>
 */
@Slf4j
@Service
public class NativeSourceReadingServiceImpl implements NativeSourceReadingService {

    private static final int MAX_TOC_PAGES = 20;
    private static final int MAX_CONTENT_PAGES = 20;

    private final HttpFetcher httpFetcher;

    public NativeSourceReadingServiceImpl(HttpFetcher httpFetcher) {
        this.httpFetcher = httpFetcher;
    }

    @Override
    public DirectedReadVO search(SourceDefinition source, NativeSourceParser parser, String keyword, int page) {
        long start = System.currentTimeMillis();
        Fetched fetched = fetch(source, parser, parser.searchPlan(keyword, Math.max(1, page)));
        List<Map<String, String>> records = parser.parseSearch(fetched.doc(), fetched.rawHtml());
        DirectedReadVO vo = baseVo(source, "search", fetched.url(), start);
        vo.setRecords(records);
        return vo;
    }

    @Override
    public DirectedReadVO detail(SourceDefinition source, NativeSourceParser parser, String bookUrl) {
        long start = System.currentTimeMillis();
        String absUrl = resolveUrl(parser.baseUrl(), bookUrl);
        Fetched fetched = fetch(source, parser, parser.detailPlan(absUrl));
        Map<String, String> record = parser.parseDetail(fetched.doc(), fetched.rawHtml());
        DirectedReadVO vo = baseVo(source, "detail", fetched.url(), start);
        vo.setRecord(record);
        return vo;
    }

    @Override
    public DirectedReadVO toc(SourceDefinition source, NativeSourceParser parser, String tocUrl) {
        long start = System.currentTimeMillis();
        String pageUrl = resolveUrl(parser.baseUrl(), tocUrl);
        String requestUrl = pageUrl;
        List<Map<String, String>> records = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        int pageCount = 0;
        while (StringUtils.hasText(pageUrl)) {
            String visitKey = withoutFragment(pageUrl);
            if (!visited.add(visitKey)) {
                throw paginationError(source, "目录分页出现循环");
            }
            if (++pageCount > MAX_TOC_PAGES) {
                throw paginationError(source, "目录分页超过上限 " + MAX_TOC_PAGES);
            }
            Fetched fetched = fetch(source, parser, parser.tocPlan(pageUrl));
            records.addAll(parser.parseToc(fetched.doc(), fetched.rawHtml()));
            pageUrl = resolveNextUrl(fetched.url(), parser.nextTocUrl(fetched.doc()));
        }
        DirectedReadVO vo = baseVo(source, "toc", requestUrl, start);
        vo.setRecords(records);
        return vo;
    }

    @Override
    public DirectedReadVO content(SourceDefinition source, NativeSourceParser parser, String contentUrl) {
        long start = System.currentTimeMillis();
        String pageUrl = resolveUrl(parser.baseUrl(), contentUrl);
        String requestUrl = pageUrl;
        Map<String, String> merged = new LinkedHashMap<>();
        List<String> contentPages = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        int pageCount = 0;
        while (StringUtils.hasText(pageUrl)) {
            String visitKey = withoutFragment(pageUrl);
            if (!visited.add(visitKey)) {
                throw paginationError(source, "正文分页出现循环");
            }
            if (++pageCount > MAX_CONTENT_PAGES) {
                throw paginationError(source, "正文分页超过上限 " + MAX_CONTENT_PAGES);
            }
            Fetched fetched = fetch(source, parser, parser.contentPlan(pageUrl));
            Map<String, String> pageRecord = parser.parseContent(fetched.doc(), fetched.rawHtml());
            if (pageRecord != null) {
                merged.putAll(pageRecord);
                String content = pageRecord.get("content");
                if (StringUtils.hasText(content)) {
                    contentPages.add(content.strip());
                }
            }
            pageUrl = resolveNextUrl(fetched.url(), parser.nextContentUrl(fetched.doc()));
        }
        String content = String.join("\n", contentPages);
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "源站正文为空: " + source.getName());
        }
        merged.put("content", content);
        DirectedReadVO vo = baseVo(source, "content", requestUrl, start);
        vo.setRecord(merged);
        return vo;
    }

    // ── 抓取 + 挑战检测 + 解析 ─────────────────────────────────────

    private record Fetched(String url, String rawHtml, Document doc) {
    }

    private Fetched fetch(SourceDefinition source, NativeSourceParser parser, NativeFetchPlan plan) {
        String absUrl = resolveUrl(parser.baseUrl(), plan.url());
        String html = httpFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey(parser.sourceKey())
                .baseUrl(parser.baseUrl())
                .allowedHosts(parser.allowedHosts())
                .url(absUrl)
                .method(plan.method())
                .body(plan.body())
                .charset(plan.charset())
                .headers(plan.headers())
                .build());
        // selector 之前先检测反爬挑战：命中归一化为 verification_required（60103），不当作 parse_error。
        if (CloudflareChallengeDetector.isChallenge(html)) {
            throw new BizException(ErrorCode.READING_UPSTREAM_BLOCKED,
                    "源站返回人机验证挑战，需人工验证（cloudflare_challenge）: " + source.getName());
        }
        Document doc = Jsoup.parse(html, absUrl);
        return new Fetched(absUrl, html, doc);
    }

    private static DirectedReadVO baseVo(SourceDefinition source, String action, String url, long start) {
        DirectedReadVO vo = new DirectedReadVO();
        vo.setSourceId(source.getId());
        vo.setSourceName(source.getName());
        vo.setAction(action);
        vo.setRequestUrl(url);
        vo.setElapsedMs(System.currentTimeMillis() - start);
        return vo;
    }

    /** 相对 URL 按书源 baseUrl 解析为绝对（是否同域交由 SSRF 校验判定）。 */
    static String resolveUrl(String baseUrl, String url) {
        if (!StringUtils.hasText(url)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "URL 不能为空");
        }
        String u = url.strip();
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        return URI.create(baseUrl).resolve(u.startsWith("/") ? u : "/" + u).toString();
    }

    private static String resolveNextUrl(String currentUrl, String nextUrl) {
        if (!StringUtils.hasText(nextUrl)) {
            return null;
        }
        return URI.create(currentUrl).resolve(nextUrl.strip()).toString();
    }

    private static String withoutFragment(String url) {
        try {
            URI uri = URI.create(url);
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null).toString();
        } catch (IllegalArgumentException | URISyntaxException e) {
            return url;
        }
    }

    private static BizException paginationError(SourceDefinition source, String reason) {
        return new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                reason + ": " + source.getName());
    }
}
