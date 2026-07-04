package com.yuan.daydayup.reading.repository.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

/**
 * 正文标准化（切片 4 子片 1）：把来源抽取所得正文规整为纯文本段落。
 *
 * <ul>
 *   <li>若含 HTML 标签：{@code <br>/<p>/</div>} 视为换行后去标签、解实体；</li>
 *   <li>按行 trim、丢弃空行、段落间以单个换行分隔；</li>
 *   <li>去除全角空格与常见零宽字符。</li>
 * </ul>
 *
 * <p>不做广告/敏感词处理（那是净化 Pipeline 的职责，子片 2）。</p>
 */
public final class ContentNormalizer {

    private static final Document.OutputSettings PLAIN = new Document.OutputSettings()
            .prettyPrint(false)
            .escapeMode(Entities.EscapeMode.xhtml);

    private ContentNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = looksLikeHtml(raw) ? htmlToText(raw) : raw;
        text = text.replace('　', ' ')                     // 全角空格
                .replaceAll("[\\u200B\\u200C\\uFEFF]", "");     // 零宽字符
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder(text.length());
        for (String line : lines) {
            String t = line.strip();
            if (!t.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    private static boolean looksLikeHtml(String s) {
        return s.indexOf('<') >= 0 && s.indexOf('>') > s.indexOf('<');
    }

    private static String htmlToText(String html) {
        String withBreaks = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n");
        Document doc = Jsoup.parse(withBreaks);
        doc.outputSettings(PLAIN);
        return doc.body().wholeText();
    }
}
