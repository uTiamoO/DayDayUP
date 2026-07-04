package com.yuan.daydayup.reading.repository.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ContentNormalizer} 单测：HTML→段落、空白/零宽清理、纯文本保序。
 */
class ContentNormalizerTest {

    @Test
    void htmlBrAndPBecomeLineBreaks() {
        String out = ContentNormalizer.normalize(
                "<p>第一段</p><p>第二段</p>第三行<br>第四行");
        assertEquals("第一段\n第二段\n第三行\n第四行", out);
    }

    @Test
    void stripsTagsAndDecodesEntities() {
        String out = ContentNormalizer.normalize("<div>他说&ldquo;你好&rdquo;<span>世界</span></div>");
        assertFalse(out.contains("<"));
        assertTrue(out.contains("你好"));
        assertTrue(out.contains("世界"));
    }

    @Test
    void plainTextTrimsLinesDropsBlank() {
        String out = ContentNormalizer.normalize("  第一行  \n\n\n  第二行\n   ");
        assertEquals("第一行\n第二行", out);
    }

    @Test
    void fullWidthSpaceAndZeroWidthRemoved() {
        String out = ContentNormalizer.normalize("正​文　内容");
        assertEquals("正文 内容", out);
    }

    @Test
    void blankInputReturnsEmpty() {
        assertEquals("", ContentNormalizer.normalize(null));
        assertEquals("", ContentNormalizer.normalize("   "));
    }
}
