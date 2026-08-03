package com.yuan.daydayup.reading.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingChapterVO;
import com.yuan.daydayup.reading.api.vo.ReadingContentVO;
import com.yuan.daydayup.reading.api.vo.ReadingFilterVO;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkDetailVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkVO;
import com.yuan.daydayup.reading.api.vo.SanitizeResultVO;
import com.yuan.daydayup.reading.api.vo.WorkBriefVO;
import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.pipeline.entity.ContentSanitizationRun;
import com.yuan.daydayup.reading.pipeline.mapper.ContentSanitizationRunMapper;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncService;
import com.yuan.daydayup.reading.service.impl.ReadingContentServiceImpl;
import com.yuan.daydayup.reading.service.impl.ReadingQueryServiceImpl;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 统一阅读 API 编排服务测试。
 */
class ReadingServiceTest {

    private WorkMapper workMapper;
    private WorkSourceBindingMapper workSourceBindingMapper;
    private ChapterMapper chapterMapper;
    private ChapterSourceBindingMapper chapterSourceBindingMapper;
    private SourceDefinitionMapper sourceDefinitionMapper;
    private ContentDiscoveryService contentDiscoveryService;
    private ChapterSyncService chapterSyncService;
    private WorkDetailSyncService workDetailSyncService;
    private ChapterContentSnapshotMapper snapshotMapper;
    private ContentSanitizationRunMapper runMapper;
    private ContentFetchService contentFetchService;
    private ContentSanitizeService contentSanitizeService;
    private ReadingQueryService queryService;
    private ReadingContentService contentService;

    @BeforeEach
    void setUp() {
        workMapper = mock(WorkMapper.class);
        workSourceBindingMapper = mock(WorkSourceBindingMapper.class);
        chapterMapper = mock(ChapterMapper.class);
        chapterSourceBindingMapper = mock(ChapterSourceBindingMapper.class);
        sourceDefinitionMapper = mock(SourceDefinitionMapper.class);
        contentDiscoveryService = mock(ContentDiscoveryService.class);
        chapterSyncService = mock(ChapterSyncService.class);
        workDetailSyncService = mock(WorkDetailSyncService.class);
        snapshotMapper = mock(ChapterContentSnapshotMapper.class);
        runMapper = mock(ContentSanitizationRunMapper.class);
        contentFetchService = mock(ContentFetchService.class);
        contentSanitizeService = mock(ContentSanitizeService.class);
        queryService = new ReadingQueryServiceImpl(workMapper, workSourceBindingMapper, chapterMapper,
                chapterSourceBindingMapper, sourceDefinitionMapper, contentDiscoveryService, chapterSyncService,
                workDetailSyncService);
        contentService = new ReadingContentServiceImpl(chapterMapper, chapterSourceBindingMapper, snapshotMapper,
                runMapper, contentFetchService, contentSanitizeService);
    }

