package com.yuan.daydayup.reading.source.nativeparser.shuba;

import com.yuan.daydayup.reading.source.nativeparser.CloudflareChallengeDetector;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ShubaSourceParser} fixture 合同测试：锁定 selector、URL 生成、GBK 编码、
 * 状态归一化、正文清洗与挑战检测。fixture HTML 依据站点研究文档假设构造，未保存任何 cookie/验证态。
 */
class ShubaSourceParserTest {

    private final ShubaSourceParser parser = new ShubaSourceParser();

    // ── 挑战检测 ───────────────────────────────────────────────────

    @Test
    void challengeFixtureDetectedAsVerificationRequired() throws IOException {
        String html = fixture("challenge.html");
        assertTrue(CloudflareChallengeDetector.isChallenge(html),
                "Cloudflare 挑战页应被检测为 verification_required");
    }

    @Test
    void searchFixtureNotDetectedAsChallenge() throws IOException {
        String html = fixture("search.html");
        assertFalse(CloudflareChallengeDetector.isChallenge(html),
                "正常搜索页不应被误判为挑战");
    }

    // ── 搜索请求编码 ───────────────────────────────────────────────

    @Test
    void searchPlanEncodesKeywordAsGbkPostForm() {
        NativeFetchPlan plan = parser.searchPlan("斗破苍穹", 1);
        assertEquals("POST", plan.method());
        assertEquals("GBK", plan.charset());
        assertTrue(plan.url().endsWith("/modules/article/search.php"));
        assertTrue(plan.body().contains("searchtype=all"), "表单体必须含 searchtype=all");
        assertTrue(plan.body().startsWith("searchkey="), "表单体必须以 searchkey= 开头");
        // 反解 GBK 应还原关键词
        String key = plan.body().substring("searchkey=".length(), plan.body().indexOf("&"));
        String decoded = URLDecoder.decode(key, Charset.forName("GBK"));
        assertEquals("斗破苍穹", decoded);
    }

    // ── 发现 URL 生成 ──────────────────────────────────────────────

    @Test
    void discoveryUrlFollowsSortCategoryStatusPagePattern() {
        assertEquals("https://www.69shuba.com/novels/monthvisit_9_2_3.htm",
                ShubaSourceParser.discoveryUrl("monthvisit", 9, 2, 3));
    }

    // ── 搜索解析 ───────────────────────────────────────────────────

    @Test
    void parseSearchExtractsCandidates() throws IOException {
        List<Map<String, String>> records = parser.parseSearch(doc("search.html"));
        assertEquals(2, records.size());
        Map<String, String> first = records.get(0);
        assertEquals("玄鉴仙族", first.get("name"));
        assertEquals("一觉睡到自然醒", first.get("author"));
        assertEquals("/book/48214.htm", first.get("bookUrl"));
        assertEquals("第一千零一章 峰回路转", first.get("lastChapter"));
        assertTrue(first.get("coverUrl").contains("48214s.jpg"));
        assertNotNull(first.get("intro"));
    }

    // ── 书籍 id 提取 ───────────────────────────────────────────────

    @Test
    void extractBookIdFromDetailUrl() {
        assertEquals("48214", ShubaSourceParser.extractBookId("https://www.69shuba.com/book/48214.htm"));
        assertEquals("48214", ShubaSourceParser.extractBookId("/book/48214.htm"));
    }

    // ── 详情解析 ───────────────────────────────────────────────────

    @Test
    void parseDetailReadsMetaAndTocUrlAndTags() throws IOException {
        String html = fixture("detail.html");
        Map<String, String> record = parser.parseDetail(Jsoup.parse(html, ShubaSourceParser.BASE_URL), html);
        assertEquals("玄鉴仙族", record.get("name"));
        assertEquals("一觉睡到自然醒", record.get("author"));
        assertEquals("玄幻魔法", record.get("kind"));
        assertEquals("serial", record.get("status"));
        assertEquals("第一千零一章 峰回路转", record.get("lastChapter"));
        assertEquals("/txt/48214/", record.get("tocUrl"));
        assertTrue(record.get("tags").contains("争霸流"), "tags 应从内联脚本正则提取");
        assertNotNull(record.get("intro"));
    }

    // ── 目录解析 ───────────────────────────────────────────────────

    @Test
    void parseTocKeepsDomOrder() throws IOException {
        List<Map<String, String>> toc = parser.parseToc(doc("toc.html"));
        assertEquals(4, toc.size());
        assertEquals("第一章 玄鉴", toc.get(0).get("chapterName"));
        assertEquals("/txt/48214/38200001", toc.get(0).get("chapterUrl"));
        assertEquals("第四章 出山", toc.get(3).get("chapterName"));
    }

    // ── 正文解析与清洗 ─────────────────────────────────────────────

    @Test
    void parseContentCleansAdsAndDomainMarkers() throws IOException {
        Map<String, String> record = parser.parseContent(doc("content.html"));
        String content = record.get("content");
        assertNotNull(content);
        assertFalse(content.contains("www.69shuba.com"), "应删除域名广告");
        assertFalse(content.contains("loadAdv"), "应删除 loadAdv 残留");
        assertFalse(content.contains("本章完"), "应删除本章完尾注");
        assertTrue(content.contains("玄鉴悬于识海之上"), "应保留中文正文段落");
    }

    @Test
    void parseContentEmptyWhenTxtnavMissing() {
        Document empty = Jsoup.parse("<html><body><div>no content</div></body></html>",
                ShubaSourceParser.BASE_URL);
        Map<String, String> record = parser.parseContent(empty);
        assertEquals("", record.get("content"), ".txtnav 缺失应映射为空正文");
    }

    // ── fixture 载入 ───────────────────────────────────────────────

    private Document doc(String name) throws IOException {
        return Jsoup.parse(fixture(name), ShubaSourceParser.BASE_URL);
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/69shuba/" + name)) {
            assertNotNull(in, "fixture 缺失: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
