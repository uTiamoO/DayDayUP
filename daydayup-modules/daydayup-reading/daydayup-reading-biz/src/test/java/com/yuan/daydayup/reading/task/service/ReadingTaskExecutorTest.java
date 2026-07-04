package com.yuan.daydayup.reading.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.compiler.service.RuleCompileService;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import com.yuan.daydayup.reading.source.service.SourceImportService;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.service.impl.ReadingTaskExecutorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReadingTaskExecutorTest {

    private SourceImportService sourceImportService;
    private RuleCompileService ruleCompileService;
    private ContentDiscoveryService contentDiscoveryService;
    private ChapterSyncService chapterSyncService;
    private ContentFetchService contentFetchService;
    private ContentSanitizeService contentSanitizeService;
    private ReadingTaskExecutor executor;

    @BeforeEach
    void setUp() {
        sourceImportService = mock(SourceImportService.class);
        ruleCompileService = mock(RuleCompileService.class);
        contentDiscoveryService = mock(ContentDiscoveryService.class);
        chapterSyncService = mock(ChapterSyncService.class);
        contentFetchService = mock(ContentFetchService.class);
        contentSanitizeService = mock(ContentSanitizeService.class);
        executor = new ReadingTaskExecutorImpl(sourceImportService, ruleCompileService, contentDiscoveryService,
                chapterSyncService, contentFetchService, contentSanitizeService, new ObjectMapper());
    }

    @Test
    void dispatchesSourceCompileToCompileOne() {
        executor.execute(task("source_compile", "{\"sourceId\":10}"));

        verify(ruleCompileService).compileOne(10L);
    }

    @Test
    void dispatchesSourceCompileAll() {
        executor.execute(task("source_compile", "{\"all\":true}"));

        verify(ruleCompileService).compileAllEnabled();
    }

    @Test
    void dispatchesTocSync() {
        executor.execute(task("toc_sync", "{\"workId\":1,\"sourceId\":2}"));

        verify(chapterSyncService).syncToc(1L, 2L);
    }

    @Test
    void dispatchesContentFetchAndSanitize() {
        executor.execute(task("content_fetch", "{\"chapterId\":3,\"sourceId\":4,\"forceRefresh\":true}"));
        executor.execute(task("content_sanitize", "{\"chapterId\":3,\"sourceId\":4}"));

        verify(contentFetchService).fetchAndStore(3L, 4L, true);
        verify(contentSanitizeService).sanitize(3L, 4L);
    }

    @Test
    void dispatchesDiscoveryAndImport() {
        executor.execute(task("work_discovery", "{\"sourceId\":8,\"keyword\":\"斗破\",\"page\":2}"));
        executor.execute(task("source_import", "{\"dir\":\"docs/source\"}"));

        verify(contentDiscoveryService).discover(8L, "斗破", 2);
        verify(sourceImportService).importFromDirectory("docs/source");
    }

    @Test
    void missingRequiredPayloadFieldFailsFast() {
        assertThrows(BizException.class, () -> executor.execute(task("toc_sync", "{\"workId\":1}")));
    }

    private ReadingTask task(String type, String payload) {
        ReadingTask task = new ReadingTask();
        task.setId(1L);
        task.setTaskType(type);
        task.setPayload(payload);
        return task;
    }
}
