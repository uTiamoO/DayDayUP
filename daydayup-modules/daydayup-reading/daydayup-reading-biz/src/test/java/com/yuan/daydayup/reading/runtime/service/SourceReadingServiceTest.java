package com.yuan.daydayup.reading.runtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.compiler.RuleCompiler;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import com.yuan.daydayup.reading.runtime.engine.RuleExecutor;
import com.yuan.daydayup.reading.runtime.http.HttpFetcher;
import com.yuan.daydayup.reading.runtime.http.ReadingHttpProperties;
import com.yuan.daydayup.reading.runtime.http.SourceRateLimiter;
import com.yuan.daydayup.reading.runtime.http.SsrfValidator;
import com.yuan.daydayup.reading.runtime.script.JsScriptEngine;
import com.yuan.daydayup.reading.runtime.script.ReadingScriptProperties;
import com.yuan.daydayup.reading.runtime.service.impl.SourceReadingServiceImpl;
import com.yuan.daydayup.reading.source.entity.SourceCompiledRule;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceCompiledRuleMapper;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SourceReadingService} 端到端：真实编译器 + 真实出站栈（MockWebServer）+ 真实抽取，
 * 只桩掉两张表的 Mapper。覆盖指定源搜索（JSON + 模板 + 关键词编码）与详情（HTML + 相对 URL + SSRF）。
 */
class SourceReadingServiceTest {

    private final ObjectMapper om = new ObjectMapper();
    private static JsScriptEngine jsEngine;
    private MockWebServer server;
    private String base;
    private SourceReadingService service;
    private SourceDefinitionMapper sourceMapper;
    private SourceCompiledRuleMapper compiledMapper;

    @org.junit.jupiter.api.BeforeAll
    static void bootEngine() {
        jsEngine = new JsScriptEngine(new ReadingScriptProperties());
    }

    @org.junit.jupiter.api.AfterAll
    static void closeEngine() {
        jsEngine.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        base = server.url("/").toString().replaceAll("/$", "");

        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);          // MockWebServer 在 127.0.0.1
        props.setDefaultMinIntervalMs(1);
        HttpFetcher fetcher = new HttpFetcher(props, new SsrfValidator(props), new SourceRateLimiter(props));

        sourceMapper = mock(SourceDefinitionMapper.class);
        compiledMapper = mock(SourceCompiledRuleMapper.class);
        service = new SourceReadingServiceImpl(sourceMapper, compiledMapper, fetcher,
                new RuleExecutor(), jsEngine, om);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** 用真实编译器把 Legado JSON 编译落成 mapper 桩数据 */
    private void stubCompiledSource(long id, String legadoJson) throws Exception {
        RuleModel model = new RuleCompiler(new RuleStringParser()).compile(om.readTree(legadoJson));

        SourceDefinition source = new SourceDefinition();
        source.setId(id);
        source.setName(model.getIdentity().getName());
        when(sourceMapper.selectById(id)).thenReturn(source);

        SourceCompiledRule compiled = new SourceCompiledRule();
        compiled.setSourceId(id);
        compiled.setCompileStatus(model.getHealth().getGrade().name().toLowerCase());
        compiled.setCompiledContent(om.writeValueAsString(model));
        when(compiledMapper.selectOne(any())).thenReturn(compiled);
    }

    private String legado() {
        return """
                {
                  "bookSourceName": "测试源", "bookSourceUrl": "%s",
                  "concurrentRate": "10/1000",
                  "header": "{'X-Src': 'daydayup'}",
                  "searchUrl": "/search?wd={{key}}&page={{page}}",
                  "ruleSearch": {
                    "bookList": "$.data[*]",
                    "name": "$.novelName",
                    "bookUrl": "/novel/{{$.novelId}}"
                  },
                  "ruleBookInfo": {
                    "name": ".title@text",
                    "author": ".author@text"
                  },
                  "ruleToc": {
                    "chapterList": ".chapter-list@a",
                    "chapterName": "text",
                    "chapterUrl": "href"
                  }
                }
                """.formatted(base);
    }

