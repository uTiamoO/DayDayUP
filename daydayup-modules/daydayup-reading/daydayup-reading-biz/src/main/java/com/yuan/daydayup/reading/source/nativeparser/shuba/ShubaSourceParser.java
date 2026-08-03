package com.yuan.daydayup.reading.source.nativeparser.shuba;

import com.yuan.daydayup.reading.source.nativeparser.NativeSourceParser;
import com.yuan.daydayup.reading.source.nativeparser.model.NativeFetchPlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 69shuba（69书吧）原生解析器。
 *
 * <p>selector 假设来自站点研究文档（{@code docs/书源/69shuba.json} 仅作参考），每个 selector 由
 * fixture 测试锁定。搜索走 GBK POST 表单；发现/详情/目录/正文走 GET。所有上游 URL 只在内部记录
 * 字段中流转，Public VO 不暴露（由下游装配保证）。</p>
 */
@Component
public class ShubaSourceParser implements NativeSourceParser {

    public static final String SOURCE_KEY = "69shuba";
    public static final String BASE_URL = "https://www.69shuba.com";
    private static final String SEARCH_URL = BASE_URL + "/modules/article/search.php";
    private static final Charset GBK = Charset.forName("GBK");

    /** 详情页内联脚本 tags:'...' 提取 */
    private static final Pattern TAGS_PATTERN = Pattern.compile("tags:\\s*'([^']*)'");
    /** /book/{id}.htm 书籍 id 提取 */
    private static final Pattern BOOK_ID_PATTERN = Pattern.compile("/book/(\\d+)\\.htm");
    /** 源特定正文清洗（对齐参考 replaceRegex，保守只删已知广告/域名/尾注） */
    private static final Pattern CONTENT_CLEANUP = Pattern.compile(
            "\\s*[（(]?本章完[）)]?\\s*$|新.{0,2}书吧|吧书.{0,2}新|请记住本书首发域名.*|www\\.69shuba\\.com|loadAdv\\([\\d, ]*\\);?");

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public String baseUrl() {
        return BASE_URL;
    }

    // ── 搜索 ───────────────────────────────────────────────────────

    @Override
    public NativeFetchPlan searchPlan(String keyword, int page) {
        // 搜索表单体使用 GBK 编码：searchkey=<gbk>&searchtype=all
        String encoded = URLEncoder.encode(keyword == null ? "" : keyword, GBK);
        return NativeFetchPlan.builder()
                .url(SEARCH_URL)
                .method("POST")
                .charset("GBK")
                .body("searchkey=" + encoded + "&searchtype=all")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=GBK")
                .build();
    }

    @Override
    public List<Map<String, String>> parseSearch(Document doc) {
        return parseListItems(doc.select(".newbox li"));
    }

    // ── 发现（分类/排行榜） ─────────────────────────────────────────

    /**
     * 发现页 URL：{@code /novels/{sort}_{categoryCode}_{statusCode}_{page}.htm}。
     * 例：sort=monthvisit, cat=9, status=2, page=3 -> {@code /novels/monthvisit_9_2_3.htm}
     */
    public static String discoveryUrl(String sort, int categoryCode, int statusCode, int page) {
        String safeSort = (sort == null || sort.isBlank()) ? ShubaMappings.SORT_MONTH_VISIT : sort;
        int safePage = Math.max(1, page);
        return BASE_URL + "/novels/" + safeSort + "_" + categoryCode + "_" + statusCode + "_" + safePage + ".htm";
    }

    public NativeFetchPlan discoveryPlan(String sort, int categoryCode, int statusCode, int page) {
        return NativeFetchPlan.get(discoveryUrl(sort, categoryCode, statusCode, page));
    }

    /** 发现页与搜索页结构一致，列表根为 {@code #article_list_content li}。 */
    public List<Map<String, String>> parseDiscovery(Document doc) {
        return parseListItems(doc.select("#article_list_content li"));
    }

    // ── 详情 ───────────────────────────────────────────────────────

    @Override
    public NativeFetchPlan detailPlan(String bookUrl) {
        return NativeFetchPlan.get(bookUrl);
    }

