package com.yuan.daydayup.reading.runtime.service;

import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.http.ReadingHttpProperties;
import com.yuan.daydayup.reading.runtime.http.SourceRateLimiter;
import com.yuan.daydayup.reading.runtime.http.SsrfValidator;
import com.yuan.daydayup.reading.runtime.service.impl.NativeSourceReadingServiceImpl;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeSourceReadingServicePaginationTest {

    private MockWebServer server;
    private NativeSourceReadingService service;
    private SourceDefinition source;
    private NativeSourceParser parser;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString().replaceAll("/$", "");

        ReadingHttpProperties properties = new ReadingHttpProperties();
        properties.setAllowPrivate(true);
        properties.setDefaultMinIntervalMs(1);
        HttpFetcher fetcher = new HttpFetcher(properties, new SsrfValidator(properties),
                new SourceRateLimiter(properties),
                new ReadingMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        service = new NativeSourceReadingServiceImpl(fetcher);

        source = new SourceDefinition();
        source.setId(1L);
        source.setName("分页测试源");
        parser = new PaginationParser(baseUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void tocFollowsRelativeNextPagesAndMergesRecords() throws Exception {
        server.enqueue(html("<a class='chapter' href='/book/c1.html'>第一章</a>"
                + "<a class='next' href='p-2.html#dir'>下一页</a>"));
        server.enqueue(html("<a class='chapter' href='/book/c2.html'>第二章</a>"));

        DirectedReadVO result = service.toc(source, parser, "/book/");

        assertEquals(2, result.getRecords().size());
        assertEquals("第一章", result.getRecords().get(0).get("chapterName"));
        assertEquals("第二章", result.getRecords().get(1).get("chapterName"));
        assertEquals("/book/", server.takeRequest().getPath());
        assertEquals("/book/p-2.html", server.takeRequest().getPath());
    }

    @Test
    void contentFollowsRelativeNextPagesAndJoinsContent() throws Exception {
        server.enqueue(html("<div class='con'>第一段</div>"
                + "<a class='next' href='c1-2.html'>下一页</a>"));
        server.enqueue(html("<div class='con'>第二段</div>"));

        DirectedReadVO result = service.content(source, parser, "/book/c1.html");

        assertEquals("第一段\n第二段", result.getRecord().get("content"));
        assertEquals("/book/c1.html", server.takeRequest().getPath());
        assertEquals("/book/c1-2.html", server.takeRequest().getPath());
    }

    @Test
    void tocRejectsPaginationLoop() {
        server.enqueue(html("<a class='chapter' href='/book/c1.html'>第一章</a>"
                + "<a class='next' href='/book/#dir'>下一页</a>"));

        BizException error = assertThrows(BizException.class,
                () -> service.toc(source, parser, "/book/"));

        assertEquals(60202, error.getCode());
        org.junit.jupiter.api.Assertions.assertFalse(error.getMessage().contains("/book/"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void tocRejectsMoreThanTwentyPages() {
        for (int page = 1; page <= 20; page++) {
            server.enqueue(html("<a class='chapter' href='/book/c" + page + ".html'>第" + page + "章</a>"
                    + "<a class='next' href='p-" + (page + 1) + ".html'>下一页</a>"));
        }

        BizException error = assertThrows(BizException.class,
                () -> service.toc(source, parser, "/book/p-1.html"));

        assertEquals(60202, error.getCode());
        assertEquals(20, server.getRequestCount());
    }

    @Test
    void contentRejectsEmptyResultAfterAllPagesParsed() {
        server.enqueue(html("<div class='missing-content'>empty</div>"));

        BizException error = assertThrows(BizException.class,
                () -> service.content(source, parser, "/book/empty.html"));

        assertEquals(60301, error.getCode());
    }

    private static MockResponse html(String body) {
        return new MockResponse().setHeader("Content-Type", "text/html; charset=utf-8").setBody(body);
    }

    private static final class PaginationParser implements NativeSourceParser {

        private final String baseUrl;

        private PaginationParser(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @Override
        public String sourceKey() {
            return "pagination-test";
        }

        @Override
        public String baseUrl() {
            return baseUrl;
        }

        @Override
        public NativeFetchPlan searchPlan(String keyword, int page) {
            return NativeFetchPlan.get(baseUrl);
        }

        @Override
        public List<Map<String, String>> parseSearch(Document doc) {
            return List.of();
        }

        @Override
        public NativeFetchPlan detailPlan(String bookUrl) {
            return NativeFetchPlan.get(bookUrl);
        }

        @Override
        public Map<String, String> parseDetail(Document doc, String rawHtml) {
            return Map.of();
        }

        @Override
        public NativeFetchPlan tocPlan(String tocUrl) {
            return NativeFetchPlan.get(tocUrl);
        }

        @Override
        public List<Map<String, String>> parseToc(Document doc) {
            List<Map<String, String>> records = new ArrayList<>();
            for (Element link : doc.select("a.chapter")) {
                Map<String, String> record = new LinkedHashMap<>();
                record.put("chapterName", link.text());
                record.put("chapterUrl", link.attr("href"));
                records.add(record);
            }
            return records;
        }

        @Override
        public String nextTocUrl(Document doc) {
            Element next = doc.selectFirst("a.next");
            return next == null ? null : next.attr("href");
        }

        @Override
        public NativeFetchPlan contentPlan(String contentUrl) {
            return NativeFetchPlan.get(contentUrl);
        }

        @Override
        public Map<String, String> parseContent(Document doc) {
            return Map.of("content", doc.select(".con").text());
        }

        @Override
        public String nextContentUrl(Document doc) {
            Element next = doc.selectFirst("a.next");
            return next == null ? null : next.attr("href");
        }
    }
}
