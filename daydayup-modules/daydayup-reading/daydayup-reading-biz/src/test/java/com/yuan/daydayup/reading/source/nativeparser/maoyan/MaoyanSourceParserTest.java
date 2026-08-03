package com.yuan.daydayup.reading.source.nativeparser.maoyan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaoyanSourceParserTest {

    private final MaoyanSourceProperties properties = new MaoyanSourceProperties();
    private final MaoyanSourceParser parser = new MaoyanSourceParser(new ObjectMapper(), properties);

    @Test
    void searchPlanUsesCanonicalJsonApiAndSafeHeaders() {
        properties.setAuthorization("test-authorization");
        properties.setClientDevice("test-device");

        NativeFetchPlan plan = parser.searchPlan("我不是戏神", 2);

        assertEquals("GET", plan.method());
        assertEquals("UTF-8", plan.charset());
        assertTrue(plan.url().startsWith(MaoyanSourceParser.BASE_URL + "/search?keyword="));
        assertTrue(plan.url().endsWith("&type=2&page=2"));
        String encoded = plan.url().substring(plan.url().indexOf("keyword=") + 8, plan.url().indexOf("&type="));
        assertEquals("我不是戏神", URLDecoder.decode(encoded, StandardCharsets.UTF_8));
        assertEquals("test-authorization", plan.headers().get("Authorization"));
        assertEquals("test-device", plan.headers().get("client-device"));
        assertEquals("app.maoyankanshu.novel", plan.headers().get("client-name"));
    }

    @Test
    void blankCredentialsAreNotSent() {
        NativeFetchPlan plan = parser.searchPlan("我不是戏神", 1);

        assertFalse(plan.headers().containsKey("Authorization"));
        assertFalse(plan.headers().containsKey("client-device"));
    }

    @Test
    void parseSearchExtractsPlatformFields() throws IOException {
        String raw = fixture("search.json");

        List<Map<String, String>> records = parser.parseSearch(Jsoup.parse(raw), raw);

        assertEquals(1, records.size());
        Map<String, String> first = records.get(0);
        assertEquals("我不是戏神", first.get("name"));
        assertEquals("三九音域", first.get("author"));
        assertEquals("/novel/697647?isSearch=1", first.get("bookUrl"));
        assertEquals("都市", first.get("kind"));
        assertEquals("3210000", first.get("wordCount"));
    }

    @Test
    void detailPlanNormalizesSourceWorkRefAndParsesMetadata() throws IOException {
        NativeFetchPlan plan = parser.detailPlan("/novel/697647?isSearch=1");
        String raw = fixture("detail.json");

        Map<String, String> record = parser.parseDetail(Jsoup.parse(raw), raw);

        assertEquals(MaoyanSourceParser.BASE_URL + "/novel/697647?isSearch=0", plan.url());
        assertEquals("我不是戏神", record.get("name"));
        assertEquals("三九音域", record.get("author"));
        assertEquals("ongoing", record.get("status"));
        assertEquals("/novel/697647/chapters", record.get("tocUrl"));
        assertEquals("第 1200 章 戏台之后", record.get("lastChapter"));
    }

    @Test
    void detailPlanDoesNotTreatAbsoluteUrlPortAsNovelId() {
        NativeFetchPlan plan = parser.detailPlan("http://127.0.0.1:50664/novel/697647?isSearch=1");

        assertEquals(MaoyanSourceParser.BASE_URL + "/novel/697647?isSearch=0", plan.url());
    }

    @Test
    void tocDecryptsVerifiedCiphertextAndKeepsOrder() throws IOException {
        String raw = fixture("toc.json");

        List<Map<String, String>> records = parser.parseToc(Jsoup.parse(raw), raw);

        assertEquals(1, records.size());
        assertEquals("第 1 章 赤色流星", records.get(0).get("chapterName"));
        assertEquals("http://api.jmlldsc.com/697/697647/91038.json", records.get(0).get("chapterUrl"));
        assertEquals("2388", records.get(0).get("wordCount"));
    }

    @Test
    void invalidCiphertextFailsWithReadingRuntimeErrorWithoutLeakingInput() {
        String raw = "{\"data\":{\"list\":[{\"chapterName\":\"坏章节\",\"path\":\"not-a-cipher\"}]}}";

        BizException error = assertThrows(BizException.class,
                () -> parser.parseToc(Jsoup.parse(raw), raw));

        assertEquals(60202, error.getCode());
        assertFalse(error.getMessage().contains("not-a-cipher"));
    }

    @Test
    void tocRejectsDecryptedUrlOutsideCanonicalHost() throws Exception {
        String encrypted = encrypt("http://evil.jmlldsc.com/content/1.json");
        String raw = "{\"data\":{\"list\":[{\"chapterName\":\"坏跳转\",\"path\":\"" + encrypted + "\"}]}}";

        BizException error = assertThrows(BizException.class,
                () -> parser.parseToc(Jsoup.parse(raw), raw));

        assertEquals(60202, error.getCode());
        assertFalse(error.getMessage().contains("evil.jmlldsc.com"));
    }

    @Test
    void contentUsesRawJsonBodyWithoutJsoupRewriting() throws IOException {
        String raw = fixture("content.json");

        Map<String, String> record = parser.parseContent(Jsoup.parse(raw), raw);

        assertEquals("<p>第一段不能被 Jsoup 吞掉。</p>\n第二段保留原始换行。", record.get("content"));
    }

    @Test
    void invalidJsonFailsWithReadingRuntimeError() {
        BizException error = assertThrows(BizException.class,
                () -> parser.parseSearch(Jsoup.parse("not-json"), "not-json"));

        assertEquals(60202, error.getCode());
    }

    @Test
    void missingExpectedDataNodeIsNotTreatedAsSuccessfulEmptyResult() {
        BizException error = assertThrows(BizException.class,
                () -> parser.parseSearch(Jsoup.parse("{\"code\":401,\"msg\":\"token expired\"}"),
                        "{\"code\":401,\"msg\":\"token expired\"}"));

        assertEquals(60202, error.getCode());
        assertFalse(error.getMessage().contains("token expired"));
    }

    @Test
    void explicitEmptySearchArrayRemainsAValidEmptyResult() {
        assertTrue(parser.parseSearch(Jsoup.parse("{\"data\":[]}"), "{\"data\":[]}").isEmpty());
    }

    private static String encrypt(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec("f041c49714d39908".getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("fixtures/maoyan/" + name)) {
            assertNotNull(in, "fixture 缺失: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
