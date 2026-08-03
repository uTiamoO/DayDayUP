package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link HttpFetcher} 集成测试（MockWebServer 跑在 127.0.0.1，故用 allow-private 放行；
 * 生产该开关必须为 false，见 {@link SsrfValidatorTest} 的拦截用例）。
 */
class HttpFetcherTest {

    private MockWebServer server;
    private HttpFetcher fetcher;
    private SimpleMeterRegistry registry;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        base = server.url("/").toString().replaceAll("/$", "");

        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);
        props.setDefaultMinIntervalMs(1);
        registry = new SimpleMeterRegistry();
        fetcher = new HttpFetcher(props, new SsrfValidator(props), new SourceRateLimiter(props),
                new ReadingMetrics(registry));
    }

    /** 断言 reading.fetch.duration 存在指定 result tag 的 Timer 且计数为 1。 */
    private void assertFetchResult(String result) {
        Timer timer = registry.find("reading.fetch.duration").tag("result", result).timer();
        assertNotNull(timer, "缺少 result=" + result + " 的 fetch Timer");
        assertEquals(1, timer.count());
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
        assertFetchResult("success");
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
    void crossHostRedirectDropsSensitiveHeaders() throws Exception {
        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);
        props.setDefaultMinIntervalMs(1);
        SsrfValidator validator = new SsrfValidator(props);
        validator.setResolver(host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")});
        HttpFetcher redirectFetcher = new HttpFetcher(props, validator, new SourceRateLimiter(props),
                new ReadingMetrics(new SimpleMeterRegistry()));
        int port = server.getPort();
        String first = "http://a.example.test:" + port + "/first";
        String second = "http://b.example.test:" + port + "/next";
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", second));
        server.enqueue(new MockResponse().setBody("landed"));

        String body = redirectFetcher.fetch(HttpFetcher.FetchRequest.builder()
                .sourceKey("redirect-test")
                .baseUrl("http://a.example.test:" + port)
                .url(first)
                .headers(Map.of(
                        "Authorization", "test-authorization",
                        "Cookie", "session=test",
                        "client-device", "test-device",
                        "X-Trace", "keep-me"))
                .build());

        assertEquals("landed", body);
        RecordedRequest initial = server.takeRequest();
        RecordedRequest redirected = server.takeRequest();
        assertEquals("test-authorization", initial.getHeader("Authorization"));
        assertNull(redirected.getHeader("Authorization"));
        assertNull(redirected.getHeader("Cookie"));
        assertNull(redirected.getHeader("client-device"));
        assertEquals("keep-me", redirected.getHeader("X-Trace"));
    }

    @Test
    void exactAllowedHostBlocksSameDomainRedirectBeforeCredentialsCanLeave() throws Exception {
        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);
        props.setDefaultMinIntervalMs(1);
        SsrfValidator validator = new SsrfValidator(props);
        validator.setResolver(host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")});
        HttpFetcher strictFetcher = new HttpFetcher(props, validator, new SourceRateLimiter(props),
                new ReadingMetrics(new SimpleMeterRegistry()));
        int port = server.getPort();
        String first = "http://a.example.test:" + port + "/first";
        String second = "http://b.example.test:" + port + "/next";
        server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", second));

        BizException error = assertThrows(BizException.class, () -> strictFetcher.fetch(
                HttpFetcher.FetchRequest.builder()
                        .sourceKey("redirect-test")
                        .baseUrl("http://a.example.test:" + port)
                        .url(first)
                        .allowedHosts(Set.of("a.example.test"))
                        .headers(Map.of("Authorization", "test-authorization"))
                        .build()));

        assertEquals(60103, error.getCode());
        assertEquals(1, server.getRequestCount());
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
        assertFalse(e.getMessage().contains(base));
        assertFalse(e.getMessage().contains("/boom"));
        assertFetchResult("failed");
    }

    @Test
    void privateTargetBlockedWhenNotAllowed() {
        ReadingHttpProperties strict = new ReadingHttpProperties();
        SimpleMeterRegistry strictRegistry = new SimpleMeterRegistry();
        HttpFetcher strictFetcher = new HttpFetcher(strict, new SsrfValidator(strict), new SourceRateLimiter(strict),
                new ReadingMetrics(strictRegistry));

        BizException e = assertThrows(BizException.class, () -> strictFetcher.fetch(
                HttpFetcher.FetchRequest.builder()
                        .sourceKey(base).baseUrl(base).url(base + "/x").build()));
        assertEquals(60103, e.getCode());
        assertEquals(0, server.getRequestCount());
        // SSRF 拦截应记为 result=blocked（与 timeout/failed 区分）
        Timer blocked = strictRegistry.find("reading.fetch.duration").tag("result", "blocked").timer();
        assertNotNull(blocked, "SSRF 拦截未记为 result=blocked");
        assertEquals(1, blocked.count());
    }
}
