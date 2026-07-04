package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.ContentSnapshotVO;

/**
 * 正文抓取与快照服务（content-repository，spec §5.4）。
 * 子片 1 只做 raw + normalized 两层；sanitized 由净化 Pipeline（子片 2）补齐。
 */
public interface ContentFetchService {

    /**
     * 抓取某章节某来源的正文并落快照（cache-first：命中未变更则复用）。
     *
     * @param chapterId    统一章节 id
     * @param sourceId     来源书源 id
     * @param forceRefresh 是否强制回源（跳过缓存）
     */
    ContentSnapshotVO fetchAndStore(Long chapterId, Long sourceId, boolean forceRefresh);
}
