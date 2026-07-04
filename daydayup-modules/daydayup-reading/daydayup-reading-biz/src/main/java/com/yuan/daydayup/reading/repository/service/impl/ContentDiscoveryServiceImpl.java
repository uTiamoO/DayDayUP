package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import com.yuan.daydayup.reading.repository.service.SearchCandidates;
import com.yuan.daydayup.reading.repository.service.WorkAssemblyService;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.springframework.stereotype.Service;

/**
 * 内容发现编排实现：直取搜索结果 → 归一化 → 作品入库。
 */
@Service
public class ContentDiscoveryServiceImpl implements ContentDiscoveryService {

    private final SourceReadingService sourceReadingService;
    private final WorkAssemblyService workAssemblyService;

    public ContentDiscoveryServiceImpl(SourceReadingService sourceReadingService,
                                       WorkAssemblyService workAssemblyService) {
        this.sourceReadingService = sourceReadingService;
        this.workAssemblyService = workAssemblyService;
    }

    @Override
    public WorkDiscoveryResultVO discover(Long sourceId, String keyword, int page) {
        DirectedReadVO read = sourceReadingService.search(sourceId, keyword, page);
        return workAssemblyService.materialize(sourceId, SearchCandidates.from(read.getRecords()));
    }
}
