package com.yuan.daydayup.reading.source.nativeparser.shuba;

import java.util.Map;

/**
 * 69shuba 站点维度映射（分类 / 状态 / 排序），来自站点研究文档。
 *
 * <p>发现页 URL 形如 {@code /novels/{sort}_{categoryCode}_{statusCode}_{page}.htm}。</p>
 */
public final class ShubaMappings {

    /** categoryCode -> 分类名 */
    public static final Map<Integer, String> CATEGORY_NAMES = Map.ofEntries(
            Map.entry(0, "全部"),
            Map.entry(1, "玄幻魔法"),
            Map.entry(2, "修真武侠"),
            Map.entry(3, "言情小说"),
            Map.entry(4, "历史军事"),
            Map.entry(5, "游戏竞技"),
            Map.entry(6, "科幻空间"),
            Map.entry(7, "悬疑惊悚"),
            Map.entry(8, "同人小说"),
            Map.entry(9, "都市小说"),
            Map.entry(10, "官场职场"),
            Map.entry(11, "穿越时空"),
            Map.entry(12, "青春校园"));

    /** 默认排序：人气（月点击） */
    public static final String SORT_MONTH_VISIT = "monthvisit";
    /** 排序：推荐（总推荐） */
    public static final String SORT_ALL_VOTE = "allvote";

    /** statusCode: 0 全部 / 1 完本 / 2 连载 */
    public static final int STATUS_ALL = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_ONGOING = 2;

    private ShubaMappings() {
    }

    /**
     * 归一化站点状态到平台状态。
     *
     * @param upstreamText 站点状态文本（如「完本」「连载」）
     * @param statusCode   发现页 URL 中的 statusCode（-1 表示无）
     * @return {@code completed} / {@code serial} / {@code unknown}
     */
    public static String normalizeStatus(String upstreamText, int statusCode) {
        if (upstreamText != null) {
            String t = upstreamText.trim();
            if (t.contains("完本") || t.contains("全本") || t.contains("完结")) {
                return "completed";
            }
            if (t.contains("连载")) {
                return "serial";
            }
        }
        if (statusCode == STATUS_COMPLETED) {
            return "completed";
        }
        if (statusCode == STATUS_ONGOING) {
            return "serial";
        }
        return "unknown";
    }

    /** 分类名归一化（去除多余空白）。 */
    public static String normalizeCategory(String raw) {
        if (raw == null) {
            return null;
        }
        String c = raw.trim();
        return c.isEmpty() ? null : c;
    }
}
