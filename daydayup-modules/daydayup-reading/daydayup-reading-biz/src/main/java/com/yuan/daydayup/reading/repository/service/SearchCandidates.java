package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.repository.support.MatchKeys;

import java.util.List;
import java.util.Map;

/**
 * 抽取记录 → {@link SearchCandidate} 映射（Legado ruleSearch 字段名，含常见别名）。
 */
public final class SearchCandidates {

    private SearchCandidates() {
    }

    public static SearchCandidate from(Map<String, String> record) {
        return new SearchCandidate(
                MatchKeys.pick(record, "name", "bookName", "title"),
                MatchKeys.pick(record, "author", "authorName"),
                MatchKeys.pick(record, "kind", "category", "categoryName"),
                MatchKeys.pick(record, "coverUrl", "cover", "img"),
                MatchKeys.pick(record, "intro", "description", "desc"),
                MatchKeys.pick(record, "lastChapter", "latestChapter", "updateTime"),
                MatchKeys.pick(record, "bookUrl", "url", "detailUrl"));
    }

    public static List<SearchCandidate> from(List<Map<String, String>> records) {
        return records == null ? List.of() : records.stream().map(SearchCandidates::from).toList();
    }
}
