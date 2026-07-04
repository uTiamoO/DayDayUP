package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.repository.support.MatchKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 目录抽取记录 → 归一化章节条目（Legado ruleToc 字段名，含常见别名）。
 *
 * @param title 章节标题
 * @param url   来源章节正文 URL
 */
public record TocEntry(String title, String url) {

    public static List<TocEntry> from(List<Map<String, String>> records) {
        List<TocEntry> entries = new ArrayList<>();
        if (records == null) {
            return entries;
        }
        for (Map<String, String> r : records) {
            String url = MatchKeys.pick(r, "chapterUrl", "url", "href");
            if (url == null) {
                continue;   // 无正文 URL 的条目跳过，保证 chapterIndex 连续
            }
            String title = MatchKeys.pick(r, "chapterName", "name", "title");
            entries.add(new TocEntry(title == null ? "" : title, url));
        }
        return entries;
    }
}
