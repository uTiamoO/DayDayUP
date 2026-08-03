package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.repository.service.impl.ChapterSyncServiceImpl;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChapterSyncServiceTest {

    @Test
    void syncTocUsesTocUrlResolvedByRealtimeDetail() {
        WorkDetailSyncService detailSyncService = mock(WorkDetailSyncService.class);
        SourceReadingService sourceReadingService = mock(SourceReadingService.class);
        ChapterAssemblyService chapterAssemblyService = mock(ChapterAssemblyService.class);
        ChapterSyncService service = new ChapterSyncServiceImpl(
                detailSyncService, sourceReadingService, chapterAssemblyService);
        when(detailSyncService.syncDetail(100L, 10L, false))
                .thenReturn(new WorkDetailSyncResult(100L, 10L, true, "/128/#dir"));
        DirectedReadVO read = new DirectedReadVO();
        read.setRecords(List.of(Map.of("chapterName", "第一章", "chapterUrl", "/128/1.html")));
        when(sourceReadingService.toc(10L, "/128/#dir")).thenReturn(read);
        TocSyncResultVO assembled = new TocSyncResultVO();
        assembled.setTocEntries(1);
        when(chapterAssemblyService.syncToc(100L, 10L, true, TocEntry.from(read.getRecords())))
                .thenReturn(assembled);

        TocSyncResultVO result = service.syncToc(100L, 10L);

        assertEquals(1, result.getTocEntries());
        verify(detailSyncService).syncDetail(100L, 10L, false);
        verify(sourceReadingService).toc(10L, "/128/#dir");
    }
}