    @Override
    public Map<String, String> parseDetail(Document doc, String rawHtml) {
        Map<String, String> record = new LinkedHashMap<>();
        putIfPresent(record, "name", metaContent(doc, "book_name"));
        putIfPresent(record, "author", metaContent(doc, "author"));
        putIfPresent(record, "coverUrl", metaContent(doc, "image"));
        String category = ShubaMappings.normalizeCategory(metaContent(doc, "category"));
        putIfPresent(record, "kind", category);
        putIfPresent(record, "updateTime", metaContent(doc, "update_time"));
        putIfPresent(record, "lastChapter", metaContent(doc, "latest_chapter_name"));

        // 状态：meta 无独立字段时按文本归一化；找不到则 unknown
        String statusText = metaContent(doc, "status");
        record.put("status", ShubaMappings.normalizeStatus(statusText, -1));

        // 字数：.booknav2 第 3 个 p，去掉 | 之后的后缀
        Elements navP = doc.select(".booknav2 p");
        if (navP.size() >= 3) {
            String words = navP.get(2).text();
            int bar = words.indexOf('|');
            putIfPresent(record, "wordCount", (bar >= 0 ? words.substring(0, bar) : words).trim());
        }

        // 标签：内联脚本 tags:'a|b|c'
        if (rawHtml != null) {
            Matcher m = TAGS_PATTERN.matcher(rawHtml);
            if (m.find()) {
                String tags = m.group(1).replace('|', ' ').trim().replaceAll("\\s+", " ");
                putIfPresent(record, "tags", tags);
            }
        }

        // 简介：.navtxt 第 1 个 p 的 textNodes，保留段落换行
        Element navtxt = doc.selectFirst(".navtxt p");
        if (navtxt != null) {
            putIfPresent(record, "intro", textNodes(navtxt).stripLeading());
        }

        // 目录 URL：.more-btn@href 优先，退回 .addbtn a:first@href
        String tocUrl = attrOf(doc.selectFirst(".more-btn"), "href");
        if (tocUrl == null) {
            tocUrl = attrOf(doc.selectFirst(".addbtn a"), "href");
        }
        putIfPresent(record, "tocUrl", tocUrl);
        return record;
    }

    // ── 目录 ───────────────────────────────────────────────────────

    @Override
    public NativeFetchPlan tocPlan(String tocUrl) {
        return NativeFetchPlan.get(tocUrl);
    }

    @Override
    public List<Map<String, String>> parseToc(Document doc) {
        List<Map<String, String>> chapters = new ArrayList<>();
        // #catalog li a：DOM 顺序即章节序（跳过 catalog 容器本身）
        for (Element a : doc.select("#catalog li a")) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            Map<String, String> ch = new LinkedHashMap<>();
            ch.put("chapterName", a.text().trim());
            ch.put("chapterUrl", href.trim());
            chapters.add(ch);
        }
        return chapters;
    }

    // ── 正文 ───────────────────────────────────────────────────────

    @Override
    public NativeFetchPlan contentPlan(String contentUrl) {
        return NativeFetchPlan.get(contentUrl);
    }

    @Override
    public Map<String, String> parseContent(Document doc) {
        Map<String, String> record = new LinkedHashMap<>();
        Element txtnav = doc.selectFirst(".txtnav");
        if (txtnav == null) {
            record.put("content", "");
            return record;
        }
        String raw = textNodes(txtnav);
        record.put("content", cleanupContent(raw));
        return record;
    }

    /** 源特定清洗：逐行去广告/域名/尾注，保留中文段落。 */
    static String cleanupContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] lines = raw.split("\n");
        List<String> kept = new ArrayList<>(lines.length);
        for (String line : lines) {
            String cleaned = CONTENT_CLEANUP.matcher(line).replaceAll("").trim();
            if (!cleaned.isEmpty()) {
                kept.add(cleaned);
            }
        }
        return String.join("\n", kept);
    }

    // ── 公共列表解析（搜索/发现同构） ───────────────────────────────

    private List<Map<String, String>> parseListItems(Elements items) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Element li : items) {
            Element titleLink = li.selectFirst("h3 a");
            if (titleLink == null) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            record.put("name", titleLink.text().trim());
            putIfPresent(record, "bookUrl", nullToEmpty(titleLink.attr("href")).trim());

            Elements labels = li.select(".labelbox label");
            if (!labels.isEmpty()) {
                putIfPresent(record, "author", labels.get(0).text().trim());
            }
            Element img = li.selectFirst("img");
            if (img != null) {
                String cover = img.attr("data-src");
                if (cover == null || cover.isBlank()) {
                    cover = img.attr("src");
                }
                putIfPresent(record, "coverUrl", nullToEmpty(cover).trim());
            }
            Element intro = li.selectFirst(".ellipsis_2");
            if (intro != null) {
                putIfPresent(record, "intro", intro.text().trim());
            }
            Element zxzj = li.selectFirst(".zxzj p");
            if (zxzj != null) {
                putIfPresent(record, "lastChapter", zxzj.ownText().trim());
            }
            list.add(record);
        }
        return list;
    }

    // ── selector 辅助 ──────────────────────────────────────────────

    /** meta[property$=xxx]@content */
    private static String metaContent(Document doc, String propertySuffix) {
        Element el = doc.selectFirst("meta[property$=" + propertySuffix + "]");
        return el == null ? null : el.attr("content");
    }

    /** 取直接文本子节点，按顺序用换行连接（对齐 Legado {@code @textNodes} 语义）。 */
    private static String textNodes(Element el) {
        StringBuilder sb = new StringBuilder();
        for (Node node : el.childNodes()) {
            if (node instanceof TextNode tn) {
                String t = tn.getWholeText();
                if (t != null && !t.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(t.strip());
                }
            } else if (node instanceof Element childEl && "br".equalsIgnoreCase(childEl.tagName())) {
                // <br> 视为换行；连续 <br> 由后续 blank 过滤
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String attrOf(Element el, String attr) {
        if (el == null) {
            return null;
        }
        String v = el.attr(attr);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 从 /book/{id}.htm 提取源内书籍 id（供 sourceWorkKey）。 */
    public static String extractBookId(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = BOOK_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
