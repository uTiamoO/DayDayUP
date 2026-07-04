package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;

/**
 * 内容发现编排（spec §5.2）：指定书源搜索 → 归一化候选 → 作品入库。
 * 桥接 parse-runtime（{@code SourceReadingService}）与 content-repository（{@link WorkAssemblyService}）。
 */
public interface ContentDiscoveryService {

    /**
     * 指定书源搜索并入库为作品。
     *
     * @param sourceId 书源 id
     * @param keyword  关键词
     * @param page     页码（从 1 起）
     */
    WorkDiscoveryResultVO discover(Long sourceId, String keyword, int page);
}
