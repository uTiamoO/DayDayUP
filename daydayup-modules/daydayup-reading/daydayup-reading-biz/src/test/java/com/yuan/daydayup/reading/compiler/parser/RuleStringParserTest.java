package com.yuan.daydayup.reading.compiler.parser;

import com.yuan.daydayup.reading.compiler.model.ExtractEngine;
import com.yuan.daydayup.reading.compiler.model.RuleChain;
import com.yuan.daydayup.reading.compiler.model.RuleStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RuleStringParser} 单测：基于两个固定回归样例（篱笆文学 HTML / 猫眼看书 JSON+crypto）
 * 的真实规则串，验证 jsoup 方言、jsonpath、模板、@js: 原生收敛。
 */
class RuleStringParserTest {

    private final RuleStringParser parser = new RuleStringParser();

    @Test
    void jsoup_classSelector() {
        RuleChain c = parser.parse(".book-item");
        assertEquals(1, c.getSteps().size());
        RuleStep s = c.getSteps().get(0);
        assertEquals(RuleStep.StepType.SELECTOR, s.getType());
        assertEquals(ExtractEngine.JSOUP, s.getEngine());
        assertEquals(".book-item", s.getSelector());
        assertNull(s.getAttribute());
    }

    @Test
    void jsoup_selectorWithTextAttribute() {
        RuleStep s = parser.parse(".book-title@text").getSteps().get(0);
        assertEquals(".book-title", s.getSelector());
        assertEquals("text", s.getAttribute());
    }

    @Test
    void jsoup_bareAttribute() {
        RuleStep s = parser.parse("href").getSteps().get(0);
        assertEquals("", s.getSelector());
        assertEquals("href", s.getAttribute());
    }

    @Test
    void jsoup_attributeEqualsSelector() {
        RuleStep s = parser.parse("[property=\"og:novel:author\"]@content").getSteps().get(0);
        assertEquals("[property=\"og:novel:author\"]", s.getSelector());
        assertEquals("content", s.getAttribute());
    }

    @Test
    void jsoup_indexSelector() {
        // 篱笆 ruleToc.chapterList: .chapter-list.1@a  → 取第 1 个 .chapter-list 下的 a
        RuleStep s = parser.parse(".chapter-list.1@a").getSteps().get(0);
        assertEquals(".chapter-list", s.getSelector());
        assertEquals(Integer.valueOf(1), s.getIndex());
        assertEquals("a", s.getAttribute());
    }

    @Test
    void jsoup_textMatchSelector() {
        // 篱笆 ruleContent.nextContentUrl: text.下一页@href
        RuleStep s = parser.parse("text.下一页@href").getSteps().get(0);
        assertEquals("text.下一页", s.getSelector());
        assertEquals("href", s.getAttribute());
    }

    @Test
    void jsonpath_list() {
        RuleStep s = parser.parse("$.data[*]").getSteps().get(0);
        assertEquals(ExtractEngine.JSONPATH, s.getEngine());
        assertEquals("$.data[*]", s.getSelector());
    }

    @Test
    void template_withEmbeddedJsonpath() {
        // 猫眼 ruleSearch.bookUrl: /novel/{{$.novelId}}?isSearch=1
        RuleStep s = parser.parse("/novel/{{$.novelId}}?isSearch=1").getSteps().get(0);
        assertEquals(ExtractEngine.TEMPLATE, s.getEngine());
    }

    @Test
    void jsonpath_withNativeAesConvergence() {
        // 猫眼 ruleToc.chapterUrl: $.path@js:java.aesBase64DecodeToString(result,"KEY","AES/CBC/PKCS5Padding","IV")
        String rule = "$.path@js:java.aesBase64DecodeToString(result,\"f041c49714d39908\",\"AES/CBC/PKCS5Padding\",\"0123456789abcdef\")";
        RuleChain c = parser.parse(rule);
        assertEquals(2, c.getSteps().size());

        RuleStep sel = c.getSteps().get(0);
        assertEquals(ExtractEngine.JSONPATH, sel.getEngine());
        assertEquals("$.path", sel.getSelector());

        RuleStep pp = c.getSteps().get(1);
        assertEquals(RuleStep.StepType.POST_PROCESSOR, pp.getType());
        assertEquals("aesDecrypt", pp.getPostProcessor());
        assertEquals("f041c49714d39908", pp.getParams().get("key"));
        assertEquals("AES/CBC/PKCS5Padding", pp.getParams().get("transformation"));
        assertEquals("0123456789abcdef", pp.getParams().get("iv"));
        // 已收敛为原生，不应留下 SCRIPT 步骤
        assertTrue(c.getSteps().stream().noneMatch(s -> s.getType() == RuleStep.StepType.SCRIPT));
    }

    @Test
    void arbitraryScript_staysAsScriptStep() {
        RuleChain c = parser.parse("$.x@js:result.replace('a','b')+source.getVariable('k')");
        assertTrue(c.hasScriptStep());
    }
}