    @Test
    void directedSearch() throws Exception {
        stubCompiledSource(1L, legado());
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"novelName\":\"凡人修仙传\",\"novelId\":123}]}"));

        DirectedReadVO vo = service.search(1L, "凡人", 1);

        assertEquals(1, vo.getRecords().size());
        assertEquals("凡人修仙传", vo.getRecords().get(0).get("name"));
        assertEquals("/novel/123", vo.getRecords().get(0).get("bookUrl"));

        var recorded = server.takeRequest();
        // 关键词按 UTF-8 URL 编码进模板；书源级 header 生效
        assertEquals("/search?wd=%E5%87%A1%E4%BA%BA&page=1", recorded.getPath());
        assertEquals("daydayup", recorded.getHeader("X-Src"));
    }

    @Test
    void directedDetail_relativeBookUrl() throws Exception {
        stubCompiledSource(1L, legado());
        server.enqueue(new MockResponse().setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<div class=\"title\">凡人修仙传</div><div class=\"author\">忘语</div>"));

        DirectedReadVO vo = service.detail(1L, "/novel/123");

        assertEquals("凡人修仙传", vo.getRecord().get("name"));
        assertEquals("忘语", vo.getRecord().get("author"));
        assertTrue(vo.getRequestUrl().startsWith(base));
    }

    @Test
    void detailCrossDomainBookUrlBlocked() throws Exception {
        stubCompiledSource(1L, legado());
        BizException e = assertThrows(BizException.class,
                () -> service.detail(1L, "http://evil.com/steal"));
        assertEquals(60103, e.getCode());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void uncompiledSourceRejected() {
        SourceDefinition source = new SourceDefinition();
        source.setId(9L);
        when(sourceMapper.selectById(9L)).thenReturn(source);
        when(compiledMapper.selectOne(any())).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> service.search(9L, "abc", 1));
        assertEquals(60201, e.getCode());
    }

    @Test
    void directedToc_listExtraction() throws Exception {
        stubCompiledSource(1L, legado());
        server.enqueue(new MockResponse().setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody("<div class=\"chapter-list\">"
                        + "<a href=\"/c/1\">第一章</a><a href=\"/c/2\">第二章</a></div>"));

        DirectedReadVO vo = service.toc(1L, "/novel/123");

        assertEquals(2, vo.getRecords().size());
        assertEquals("第一章", vo.getRecords().get(0).get("chapterName"));
        assertEquals("/c/1", vo.getRecords().get(0).get("chapterUrl"));
        assertEquals("第二章", vo.getRecords().get(1).get("chapterName"));
    }

    @Test
    void rejectedGradeNotRunnable() {
        SourceDefinition source = new SourceDefinition();
        source.setId(8L);
        when(sourceMapper.selectById(8L)).thenReturn(source);
        SourceCompiledRule compiled = new SourceCompiledRule();
        compiled.setCompileStatus("rejected");
        when(compiledMapper.selectOne(any())).thenReturn(compiled);

        BizException e = assertThrows(BizException.class, () -> service.search(8L, "abc", 1));
        assertEquals(60201, e.getCode());
    }

    @Test
    void degradedSourceWithScriptStepRunsViaGraalJs() throws Exception {
        // name 带 @js: 后缀 → SCRIPT 步（未被原生收敛）→ degraded，但在线链路可经 GraalJS 执行
        stubCompiledSource(2L, """
                {
                  "bookSourceName": "js源", "bookSourceUrl": "%s",
                  "searchUrl": "/search?wd={{key}}&page={{page}}",
                  "ruleSearch": {
                    "bookList": "$.data[*]",
                    "name": "$.novelName@js:java.put('n', result); java.get('n') + '_js'"
                  }
                }
                """.formatted(base));
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"novelName\":\"仙逆\"}]}"));

        DirectedReadVO vo = service.search(2L, "仙", 1);
        assertEquals("仙逆_js", vo.getRecords().get(0).get("name"));
    }
}
