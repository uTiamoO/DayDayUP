package com.yuan.daydayup.reading.source.nativeparser.maoyan;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfEnvironmentVariable(named = "MAOYAN_LIVE_TEST", matches = "true")
class MaoyanLiveSmokeTest {

    @Test
    void liveSearchDetailTocAndContent() {
        String authorization = System.getenv("READING_MAOYAN_AUTHORIZATION");
        String clientDevice = System.getenv("READING_MAOYAN_CLIENT_DEVICE");
        Assumptions.assumeTrue(authorization != null && !authorization.isBlank(),
                "live smoke 缺少外部 Authorization，跳过");
        Assumptions.assumeTrue(clientDevice != null && !clientDevice.isBlank(),
                "live smoke 缺少外部 client-device，跳过");

        MaoyanSourceProperties sourceProperties = new MaoyanSourceProperties();
        sourceProperties.setAuthorization(authorization);
        sourceProperties.setClientDevice(clientDevice);
        MaoyanSourceParser parser = new MaoyanSourceParser(new ObjectMapper(), sourceProperties);

        ReadingHttpProperties httpProperties = new ReadingHttpProperties();
        httpProperties.setConnectTimeoutMs(10000);
        httpProperties.setReadTimeoutMs(30000);
        HttpFetcher fetcher = new HttpFetcher(httpProperties, new SsrfValidator(httpProperties),
                new SourceRateLimiter(httpProperties),
                new ReadingMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        NativeSourceReadingService service = new NativeSourceReadingServiceImpl(fetcher);
        SourceDefinition source = new SourceDefinition();
        source.setId(90030001L);
        source.setName("猫眼看书");

        DirectedReadVO search = service.search(source, parser, "我不是戏神", 1);
        assertFalse(search.getRecords().isEmpty());
        DirectedReadVO detail = service.detail(source, parser, search.getRecords().get(0).get("bookUrl"));
        DirectedReadVO toc = service.toc(source, parser, detail.getRecord().get("tocUrl"));
        assertFalse(toc.getRecords().isEmpty());
        DirectedReadVO content = service.content(source, parser, toc.getRecords().get(0).get("chapterUrl"));
        assertFalse(content.getRecord().get("content").isBlank());
    }
}
