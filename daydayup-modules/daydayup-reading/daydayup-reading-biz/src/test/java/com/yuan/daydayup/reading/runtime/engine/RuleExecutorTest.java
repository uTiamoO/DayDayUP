package com.yuan.daydayup.reading.runtime.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.compiler.RuleCompiler;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RuleExecutor} 端到端单测（离线）：Legado JSON → 编译 RuleModel → 对固定 HTML/JSON 响应抽取。
 * 覆盖 jsoup 方言（类/属性/索引/嵌套标签/text.）、JSONPath、模板、原生 aes 后处理。
 */
class RuleExecutorTest {

    private final ObjectMapper om = new ObjectMapper();
    private final RuleCompiler compiler = new RuleCompiler(new RuleStringParser());
    private final RuleExecutor executor = new RuleExecutor();

    private RuleModel compile(String legadoJson) throws Exception {
        return compiler.compile(om.readTree(legadoJson));
    }

    @Test
    void htmlSearchList() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "http://h",
                  "ruleSearch": {
                    "bookList": ".book-item",
                    "name": ".book-title@text",
                    "author": ".book-author@text",
                    "bookUrl": "href",
                    "coverUrl": "img@src"
                  }
                }
                """);
        String html = """
                <html><body>
                  <a class="book-item" href="/b/1"><div class="book-title">书甲</div><div class="book-author">作者甲</div><img src="/c1.jpg"></a>
                  <a class="book-item" href="/b/2"><div class="book-title">书乙</div><div class="book-author">作者乙</div><img src="/c2.jpg"></a>
                </body></html>
                """;
        List<Map<String, String>> records = executor.extractList("html", html, model.getActions().get("search"));
        assertEquals(2, records.size());
        assertEquals("书甲", records.get(0).get("name"));
        assertEquals("作者甲", records.get(0).get("author"));
        assertEquals("/b/1", records.get(0).get("bookUrl"));
        assertEquals("/c1.jpg", records.get(0).get("coverUrl"));
        assertEquals("书乙", records.get(1).get("name"));
    }

    @Test
    void htmlTocIndexAndTextMatch() throws Exception {
        // 篱笆：.chapter-list.1@a → 第 2 个 .chapter-list 下的 a
        RuleModel model = compile("""
                {
                  "bookSourceName": "t", "bookSourceUrl": "http://h",
                  "ruleToc": { "chapterList": ".chapter-list.1@a", "chapterName": "text", "chapterUrl": "href" }
                }
                """);
        String html = """
                <div class="chapter-list"><a href="/x0">卷一</a></div>
                <div class="chapter-list"><a href="/c1">第一章</a><a href="/c2">第二章</a></div>
                """;
        List<Map<String, String>> records = executor.extractList("html", html, model.getActions().get("toc"));
        assertEquals(2, records.size());
        assertEquals("第一章", records.get(0).get("chapterName"));
        assertEquals("/c1", records.get(0).get("chapterUrl"));
        assertEquals("第二章", records.get(1).get("chapterName"));
    }

    @Test
    void jsonSearchListWithTemplateUrl() throws Exception {
        RuleModel model = compile("""
                {
                  "bookSourceName": "m", "bookSourceUrl": "http://api",
                  "ruleSearch": {
                    "bookList": "$.data[*]",
                    "name": "$.novelName",
                    "author": "$.authorName",
                    "bookUrl": "/novel/{{$.novelId}}?isSearch=1"
                  }
                }
                """);
        String json = """
                {"data":[
                  {"novelName":"凡人修仙传","authorName":"忘语","novelId":123},
                  {"novelName":"仙逆","authorName":"耳根","novelId":456}
                ]}
                """;
        List<Map<String, String>> records = executor.extractList("json", json, model.getActions().get("search"));
        assertEquals(2, records.size());
        assertEquals("凡人修仙传", records.get(0).get("name"));
        assertEquals("忘语", records.get(0).get("author"));
        assertEquals("/novel/123?isSearch=1", records.get(0).get("bookUrl"));
        assertEquals("/novel/456?isSearch=1", records.get(1).get("bookUrl"));
    }

    @Test
    void jsonTocWithNativeAesDecrypt() throws Exception {
        // 猫眼 toc：chapterUrl 取 $.path 后经 aes 解密（编译期原生收敛，运行时原生执行）
        String plainUrl = "http://api.jmlldsc.com/697/697647/91038.json";
        String cipherB64 = NativePostProcessorsTest.aesEncryptBase64(plainUrl);
        String legado = """
                {
                  "bookSourceName": "猫眼", "bookSourceUrl": "http://api.jmlldsc.com",
                  "ruleToc": {
                    "chapterList": "$.data.list[*]",
                    "chapterName": "$.chapterName",
                    "chapterUrl": "$.path@js:java.aesBase64DecodeToString(result,\\"f041c49714d39908\\",\\"AES/CBC/PKCS5Padding\\",\\"0123456789abcdef\\")"
                  }
                }
                """;
        RuleModel model = compile(legado);
        String json = "{\"data\":{\"list\":[{\"chapterName\":\"第一章\",\"path\":\"" + cipherB64 + "\"}]}}";

        List<Map<String, String>> records = executor.extractList("json", json, model.getActions().get("toc"));
        assertEquals(1, records.size());
        assertEquals("第一章", records.get(0).get("chapterName"));
        assertEquals(plainUrl, records.get(0).get("chapterUrl"));
    }
}
