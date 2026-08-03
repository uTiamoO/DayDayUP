package com.yuan.daydayup.reading.source.nativeparser.sudugu;

import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 速读谷（sudugu.org）原生书源解析器。 */
@Component
public class SuduguSourceParser implements NativeSourceParser {

    public static final String SOURCE_KEY = "sudugu";
    public static final String BASE_URL = "https://www.sudugu.org";
    private static final String SEARCH_URL = BASE_URL + "/i/sor.aspx";

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public String baseUrl() {
        return BASE_URL;
    }

    @Override
    public NativeFetchPlan searchPlan(String keyword, int page) {
        String encoded = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8);
        return NativeFetchPlan.builder()
                .url(SEARCH_URL + "?key=" + encoded + "&p=" + Math.max(1, page))
                .charset(StandardCharsets.UTF_8.name())
                .build();
    }

    @Override
    public List<Map<String, String>> parseSearch(Document doc) {
        return parseItems(doc.select(".container > .item"));
    }

    @Override
    public NativeFetchPlan detailPlan(String bookUrl) {
        return NativeFetchPlan.get(bookUrl);
    }

    @Override
    public Map<String, String> parseDetail(Document doc, String rawHtml) {
        Map<String, String> record = new LinkedHashMap<>();
        Element item = doc.selectFirst(".container > .item");
        if (item == null) {
            return record;
        }
        Element itemText = item.selectFirst(".itemtxt");
        if (itemText == null) {
            return record;
        }
        Element title = itemText.selectFirst("h1 > a");
        putIfPresent(record, "name", text(title));
        putIfPresent(record, "coverUrl", secureCover(attr(directCover(item), "src")));
        putIfPresent(record, "author", author(itemText));

        Elements summary = directSummarySpans(itemText);
        String statusText = summary.size() > 0 ? summary.get(0).text() : null;
        record.put("status", normalizeStatus(statusText));
        if (summary.size() > 1) {
            putIfPresent(record, "kind", summary.get(1).text());
        }

        putIfPresent(record, "wordCount", text(itemText.selectFirst("h1 > i")));
        putIfPresent(record, "lastChapter", text(itemText.selectFirst("ul > li:first-child > a")));
        Element intro = doc.selectFirst(".container > .des.bb");
        putIfPresent(record, "intro", intro == null ? null : intro.text().trim());

        Element directory = doc.selectFirst("h2#dir");
        if (directory != null) {
            putIfPresent(record, "tocUrl", attr(directory.selectFirst("a"), "href"));
            String update = text(directory.selectFirst("span"));
            putIfPresent(record, "updateTime", stripPrefix(update, "更新时间："));
        }
        return record;
    }

    @Override
    public NativeFetchPlan tocPlan(String tocUrl) {
        return NativeFetchPlan.get(tocUrl);
    }

    @Override
    public List<Map<String, String>> parseToc(Document doc) {
        List<Map<String, String>> chapters = new ArrayList<>();
        for (Element link : doc.select("#list > ul > li > a")) {
            String href = attr(link, "href");
            if (href == null) {
                continue;
            }
            Map<String, String> chapter = new LinkedHashMap<>();
            chapter.put("chapterName", link.text().trim());
            chapter.put("chapterUrl", href);
            chapters.add(chapter);
        }
        return chapters;
    }

    @Override
    public String nextTocUrl(Document doc) {
        Element next = doc.selectFirst("#pages > a.gr");
        return next != null && "下一页".equals(next.text().trim()) ? attr(next, "href") : null;
    }

    @Override
    public NativeFetchPlan contentPlan(String contentUrl) {
        return NativeFetchPlan.get(contentUrl);
    }

    @Override
    public Map<String, String> parseContent(Document doc) {
        Map<String, String> record = new LinkedHashMap<>();
        Element container = doc.selectFirst(".con");
        if (container == null) {
            record.put("content", "");
            return record;
        }
        List<String> paragraphs = new ArrayList<>();
        List<Element> paragraphsNodes = new ArrayList<>();
        for (Element child : container.children()) {
            if ("p".equals(child.tagName())) {
                paragraphsNodes.add(child);
            }
        }
        if (paragraphsNodes.isEmpty()) {
            addContentLine(paragraphs, container.text());
        } else {
            for (Element node : paragraphsNodes) {
                addContentLine(paragraphs, node.text());
            }
        }
        record.put("content", String.join("\n", paragraphs));
        return record;
    }

    @Override
    public String nextContentUrl(Document doc) {
        for (Element link : doc.select(".prenext a")) {
            if ("下一页".equals(link.text().trim())) {
                return attr(link, "href");
            }
        }
        return null;
    }

    private static List<Map<String, String>> parseItems(Elements items) {
        List<Map<String, String>> records = new ArrayList<>();
        for (Element item : items) {
            Element itemText = item.selectFirst(".itemtxt");
            Element title = itemText == null ? null : itemText.selectFirst("h3 > a, h1 > a");
            if (title == null) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            record.put("name", title.text().trim());
            putIfPresent(record, "bookUrl", attr(title, "href"));
            putIfPresent(record, "coverUrl", secureCover(attr(directCover(item), "src")));
            putIfPresent(record, "author", author(itemText));
            Elements summary = directSummarySpans(itemText);
            if (summary.size() > 0) {
                putIfPresent(record, "statusText", summary.get(0).text());
            }
            if (summary.size() > 1) {
                putIfPresent(record, "kind", summary.get(1).text());
            }
            putIfPresent(record, "lastChapter", text(itemText.selectFirst("ul > li:first-child > a")));
            records.add(record);
        }
        return records;
    }

    private static Elements directSummarySpans(Element itemText) {
        for (Element child : itemText.children()) {
            if ("p".equals(child.tagName()) && !child.select("span").isEmpty()) {
                return child.select("span");
            }
        }
        return new Elements();
    }

    private static Element directCover(Element item) {
        for (Element child : item.children()) {
            if ("a".equals(child.tagName())) {
                Element image = child.selectFirst("img");
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    private static String author(Element itemText) {
        return stripPrefix(text(itemText.selectFirst("a[href^=/zuozhe/]")), "作者：");
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "unknown";
        }
        if (status.contains("完结") || status.contains("完本") || status.contains("全本")) {
            return "completed";
        }
        if (status.contains("连载")) {
            return "serial";
        }
        return "unknown";
    }

    private static void addContentLine(List<String> paragraphs, String value) {
        if (value == null) {
            return;
        }
        String line = value.trim();
        if (line.isEmpty() || line.matches("[（(]?本章完[）)]?") || line.contains("更新不易，记得分享网")) {
            return;
        }
        paragraphs.add(line);
    }

    private static String secureCover(String url) {
        if (url != null && url.startsWith("http://www.sudugu.org/")) {
            return "https://www.sudugu.org/" + url.substring("http://www.sudugu.org/".length());
        }
        return url;
    }

    private static String text(Element element) {
        return element == null ? null : element.text().trim();
    }

    private static String attr(Element element, String name) {
        if (element == null) {
            return null;
        }
        String value = element.attr(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stripPrefix(String value, String prefix) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.startsWith(prefix) ? trimmed.substring(prefix.length()).trim() : trimmed;
    }

    private static void putIfPresent(Map<String, String> record, String key, String value) {
        if (value != null && !value.isBlank()) {
            record.put(key, value);
        }
    }

    /** 从 /{bookId}/ 详情 URL 提取源内书籍 id。 */
    public static String extractBookId(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String path = URI.create(url).getPath();
            if (path == null) {
                return null;
            }
            for (String segment : path.split("/")) {
                if (segment.matches("\\d+")) {
                    return segment;
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
