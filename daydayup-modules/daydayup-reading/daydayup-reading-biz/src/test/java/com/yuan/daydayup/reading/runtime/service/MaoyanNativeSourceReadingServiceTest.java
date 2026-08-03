package com.yuan.daydayup.reading.runtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.http.ReadingHttpProperties;
import com.yuan.daydayup.reading.runtime.http.SourceRateLimiter;
import com.yuan.daydayup.reading.runtime.http.SsrfValidator;
import com.yuan.daydayup.reading.runtime.service.impl.NativeSourceReadingServiceImpl;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.nativeparser.maoyan.MaoyanSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.maoyan.MaoyanSourceProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaoyanNativeSourceReadingServiceTest {

    private MockWebServer server;
    private NativeSourceReadingService service;
    private MaoyanSourceParser parser;
    private SourceDefinition source;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String baseUrl = server.url("/").toString().replaceAll("/$", "");

        ReadingHttpProperties httpProperties = new ReadingHttpProperties();
        httpProperties.setAllowPrivate(true);
        httpProperties.setDefaultMinIntervalMs(1);
        registry = new SimpleMeterRegistry();
        HttpFetcher fetcher = new HttpFetcher(httpProperties, new SsrfValidator(httpProperties),
                new SourceRateLimiter(httpProperties),
                new ReadingMetrics(registry));
        service = new NativeSourceReadingServiceImpl(fetcher);

        MaoyanSourceProperties sourceProperties = new MaoyanSourceProperties();
        sourceProperties.setAuthorization("test-authorization");
        sourceProperties.setClientDevice("test-device");
        parser = new MaoyanSourceParser(new ObjectMapper(), sourceProperties, baseUrl);
        source = new SourceDefinition();
        source.setId(90030001L);
        source.setName("猫眼看书");
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void searchDetailTocAndRawJsonContentUseUnifiedHttpStack() throws Exception {
        String baseUrl = parser.baseUrl();
        String contentUrl = baseUrl + "/content/1.json";
        server.enqueue(json("{\"data\":[{\"novelId\":697647,\"novelName\":\"我不是戏神\",\"authorName\":\"三九音域\"}]}"));
        server.enqueue(json("{\"data\":{\"novelId\":697647,\"novelName\":\"我不是戏神\",\"authorName\":\"三九音域\"}}"));
        server.enqueue(json("{\"data\":{\"list\":[{\"chapterName\":\"第 1 章\",\"path\":\""
                + encrypt(contentUrl) + "\"}]}}"));
        server.enqueue(json("{\"content\":\"<p>第一段</p>\\n第二段\"}"));

        DirectedReadVO search = service.search(source, parser, "我不是戏神", 1);
        DirectedReadVO detail = service.detail(source, parser, search.getRecords().get(0).get("bookUrl"));
        DirectedReadVO toc = service.toc(source, parser, detail.getRecord().get("tocUrl"));
        DirectedReadVO content = service.content(source, parser, toc.getRecords().get(0).get("chapterUrl"));

        assertEquals("我不是戏神", search.getRecords().get(0).get("name"));
        assertEquals("三九音域", detail.getRecord().get("author"));
        assertEquals(contentUrl, toc.getRecords().get(0).get("chapterUrl"));
        assertEquals("<p>第一段</p>\n第二段", content.getRecord().get("content"));

        var searchRequest = server.takeRequest();
        assertTrue(searchRequest.getPath().startsWith("/search?keyword="));
        assertEquals("test-authorization", searchRequest.getHeader("Authorization"));
        assertEquals("/novel/697647?isSearch=0", server.takeRequest().getPath());
        assertEquals("/novel/697647/chapters", server.takeRequest().getPath());
        assertEquals("/content/1.json", server.takeRequest().getPath());
        assertEquals(4, registry.find("reading.fetch.duration")
                .tag("source", "maoyankanshu").timers().stream().mapToLong(timer -> timer.count()).sum());
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json; charset=utf-8").setBody(body);
    }

    private static String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec("f041c49714d39908".getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }
}
