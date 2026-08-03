package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.repository.service.ChapterAssemblyService;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.TocEntry;
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncResult;
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncService;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.springframework.stereotype.Service;

/**
 * 目录同步编排实现：先实时解析详情页获得目录 URL，再抓取目录并物化。
 */
@Service
public class ChapterSyncServiceImpl implements ChapterSyncService {

    private final WorkDetailSyncService workDetailSyncService;
    private final SourceReadingService sourceReadingService;
    private final ChapterAssemblyService chapterAssemblyService;

    public ChapterSyncServiceImpl(WorkDetailSyncService workDetailSyncService,
                                  SourceReadingService sourceReadingService,
                                  ChapterAssemblyService chapterAssemblyService) {
        this.workDetailSyncService = workDetailSyncService;
        this.sourceReadingService = sourceReadingService;
        this.chapterAssemblyService = chapterAssemblyService;
    }

    @Override
    public TocSyncResultVO syncToc(Long workId, Long sourceId) {
        long start = System.currentTimeMillis();
        WorkDetailSyncResult detail = workDetailSyncService.syncDetail(workId, sourceId, false);
        DirectedReadVO read = sourceReadingService.toc(detail.sourceId(), detail.tocUrl());

        TocSyncResultVO result = chapterAssemblyService.syncToc(
                workId, detail.sourceId(), detail.primarySource(), TocEntry.from(read.getRecords()));
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }
}
