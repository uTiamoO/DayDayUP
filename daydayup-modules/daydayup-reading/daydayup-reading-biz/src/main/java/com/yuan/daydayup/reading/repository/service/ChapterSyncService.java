package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;

/**
 * 目录同步编排（spec §5.3）：定位来源书籍 URL → 定向抓目录 → 章节资产化。
 * 桥接 parse-runtime（{@code SourceReadingService}）与 content-repository（{@link ChapterAssemblyService}）。
 */
public interface ChapterSyncService {

    /**
     * 同步某作品某来源的目录。tocUrl 取自该来源的 {@code WorkSourceBinding.sourceBookUrl}。
     *
     * @param workId   统一作品 id
     * @param sourceId 来源书源 id
     */
    TocSyncResultVO syncToc(Long workId, Long sourceId);
}