    @Test
    void sourceSearchDiscoversAndReturnsPlatformWorkIds() {
        WorkBriefVO work = new WorkBriefVO();
        work.setWorkId(100L);
        work.setTitle("斗破苍穹");
        work.setAuthor("天蚕土豆");
        work.setAggregationStatus("single_source");
        work.setSourceCount(1);
        WorkDiscoveryResultVO result = new WorkDiscoveryResultVO();
        result.setTotal(1);
        result.setWorks(List.of(work));
        when(contentDiscoveryService.discover(10L, "斗破", 1)).thenReturn(result);

        ReadingPageVO<ReadingWorkVO> page = queryService.search("source", "斗破", 10L, null, null, 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals(100L, page.getList().get(0).getWorkId());
        assertEquals("斗破苍穹", page.getList().get(0).getTitle());
    }

    @Test
    void aggregateSearchQueriesWorkReadModel() {
        Work work = work(100L, "凡人修仙传");
        Page<Work> result = new Page<>(1, 20, 1);
        result.setRecords(List.of(work));
        when(workMapper.selectReadingPage(any(), eq("凡人"), eq("仙侠"), eq("serial"), eq(null), eq(null))).thenReturn(result);

        ReadingPageVO<ReadingWorkVO> page = queryService.search("aggregate", "凡人", null, "仙侠", "serial", 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals("凡人修仙传", page.getList().get(0).getTitle());
        assertEquals(100L, page.getList().get(0).getWorkId());
    }

    @Test
    void detailReturnsPrimarySourceAndLatestChapterWithoutSourceUrl() {
        Work work = work(100L, "诡秘之主");
        work.setDescription("详情已缓存");
        work.setCompletionStatus("completed");
        when(workMapper.selectById(100L)).thenReturn(work);
        when(workSourceBindingMapper.selectPrimaryByWorkId(100L)).thenReturn(binding(100L, 10L, true));
        SourceDefinition source = new SourceDefinition();
        source.setId(10L);
        source.setName("优质书源");
        source.setSiteName("站点");
        when(sourceDefinitionMapper.selectById(10L)).thenReturn(source);
        Chapter chapter = chapter(200L, 100L, 9, "第十章");
        when(chapterMapper.selectLatestByWorkId(100L)).thenReturn(chapter);

        ReadingWorkDetailVO detail = queryService.detail(100L, null, "cache-first");

        assertEquals("诡秘之主", detail.getTitle());
        assertEquals("优质书源", detail.getPrimarySource().getSourceName());
        assertEquals(200L, detail.getLatestChapter().getChapterId());
        verify(workDetailSyncService, never()).syncDetail(any(), any());
    }

    @Test
    void detailCacheMissRefreshesPrimarySourceBeforeReturning() {
        Work cached = work(100L, "凡人修仙传");
        cached.setCompletionStatus("unknown");
        Work refreshed = work(100L, "凡人修仙传");
        refreshed.setDescription("一个普通山村小子的修仙故事");
        refreshed.setCompletionStatus("completed");
        refreshed.setWordCount(7_410_000L);
        when(workMapper.selectById(100L)).thenReturn(cached, refreshed);

        ReadingWorkDetailVO detail = queryService.detail(100L, null, "cache-first");

        verify(workDetailSyncService).syncDetail(100L, null);
        assertEquals("一个普通山村小子的修仙故事", detail.getDescription());
        assertEquals(7_410_000L, detail.getWordCount());
    }

    @Test
    void detailForceRefreshUsesRequestedSource() {
        Work cached = work(100L, "凡人修仙传");
        cached.setDescription("旧详情");
        cached.setCompletionStatus("completed");
        Work refreshed = work(100L, "凡人修仙传");
        refreshed.setDescription("实时详情");
        refreshed.setCompletionStatus("completed");
        when(workMapper.selectById(100L)).thenReturn(cached, refreshed);

        ReadingWorkDetailVO detail = queryService.detail(100L, 10L, "force-refresh");

        verify(workDetailSyncService).syncDetail(100L, 10L);
        assertEquals("实时详情", detail.getDescription());
    }

    @Test
    void chaptersMarksRequestedSourceAvailability() {
        Work work = work(100L, "剑来");
        when(workMapper.selectById(100L)).thenReturn(work);
        Chapter first = chapter(201L, 100L, 0, "第一章");
        Chapter second = chapter(202L, 100L, 1, "第二章");
        Page<Chapter> result = new Page<>(1, 50, 2);
        result.setRecords(List.of(first, second));
        when(chapterMapper.selectPageByWorkId(any(), eq(100L))).thenReturn(result);
        ChapterSourceBinding binding = new ChapterSourceBinding();
        binding.setChapterId(201L);
        binding.setSourceId(10L);
        binding.setSourceChapterTitle("源第一章");
        when(chapterSourceBindingMapper.selectByWorkAndSource(100L, 10L)).thenReturn(List.of(binding));

        ReadingPageVO<ReadingChapterVO> page = queryService.chapters(100L, 10L, "cache-first", 1, 50);

        assertTrue(page.getList().get(0).isSourceAvailable());
        assertEquals("源第一章", page.getList().get(0).getSourceChapterTitle());
        assertFalse(page.getList().get(1).isSourceAvailable());
        verify(chapterSyncService, never()).syncToc(any(), any());
    }

    @Test
    void workSourcesUseDatabasePaginationAndBatchSourceSummary() {
        Work work = work(100L, "诡秘之主");
        when(workMapper.selectById(100L)).thenReturn(work);
        WorkSourceBinding binding = binding(100L, 10L, true);
        Page<WorkSourceBinding> result = new Page<>(1, 20, 1);
        result.setRecords(List.of(binding));
        when(workSourceBindingMapper.selectActivePageByWorkId(any(), eq(100L))).thenReturn(result);
        when(workSourceBindingMapper.selectSourceSummariesByBindingIds(List.of(10L)))
                .thenReturn(List.of(Map.of(
                        "id", 10L,
                        "sourceName", "优质书源",
                        "siteName", "站点",
                        "sourceStatus", 1,
                        "sourcePriority", 9)));

        var page = queryService.sources(100L, 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals("优质书源", page.getList().get(0).getSourceName());
        assertEquals(9, page.getList().get(0).getPriority());
        verify(sourceDefinitionMapper, never()).selectById(10L);
    }

    @Test
    void sourcesListReturnsEnabledSourceDefinitionsWithPageLimit() {
        SourceDefinition source = new SourceDefinition();
        source.setId(10L);
        source.setName("猫眼看书");
        source.setSiteName("JSON源");
        source.setStatus(1);
        source.setPriority(9999);
        source.setCompileGrade("degraded");
        Page<SourceDefinition> result = new Page<>(1, 20, 1);
        result.setRecords(List.of(source));
        when(sourceDefinitionMapper.selectPage(any(), any())).thenReturn(result);

        var page = queryService.sourceDefinitions(1, 20);

        assertEquals(1, page.getTotal());
        assertEquals("猫眼看书", page.getList().get(0).getSourceName());
        assertEquals("degraded", page.getList().get(0).getBindingStatus());
    }

    @Test
    void filtersReturnCategoriesAndCompletionStatuses() {
        when(workMapper.selectCategoryCounts(20)).thenReturn(List.of(Map.of("name", "玄幻", "count", 2L)));
        when(workMapper.selectCompletionStatusCounts()).thenReturn(List.of(Map.of("name", "completed", "count", 1L)));

        List<ReadingFilterVO> filters = queryService.filters("all", 20);

        assertEquals(2, filters.size());
        assertEquals("玄幻", filters.get(0).getName());
        assertEquals("status:completed", filters.get(1).getCode());
        assertEquals("完结", filters.get(1).getName());
    }


    @Test
    void chaptersForceRefreshSyncsTocBeforeQuery() {
        Work work = work(100L, "剑来");
        when(workMapper.selectById(100L)).thenReturn(work);
        Page<Chapter> result = new Page<>(1, 50, 0);
        result.setRecords(List.of());
        when(chapterMapper.selectPageByWorkId(any(), eq(100L))).thenReturn(result);

        queryService.chapters(100L, 10L, "force-refresh", 1, 50);

        verify(chapterSyncService).syncToc(100L, 10L);
    }

    @Test
    void chaptersCacheMissSyncsPrimarySourceBeforeQuery() {
        Work work = work(100L, "凡人修仙传");
        when(workMapper.selectById(100L)).thenReturn(work);
        WorkSourceBinding primary = binding(100L, 10L, true);
        when(workSourceBindingMapper.selectPrimaryByWorkId(100L)).thenReturn(primary);
        Page<Chapter> empty = new Page<>(1, 50, 0);
        empty.setRecords(List.of());
        Chapter first = chapter(201L, 100L, 0, "第1章 山边小村");
        Page<Chapter> refreshed = new Page<>(1, 50, 1);
        refreshed.setRecords(List.of(first));
        when(chapterMapper.selectPageByWorkId(any(), eq(100L))).thenReturn(empty, refreshed);

        ReadingPageVO<ReadingChapterVO> page = queryService.chapters(100L, null, "cache-first", 1, 50);

        verify(chapterSyncService).syncToc(100L, 10L);
        assertEquals(1, page.getTotal());
        assertEquals("第1章 山边小村", page.getList().get(0).getChapterTitle());
    }

    @Test
    void chaptersMissingRequestedSourceBindingsSyncsRequestedSource() {
        Work work = work(100L, "凡人修仙传");
        when(workMapper.selectById(100L)).thenReturn(work);
        Chapter first = chapter(201L, 100L, 0, "第1章 山边小村");
        Page<Chapter> cached = new Page<>(1, 50, 1);
        cached.setRecords(List.of(first));
        when(chapterMapper.selectPageByWorkId(any(), eq(100L))).thenReturn(cached, cached);
        ChapterSourceBinding aligned = new ChapterSourceBinding();
        aligned.setChapterId(201L);
        aligned.setSourceId(20L);
        when(chapterSourceBindingMapper.selectByWorkAndSource(100L, 20L))
                .thenReturn(List.of(), List.of(aligned));

        ReadingPageVO<ReadingChapterVO> page = queryService.chapters(100L, 20L, "cache-first", 1, 50);

        verify(chapterSyncService).syncToc(100L, 20L);
        assertTrue(page.getList().get(0).isSourceAvailable());
    }

    @Test
    void invalidRefreshPolicyIsInvalidArgument() {
        when(workMapper.selectById(100L)).thenReturn(work(100L, "凡人修仙传"));

        BizException error = assertThrows(BizException.class,
                () -> queryService.chapters(100L, null, "always", 1, 50));

        assertEquals(ErrorCode.READING_INVALID_ARGUMENT.getCode(), error.getCode());
    }

    @Test
    void contentDefaultsToSanitizedCacheFirst() {
        stubChapterAndBinding();
        ChapterContentSnapshot snapshot = snapshot("raw", "normalized", "sanitized");
        when(snapshotMapper.selectByChapterAndSource(200L, 10L)).thenReturn(snapshot);
        ContentSanitizationRun run = run(88, 900L);
        when(runMapper.selectLatestBySnapshotId(800L)).thenReturn(run);

        ReadingContentVO content = contentService.content(200L, 10L, null, null);

        assertEquals("sanitized", content.getContentVersion());
        assertEquals("sanitized", content.getContent());
        assertEquals(88, content.getQualityScore());
        assertFalse(content.isFreshlyFetched());
        verify(contentFetchService, never()).fetchAndStore(any(), any(), eq(false));
    }

    @Test
    void contentColdCacheFetchesAndSanitizesWithoutForceRefresh() {
        stubChapterAndBinding();
        ChapterContentSnapshot afterFetch = snapshot("raw", "normalized", null);
        ChapterContentSnapshot afterSanitize = snapshot("raw", "normalized", "sanitized");
        when(snapshotMapper.selectByChapterAndSource(200L, 10L))
                .thenReturn(null, afterFetch, afterSanitize);
        SanitizeResultVO sanitizeResult = new SanitizeResultVO();
        sanitizeResult.setSanitizedContent("sanitized");
        when(contentSanitizeService.sanitize(200L, 10L)).thenReturn(sanitizeResult);

        ReadingContentVO content = contentService.content(200L, 10L, "sanitized", "cache-first");

        assertEquals("sanitized", content.getContent());
        assertTrue(content.isFreshlyFetched());
        verify(contentFetchService).fetchAndStore(200L, 10L, false);
        verify(contentSanitizeService).sanitize(200L, 10L);
    }

    @Test
    void contentForceRefreshFetchesAndSanitizes() {
        stubChapterAndBinding();
        ChapterContentSnapshot before = snapshot("raw", "normalized", null);
        ChapterContentSnapshot afterFetch = snapshot("raw2", "normalized2", null);
        ChapterContentSnapshot afterSanitize = snapshot("raw2", "normalized2", "sanitized2");
        when(snapshotMapper.selectByChapterAndSource(200L, 10L)).thenReturn(before, afterFetch, afterSanitize, afterSanitize);
        SanitizeResultVO sanitizeResult = new SanitizeResultVO();
        sanitizeResult.setSanitizedContent("sanitized2");
        when(contentSanitizeService.sanitize(200L, 10L)).thenReturn(sanitizeResult);

        ReadingContentVO content = contentService.content(200L, 10L, "latest", "force-refresh");

        assertEquals("sanitized", content.getContentVersion());
        assertEquals("sanitized2", content.getContent());
        assertTrue(content.isFreshlyFetched());
        verify(contentFetchService).fetchAndStore(200L, 10L, true);
        verify(contentSanitizeService).sanitize(200L, 10L);
    }


    @Test
    void contentSanitizesCachedRawWithoutRefetching() {
        stubChapterAndBinding();
        ChapterContentSnapshot snapshot = snapshot("raw", "normalized", null);
        ChapterContentSnapshot sanitized = snapshot("raw", "normalized", "sanitized");
        when(snapshotMapper.selectByChapterAndSource(200L, 10L)).thenReturn(snapshot, sanitized);

        ReadingContentVO content = contentService.content(200L, 10L, "sanitized", "cache-first");

        assertEquals("sanitized", content.getContent());
        assertTrue(content.isFreshlyFetched());
        verify(contentFetchService, never()).fetchAndStore(any(), any(), eq(false));
        verify(contentSanitizeService).sanitize(200L, 10L);
    }

    private void stubChapterAndBinding() {
        when(chapterMapper.selectById(200L)).thenReturn(chapter(200L, 100L, 0, "第一章"));
        ChapterSourceBinding binding = new ChapterSourceBinding();
        binding.setChapterId(200L);
        binding.setSourceId(10L);
        when(chapterSourceBindingMapper.selectByChapterAndSource(200L, 10L)).thenReturn(binding);
    }

    private Work work(Long id, String title) {
        Work work = new Work();
        work.setId(id);
        work.setTitle(title);
        work.setAuthorName("作者");
        work.setAggregationStatus("single_source");
        work.setSourceCount(1);
        return work;
    }

    private WorkSourceBinding binding(Long workId, Long sourceId, boolean primary) {
        WorkSourceBinding binding = new WorkSourceBinding();
        binding.setWorkId(workId);
        binding.setSourceId(sourceId);
        binding.setId(sourceId);
        binding.setIsPrimarySource(primary ? 1 : 0);
        binding.setBindingStatus("active");
        binding.setSourceBookName("来源书名");
        binding.setSourceAuthorName("来源作者");
        binding.setMatchConfidence(100);
        return binding;
    }

    private Chapter chapter(Long id, Long workId, int index, String title) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setWorkId(workId);
        chapter.setChapterIndex(index);
        chapter.setChapterTitle(title);
        chapter.setIsVipChapter(0);
        chapter.setChapterStatus("active");
        return chapter;
    }

    private ChapterContentSnapshot snapshot(String raw, String normalized, String sanitized) {
        ChapterContentSnapshot snapshot = new ChapterContentSnapshot();
        snapshot.setId(800L);
        snapshot.setChapterId(200L);
        snapshot.setSourceId(10L);
        snapshot.setRawContent(raw);
        snapshot.setNormalizedContent(normalized);
        snapshot.setSanitizedContent(sanitized);
        snapshot.setContentStatus(sanitized == null ? "normalized" : "sanitized");
        return snapshot;
    }

    private ContentSanitizationRun run(int qualityScore, Long runId) {
        ContentSanitizationRun run = new ContentSanitizationRun();
        run.setId(runId);
        run.setQualityScore(qualityScore);
        return run;
    }
}
