package com.yuan.daydayup.reading.source.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.reading.api.vo.SourceImportResultVO;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParserRegistry;
import com.yuan.daydayup.reading.source.nativeparser.maoyan.MaoyanSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.maoyan.MaoyanSourceProperties;
import com.yuan.daydayup.reading.source.nativeparser.sudugu.SuduguSourceParser;
import com.yuan.daydayup.reading.source.service.impl.SourceImportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 书源导入兼容性测试。
 */
class SourceImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importsCustomTopLevelSourceAndRedactsSensitiveHeaders() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper, new NativeSourceParserRegistry(java.util.List.of()));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        when(mapper.selectByUrlIncludeDeleted(eq("http://api.example.com"))).thenReturn(null);

        Files.writeString(tempDir.resolve("custom.json"), """
                {
                  "maoyankanshu": {
                    "sourceName": "maoyankanshu",
                    "sourceUrl": "http://api.example.com",
                    "sourceType": "text",
                    "enable": 1,
                    "weight": "9999",
                    "httpHeaders": {
                      "User-Agent": "okhttp/4.9.2",
                      "Authorization": "Bearer secret-token",
                      "Cookie": "qttoken=cookie-secret",
                      "client-device": "device-secret"
                    },
                    "searchBook": {"list": "$.data", "bookName": "$.name"}
                  }
                }
                """, StandardCharsets.UTF_8);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getInserted());
        verify(mapper).insert(any(SourceDefinition.class));
        org.mockito.ArgumentCaptor<SourceDefinition> captor = org.mockito.ArgumentCaptor.forClass(SourceDefinition.class);
        verify(mapper).insert(captor.capture());
        SourceDefinition source = captor.getValue();
        assertEquals("maoyankanshu", source.getName());
        assertEquals("http://api.example.com", source.getBookSourceUrl());
        assertEquals(9999, source.getPriority());
        assertEquals(1, source.getStatus());
        assertNotNull(source.getRawContent());
        assertTrue(source.getRawContent().contains("<redacted>"));
        assertFalse(source.getRawContent().contains("secret-token"));
        assertFalse(source.getRawContent().contains("cookie-secret"));
        assertFalse(source.getRawContent().contains("device-secret"));
    }

    @Test
    void legadoHeaderTextRedactsClientDevice() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper, new NativeSourceParserRegistry(java.util.List.of()));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        when(mapper.selectByUrlIncludeDeleted(eq("http://api.example.com"))).thenReturn(null);
        Files.writeString(tempDir.resolve("legado-header.json"), """
                [{"bookSourceName":"JSON API","bookSourceUrl":"http://api.example.com",
                  "header":"{'client-device':'device-secret','Authorization':'token-secret'}"}]
                """, StandardCharsets.UTF_8);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getInserted());
        org.mockito.ArgumentCaptor<SourceDefinition> captor = org.mockito.ArgumentCaptor.forClass(SourceDefinition.class);
        verify(mapper).insert(captor.capture());
        assertFalse(captor.getValue().getRawContent().contains("device-secret"));
        assertFalse(captor.getValue().getRawContent().contains("token-secret"));
    }

    @Test
    void skipsUnchangedLegadoSourceByFingerprint() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper, new NativeSourceParserRegistry(java.util.List.of()));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        String json = """
                [{"bookSourceName":"篱笆文学","bookSourceUrl":"https://m.libahao.com","bookSourceType":0,"enabled":true,"weight":0}]
                """;
        Files.writeString(tempDir.resolve("legado.json"), json, StandardCharsets.UTF_8);
        SourceDefinition existing = new SourceDefinition();
        existing.setId(1L);
        existing.setFingerprint(sha256(objectMapper.readTree(json).get(0).toString()));
        when(mapper.selectByUrlIncludeDeleted(eq("https://m.libahao.com"))).thenReturn(existing);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSkipped());
        verify(mapper, never()).insert(any(SourceDefinition.class));
        verify(mapper, never()).updateById(any(SourceDefinition.class));
    }

    @Test
    void knownNativeBaseUrlIsImportedAsNativeSource() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper,
                new NativeSourceParserRegistry(java.util.List.of(new SuduguSourceParser())));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        when(mapper.selectByUrlIncludeDeleted(eq("https://www.sudugu.org"))).thenReturn(null);
        Files.writeString(tempDir.resolve("sudugu.json"), """
                [{"bookSourceName":"速读谷(SUDUGU)","bookSourceUrl":"https://www.sudugu.org/",
                  "bookSourceType":0,"enabled":true,"weight":0}]
                """, StandardCharsets.UTF_8);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getInserted());
        org.mockito.ArgumentCaptor<SourceDefinition> captor = org.mockito.ArgumentCaptor.forClass(SourceDefinition.class);
        verify(mapper).insert(captor.capture());
        SourceDefinition source = captor.getValue();
        assertEquals("https://www.sudugu.org", source.getBookSourceUrl());
        assertEquals("native", source.getOriginType());
        assertEquals("native:sudugu", source.getOriginPath());
        assertEquals("sudugu", source.getTags());
        assertEquals("full", source.getCompileGrade());
    }

    @Test
    void unchangedLegacyRowIsPromotedWhenNativeParserBecomesAvailable() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper,
                new NativeSourceParserRegistry(java.util.List.of(new SuduguSourceParser())));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        String json = """
                [{"bookSourceName":"速读谷(SUDUGU)","bookSourceUrl":"https://www.sudugu.org",
                  "bookSourceType":0,"enabled":true,"weight":0}]
                """;
        Files.writeString(tempDir.resolve("sudugu-existing.json"), json, StandardCharsets.UTF_8);
        SourceDefinition existing = new SourceDefinition();
        existing.setId(9L);
        existing.setOriginType("legado");
        existing.setOriginPath("7298.json");
        existing.setTags("自建书源");
        existing.setFingerprint(sha256(objectMapper.readTree(json).get(0).toString()));
        when(mapper.selectByUrlIncludeDeleted(eq("https://www.sudugu.org"))).thenReturn(existing);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getUpdated());
        assertEquals("native", existing.getOriginType());
        assertEquals("native:sudugu", existing.getOriginPath());
        assertEquals("sudugu", existing.getTags());
        assertEquals("full", existing.getCompileGrade());
        verify(mapper).updateById(existing);
    }

    @Test
    void legadoCommentSuffixIsRemovedBeforeMaoyanNativeClassification() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        MaoyanSourceParser parser = new MaoyanSourceParser(objectMapper, new MaoyanSourceProperties());
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper, new NativeSourceParserRegistry(java.util.List.of(parser)));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        when(mapper.selectByUrlIncludeDeleted(eq("http://api.jmlldsc.com"))).thenReturn(null);
        Files.writeString(tempDir.resolve("maoyan.json"), """
                [{"bookSourceName":"猫眼看书","bookSourceUrl":"http://api.jmlldsc.com##@曦灵",
                  "bookSourceType":0,"enabled":true,"weight":0}]
                """, StandardCharsets.UTF_8);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getInserted());
        org.mockito.ArgumentCaptor<SourceDefinition> captor = org.mockito.ArgumentCaptor.forClass(SourceDefinition.class);
        verify(mapper).insert(captor.capture());
        SourceDefinition source = captor.getValue();
        assertEquals("http://api.jmlldsc.com", source.getBookSourceUrl());
        assertEquals("native", source.getOriginType());
        assertEquals("native:maoyankanshu", source.getOriginPath());
        assertEquals("maoyankanshu", source.getTags());
        assertEquals("full", source.getCompileGrade());
    }

    @Test
    void unchangedMaoyanLegacyRowIsPromotedWhenNativeParserBecomesAvailable() throws Exception {
        SourceDefinitionMapper mapper = mock(SourceDefinitionMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        MaoyanSourceParser parser = new MaoyanSourceParser(objectMapper, new MaoyanSourceProperties());
        SourceImportServiceImpl service = new SourceImportServiceImpl(
                mapper, objectMapper, new NativeSourceParserRegistry(java.util.List.of(parser)));
        ReflectionTestUtils.setField(service, "defaultImportDir", tempDir.toString());
        String json = """
                [{"bookSourceName":"猫眼看书","bookSourceUrl":"http://api.jmlldsc.com",
                  "bookSourceType":0,"enabled":true,"weight":0}]
                """;
        Files.writeString(tempDir.resolve("maoyan-existing.json"), json, StandardCharsets.UTF_8);
        SourceDefinition existing = new SourceDefinition();
        existing.setId(10L);
        existing.setOriginType("legado");
        existing.setOriginPath("7392.json");
        existing.setTags("JSON API");
        existing.setFingerprint(sha256(objectMapper.readTree(json).get(0).toString()));
        when(mapper.selectByUrlIncludeDeleted(eq("http://api.jmlldsc.com"))).thenReturn(existing);

        SourceImportResultVO result = service.importFromDirectory(null);

        assertEquals(1, result.getUpdated());
        assertEquals("native", existing.getOriginType());
        assertEquals("native:maoyankanshu", existing.getOriginPath());
        assertEquals("maoyankanshu", existing.getTags());
        assertEquals("full", existing.getCompileGrade());
        verify(mapper).updateById(existing);
    }

    private static String sha256(String s) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
