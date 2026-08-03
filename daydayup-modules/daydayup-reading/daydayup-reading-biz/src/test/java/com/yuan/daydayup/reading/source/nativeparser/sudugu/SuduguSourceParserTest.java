package com.yuan.daydayup.reading.source.nativeparser.sudugu;

import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuduguSourceParserTest {

    private final SuduguSourceParser parser = new SuduguSourceParser();

    @Test
    void searchPlanUsesUtf8GetAndPageParameter() {
        NativeFetchPlan plan = parser.searchPlan("凡人修仙传", 2);

        assertEquals("GET", plan.method());
        assertEquals("UTF-8", plan.charset());
        assertTrue(plan.url().startsWith("https://www.sudugu.org/i/sor.aspx?key="));
        assertTrue(plan.url().endsWith("&p=2"));
        String encoded = plan.url().substring(plan.url().indexOf("key=") + 4, plan.url().lastIndexOf("&p="));
        assertEquals("凡人修仙传", URLDecoder.decode(encoded, StandardCharsets.UTF_8));
    }

    @Test
    void parseSearchExtractsCandidates() throws IOException {
        List<Map<String, String>> records = parser.parseSearch(doc("search.html"));

        assertEquals(2, records.size());
        Map<String, String> first = records.get(0);
        assertEquals("凡人修仙传", first.get("name"));
        assertEquals("忘语", first.get("author"));
        assertEquals("/128/", first.get("bookUrl"));
        assertEquals("已完结", first.get("statusText"));
        assertEquals("仙侠", first.get("kind"));
        assertEquals("新书《玄界之门》", first.get("lastChapter"));
        assertTrue(first.get("coverUrl").contains("cover-128.jpg"));
    }

    @Test
    void parseDetailExtractsBookMetadataAndTocUrl() throws IOException {
        Map<String, String> record = parser.parseDetail(doc("detail.html"), fixture("detail.html"));

        assertEquals("凡人修仙传", record.get("name"));
        assertEquals("忘语", record.get("author"));
        assertEquals("completed", record.get("status"));
        assertEquals("仙侠小说", record.get("kind"));
        assertEquals("741.0万字", record.get("wordCount"));
        assertEquals("新书《玄界之门》", record.get("lastChapter"));
        assertEquals("/128/#dir", record.get("tocUrl"));
        assertEquals("2025-09-16 02:49:41", record.get("updateTime"));
        assertTrue(record.get("intro").contains("普通山村小子"));
    }

    @Test
    void parseTocAndNextPageKeepDomOrder() throws IOException {
        Document doc = doc("toc.html");
        List<Map<String, String>> records = parser.parseToc(doc);

        assertEquals(3, records.size());
        assertEquals("第1章 山边小村", records.get(0).get("chapterName"));
        assertEquals("/128/10643.html", records.get(0).get("chapterUrl"));
        assertEquals("第3章 七玄门", records.get(2).get("chapterName"));
        assertEquals("p-2.html#dir", parser.nextTocUrl(doc));
    }

    @Test
    void parseContentKeepsParagraphsAndExposesOnlyNextPage() throws IOException {
        Document first = doc("content.html");
        Map<String, String> record = parser.parseContent(first);

        assertEquals("第1章 山边小村\n二愣子睁大双眼望着黑屋顶。", record.get("content"));
        assertEquals("/128/10643-2.html", parser.nextContentUrl(first));

        Document last = doc("content-last.html");
        String lastContent = parser.parseContent(last).get("content");
        assertTrue(lastContent.contains("韩立轻轻合上房门。"));
        assertFalse(lastContent.contains("本章完"));
        assertFalse(lastContent.contains("更新不易"));
        assertEquals(null, parser.nextContentUrl(last), "下一章不得被当成正文分页");
    }

    @Test
    void extractBookIdFromBookUrl() {
        assertEquals("128", SuduguSourceParser.extractBookId("https://www.sudugu.org/128/"));
        assertEquals("128", SuduguSourceParser.extractBookId("/128/#dir"));
        assertNotNull(parser.baseUrl());
    }

    private Document doc(String name) throws IOException {
        return Jsoup.parse(fixture(name), SuduguSourceParser.BASE_URL);
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/sudugu/" + name)) {
            assertNotNull(in, "fixture 缺失: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
