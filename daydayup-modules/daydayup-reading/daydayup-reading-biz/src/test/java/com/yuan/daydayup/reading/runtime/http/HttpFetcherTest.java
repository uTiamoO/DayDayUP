package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.exception.BizException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link HttpFetcher} 集成测试（MockWebServer 跑在 127.0.0.1，故用 allow-private 放行；
 * 生产该开关必须为 false，见 {@link SsrfValidatorTest} 的拦截用例）。
 */
class HttpFetcherTest {

    private MockWebServer server;
    private HttpFetcher fetcher;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        base = server.url("/").toString().replaceAll("/$", "");

        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);
        props.setDefaultMinIntervalMs(1);
        fetcher = new HttpFetcher(props, new SsrfValidator(props), new SourceRateLimiter(props));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private HttpFetcher.FetchRequest.FetchRequestBuilder req(String path) {
        return HttpFetcher.FetchRequest.builder()
                .sourceKey(base)
                .baseUrl(base)
                .url(base + path);
    }

    @Test
    void getWithHeadersAndGbkDecode() throws Exception {
        byte[] gbk = "中文内容".getBytes(Charset.forName("GBK"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html; charset=gbk")
                .setBody(new Buffer().write(gbk)));

        String body = fetcher.fetch(req("/page")
                .headers(Map.of("X-Token", "abc"))
                .build());
        assertEquals("中文内容", body);

        RecordedRequest recorded = server.takeRequest();
        assertEquals("abc", recorded.getHeader("X-Token"));
        assertEquals("GET", recorded.getMethod());
    }

    @Test
    void explicitCharsetOverridesContentType() throws Exception {
        byte[] gbk = "覆盖测试".getBytes(Charset.forName("GBK"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/html")   // 未声明 charset
                .setBody(new Buffer().write(gbk)));

        String body = fetcher.fetch(req("/page").charset("gbk").build());
        assertEquals("覆盖测试", body);
    }

    @Test
    void followsRedirectWithValidation() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/next"));
        server.enqueue(new MockResponse().setBody("landed"));

        assertEquals("landed", fetcher.fetch(req("/first").build()));
        server.takeRequest();
        assertEquals("/next", server.takeRequest().getPath());
    }

    @Test
    void postBodySent() throws Exception {
        server.enqueue(new MockResponse().setBody("ok"));

        fetcher.fetch(req("/search").method("POST").body("kw=abc&p=1").build());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("kw=abc&p=1", recorded.getBody().readUtf8());
    }

    @Test
    void non2xxMapsToFetchFailed() {
        server.enqueue(new MockResponse().setResponseCode(500));
        BizException e = assertThrows(BizException.class, () -> fetcher.fetch(req("/boom").build()));
        assertEquals(60101, e.getCode());
    }

    @Test
    void privateTargetBlockedWhenNotAllowed() {
        ReadingHttpProperties strict = new ReadingHttpProperties();
        HttpFetcher strictFetcher = new HttpFetcher(strict, new SsrfValidator(strict), new SourceRateLimiter(strict));

        BizException e = assertThrows(BizException.class, () -> strictFetcher.fetch(
                HttpFetcher.FetchRequest.builder()
                        .sourceKey(base).baseUrl(base).url(base + "/x").build()));
        assertEquals(60103, e.getCode());
        assertEquals(0, server.getRequestCount());
    }
}
