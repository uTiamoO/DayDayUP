package com.yuan.daydayup.reading.compiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RuleCompiler} 单测：验证编译体检的三级分级（rulemodel §5.2）。
 */
class RuleCompilerTest {

    private final ObjectMapper om = new ObjectMapper();
    private final RuleCompiler compiler = new RuleCompiler(new RuleStringParser());

    @Test
    void compilesProjectNativeSourceFormatToExecutableRuleModel() throws Exception {
        String json = Files.readString(projectRoot().resolve("docs/书源/maoyankanshu.json"));
        JsonNode root = om.readTree(json);
        JsonNode source = root.path("maoyankanshu");

        RuleModel model = compiler.compile(source);

        assertEquals("maoyankanshu", model.getIdentity().getName());
        assertEquals("http://api.jmlldsc.com", model.getIdentity().getBaseUrl());
        assertEquals("text", model.getIdentity().getBookType());
        assertTrue(model.getIdentity().isEnabled());

        assertNotNull(model.getActions().get("search").getRequest());
        assertEquals("json", model.getActions().get("search").getResponseType());
        assertEquals("$.data", model.getActions().get("search").getList().getRaw());
        assertEquals("$.novelName", model.getActions().get("search").getFields().get("name").getRaw());
        assertTrue(model.getActions().get("search").getRequest().getUrlTemplate().contains("keyword={{key}}"));

        assertEquals("json", model.getActions().get("detail").getResponseType());
        assertEquals("$.data.novelName", model.getActions().get("detail").getFields().get("name").getRaw());
        assertTrue(model.getActions().get("detail").getRequest().getUrlTemplate().contains("/novel/{{detailUrl}}"));

        assertEquals("json", model.getActions().get("toc").getResponseType());
        assertEquals("$.data.list", model.getActions().get("toc").getList().getRaw());
        assertTrue(model.getActions().get("toc").getRequest().getUrlTemplate().contains("/novel/{{detailUrl}}/chapters"));

        assertEquals("json", model.getActions().get("content").getResponseType());
        assertTrue(model.getActions().get("content").getRequest().getUrlTemplate().contains("{{chapterUrl}}"));
        assertEquals("$.content", model.getActions().get("content").getFields().get("content").getRaw());
        assertEquals("<redacted>", model.getHttp().getHeaders().get("Authorization"));
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("docs/书源/maoyankanshu.json"))) {
            current = current.getParent();
        }
        assertNotNull(current, "project root with docs/书源/maoyankanshu.json should exist");
        return current;
    }

    @Test
    void fullGrade_whenAllNativeMappable() throws Exception {
        // 纯 jsoup + jsonpath + 可原生收敛的 aes @js → full
        String json = """
                {
                  "bookSourceName": "猫眼看书",
                  "bookSourceUrl": "http://api.jmlldsc.com##@曦灵",
                  "bookSourceType": 0,
                  "enabled": true,
                  "ruleSearch": { "bookList": "$.data[*]", "name": "$.novelName" },
                  "ruleToc": {
                    "chapterList": "$.data.list[*]",
                    "chapterName": "$.chapterName",
                    "chapterUrl": "$.path@js:java.aesBase64DecodeToString(result,\\"f041c49714d39908\\",\\"AES/CBC/PKCS5Padding\\",\\"0123456789abcdef\\")"
                  }
                }
                """;
        RuleModel model = compiler.compile(om.readTree(json));
        assertEquals(CompileGrade.FULL, model.getHealth().getGrade());
        assertTrue(model.getHealth().getScriptDeps().isEmpty(), "aes 应被原生收敛，不留 script 依赖");
        // 身份的 ## 注释应被剥离
        assertEquals("http://api.jmlldsc.com", model.getIdentity().getBaseUrl());
        assertEquals("json", model.getActions().get("toc").getResponseType());
    }

    @Test
    void degradedGrade_whenArbitraryScript() throws Exception {
        String json = """
                {
                  "bookSourceName": "脚本源",
                  "bookSourceUrl": "http://example.com",
                  "ruleSearch": { "bookList": "$.data[*]", "name": "$.n@js:result.replace('x','y')+java.ajax('http://a')" }
                }
                """;
        RuleModel model = compiler.compile(om.readTree(json));
        assertEquals(CompileGrade.DEGRADED, model.getHealth().getGrade());
        assertTrue(!model.getHealth().getScriptDeps().isEmpty());
    }

    @Test
    void rejectedGrade_whenWebViewDependency() throws Exception {
        String json = """
                {
                  "bookSourceName": "浏览器源",
                  "bookSourceUrl": "http://example.com",
                  "ruleSearch": { "bookList": ".list", "bookUrl": "a@href,{\\"webView\\":true}" }
                }
                """;
        RuleModel model = compiler.compile(om.readTree(json));
        assertEquals(CompileGrade.REJECTED, model.getHealth().getGrade());
    }
}
