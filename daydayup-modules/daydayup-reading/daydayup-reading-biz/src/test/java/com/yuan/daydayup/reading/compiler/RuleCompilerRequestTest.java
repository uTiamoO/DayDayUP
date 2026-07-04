package com.yuan.daydayup.reading.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RequestSpec;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import com.yuan.daydayup.reading.runtime.engine.RuleExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RuleCompiler} 请求装配（RequestSpec / Http）与编译产物 JSON 往返测试。
 */
class RuleCompilerRequestTest {

    private final ObjectMapper om = new ObjectMapper();
    private final RuleCompiler compiler = new RuleCompiler(new RuleStringParser());

    private RuleModel compile(String legadoJson) throws Exception {
        return compiler.compile(om.readTree(legadoJson));
    }

    @Test
    void searchUrlPlain() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "https://m.libahao.com",
                  "searchUrl": "https://m.libahao.com/sou?wd={{key}}",
                  "ruleSearch": { "bookList": ".item", "name": "text" }
                }
                """);
        RequestSpec spec = model.getActions().get("search").getRequest();
        assertEquals("https://m.libahao.com/sou?wd={{key}}", spec.getUrlTemplate());
        assertEquals("GET", spec.getMethod());
        assertNull(spec.getBody());
    }

    @Test
    void searchUrlWithOptions_lenientJson() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "http://h",
                  "searchUrl": "/search,{'method':'POST','body':'kw={{key}}&p={{page}}','charset':'gbk','headers':{'X-A':'1'}}",
                  "ruleSearch": { "bookList": ".item", "name": "text" }
                }
                """);
        RequestSpec spec = model.getActions().get("search").getRequest();
        assertEquals("/search", spec.getUrlTemplate());
        assertEquals("POST", spec.getMethod());
        assertEquals("kw={{key}}&p={{page}}", spec.getBody());
        assertEquals("gbk", spec.getCharset());
        assertEquals("1", spec.getHeaders().get("X-A"));
        assertEquals(CompileGrade.FULL, model.getHealth().getGrade());
    }

    @Test
    void headerLenientAndConcurrentRate() throws Exception {
        // 猫眼式 header：单引号 + 换行的宽松 JSON
        RuleModel model = compile("""
                {
                  "bookSourceName": "m", "bookSourceUrl": "http://api.x.com",
                  "concurrentRate": "3/1000",
                  "header": "{\\n'User-Agent': 'okhttp/4.9.2','client-source': 'android'}",
                  "ruleSearch": { "bookList": "$.data[*]", "name": "$.name" }
                }
                """);
        assertEquals("3/1000", model.getHttp().getConcurrentRate());
        assertEquals("okhttp/4.9.2", model.getHttp().getHeaders().get("User-Agent"));
        assertEquals("android", model.getHttp().getHeaders().get("client-source"));
        assertEquals(CompileGrade.FULL, model.getHealth().getGrade());
    }

    @Test
    void searchUrlWithJs_degrades() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "http://h",
                  "searchUrl": "@js:buildUrl(key)",
                  "ruleSearch": { "bookList": ".item", "name": "text" }
                }
                """);
        assertNull(model.getActions().get("search").getRequest());
        assertEquals(CompileGrade.DEGRADED, model.getHealth().getGrade());
        assertFalse(model.getHealth().getScriptDeps().isEmpty());
    }

    @Test
    void compiledModelJsonRoundTrip() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "http://h",
                  "concurrentRate": "500",
                  "searchUrl": "/s?wd={{key}}&page={{page}}",
                  "ruleSearch": {
                    "bookList": ".book-item",
                    "name": ".book-title@text",
                    "bookUrl": "href"
                  }
                }
                """);
        // 持久化 → 反序列化 → 仍可执行抽取（SourceReadingService 的实际路径）
        String json = om.writeValueAsString(model);
        RuleModel revived = om.readValue(json, RuleModel.class);

        assertEquals("http://h", revived.getIdentity().getBaseUrl());
        assertEquals("500", revived.getHttp().getConcurrentRate());
        assertEquals("/s?wd={{key}}&page={{page}}", revived.getActions().get("search").getRequest().getUrlTemplate());

        String html = "<a class=\"book-item\" href=\"/b/1\"><div class=\"book-title\">书甲</div></a>";
        List<Map<String, String>> records = new RuleExecutor()
                .extractList("html", html, revived.getActions().get("search"));
        assertEquals(1, records.size());
        assertEquals("书甲", records.get(0).get("name"));
        assertEquals("/b/1", records.get(0).get("bookUrl"));
        assertTrue(revived.getHealth().getGrade() == CompileGrade.FULL);
    }
}
