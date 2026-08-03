package com.yuan.daydayup.reading.source.nativeparser.sudugu;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.http.ReadingHttpProperties;
import com.yuan.daydayup.reading.runtime.http.SourceRateLimiter;
import com.yuan.daydayup.reading.runtime.http.SsrfValidator;
import com.yuan.daydayup.reading.runtime.service.NativeSourceReadingService;
import com.yuan.daydayup.reading.runtime.service.impl.NativeSourceReadingServiceImpl;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "SUDUGU_LIVE_TEST", matches = "true")
class SuduguLiveSmokeTest {

    @Test
    void liveSearchDetailFullTocAndPagedContent() {
        ReadingHttpProperties properties = new ReadingHttpProperties();
        properties.setConnectTimeoutMs(10000);
        properties.setReadTimeoutMs(30000);
        properties.setDefaultMinIntervalMs(50);
        HttpFetcher fetcher = new HttpFetcher(properties, new SsrfValidator(properties),
                new SourceRateLimiter(properties),
                new ReadingMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        NativeSourceReadingService service = new NativeSourceReadingServiceImpl(fetcher);
        SuduguSourceParser parser = new SuduguSourceParser();
        SourceDefinition source = new SourceDefinition();
        source.setId(90020001L);
        source.setName("速读谷");

        DirectedReadVO search = service.search(source, parser, "凡人修仙传", 1);
        assertFalse(search.getRecords().isEmpty());
        assertEquals("/128/", search.getRecords().get(0).get("bookUrl"));

        DirectedReadVO detail = service.detail(source, parser, "/128/");
        assertEquals("凡人修仙传", detail.getRecord().get("name"));
        assertEquals("忘语", detail.getRecord().get("author"));

        DirectedReadVO toc = service.toc(source, parser, detail.getRecord().get("tocUrl"));
        assertTrue(toc.getRecords().size() >= 2500, "应跨目录分页抓到完整作品目录");
        assertEquals("第1章 山边小村", toc.getRecords().get(0).get("chapterName"));

        DirectedReadVO content = service.content(source, parser,
                toc.getRecords().get(0).get("chapterUrl"));
        String text = content.getRecord().get("content");
        assertTrue(text.contains("第1章 山边小村"));
        assertTrue(text.length() > 1000, "首章应合并正文分页，而不是只返回第一页");
    }
}
