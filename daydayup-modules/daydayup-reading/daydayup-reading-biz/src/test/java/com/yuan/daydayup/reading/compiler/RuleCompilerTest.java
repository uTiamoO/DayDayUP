package com.yuan.daydayup.reading.compiler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.compiler.model.CompileGrade;
import com.yuan.daydayup.reading.compiler.model.RuleModel;
import com.yuan.daydayup.reading.compiler.parser.RuleStringParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RuleCompiler} 单测：验证编译体检的三级分级（rulemodel §5.2）。
 */
class RuleCompilerTest {

    private final ObjectMapper om = new ObjectMapper();
    private final RuleCompiler compiler = new RuleCompiler(new RuleStringParser());

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
