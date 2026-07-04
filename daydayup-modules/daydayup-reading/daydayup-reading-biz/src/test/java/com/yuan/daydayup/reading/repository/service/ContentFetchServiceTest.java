package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ContentSnapshotVO;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.impl.ContentFetchServiceImpl;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContentFetchService} 单测：内存快照表 + mock runtime，覆盖首抓、cache-first 命中、
 * force-refresh、空正文 60301、HTML 正文标准化落库。
 */
class ContentFetchServiceTest {

    private final List<ChapterContentSnapshot> store = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(7000);

    private ChapterSourceBindingMapper bindingMapper;
    private ChapterContentSnapshotMapper snapshotMapper;
    private SourceReadingService sourceReadingService;
    private ContentFetchService service;

    @BeforeEach
    void setUp() {
        store.clear();
        bindingMapper = mock(ChapterSourceBindingMapper.class);
        snapshotMapper = mock(ChapterContentSnapshotMapper.class);
        sourceReadingService = mock(SourceReadingService.class);

        ChapterSourceBinding binding = new ChapterSourceBinding();
        binding.setChapterId(1L);
        binding.setSourceId(10L);
        binding.setSourceChapterUrl("/c/1");
        when(bindingMapper.selectByChapterAndSource(1L, 10L)).thenReturn(binding);

        doAnswer(inv -> {
            ChapterContentSnapshot s = inv.getArgument(0);
            s.setId(seq.incrementAndGet());
            store.add(s);
            return 1;
        }).when(snapshotMapper).insert(any(ChapterContentSnapshot.class));
        when(snapshotMapper.updateById(any(ChapterContentSnapshot.class))).thenReturn(1);
        when(snapshotMapper.selectByChapterAndSource(1L, 10L)).thenAnswer(inv ->
                store.stream().findFirst().orElse(null));

        service = new ContentFetchServiceImpl(bindingMapper, snapshotMapper, sourceReadingService);
    }

    private void stubContent(String raw) {
        DirectedReadVO vo = new DirectedReadVO();
        vo.setRecord(raw == null ? Map.of() : Map.of("content", raw));
        when(sourceReadingService.content(eq(10L), any())).thenReturn(vo);
    }

    @Test
    void firstFetchStoresRawAndNormalized() {
        stubContent("<p>第一段</p><p>第二段</p>");
        ContentSnapshotVO vo = service.fetchAndStore(1L, 10L, false);

        assertTrue(vo.isFreshlyFetched());
        assertEquals("normalized", vo.getContentStatus());
        assertEquals("第一段\n第二段", vo.getContent());
        assertEquals(1, store.size());
        assertEquals("<p>第一段</p><p>第二段</p>", store.get(0).getRawContent());
    }

    @Test
    void cacheFirstReusesExistingSnapshot() {
        stubContent("<p>正文</p>");
        service.fetchAndStore(1L, 10L, false);          // 首抓
        ContentSnapshotVO vo = service.fetchAndStore(1L, 10L, false);   // 再取 → 命中缓存

        assertFalse(vo.isFreshlyFetched());
        // runtime 只被调用一次（第二次走缓存）
        verify(sourceReadingService, times(1)).content(eq(10L), any());
    }

    @Test
    void forceRefreshBypassesCache() {
        stubContent("<p>正文</p>");
        service.fetchAndStore(1L, 10L, false);
        ContentSnapshotVO vo = service.fetchAndStore(1L, 10L, true);   // 强制回源

        assertTrue(vo.isFreshlyFetched());
        verify(sourceReadingService, times(2)).content(eq(10L), any());
        assertEquals(1, store.size());   // 幂等 upsert，不新增行
    }

    @Test
    void emptyContentThrowsAndMarksEmpty() {
        stubContent("   ");
        BizException e = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.fetchAndStore(1L, 10L, false));
        assertEquals(60301, e.getCode());
        assertEquals("empty", store.get(0).getContentStatus());
    }

    @Test
    void missingBindingThrows() {
        when(bindingMapper.selectByChapterAndSource(9L, 10L)).thenReturn(null);
        BizException e = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.fetchAndStore(9L, 10L, false));
        assertEquals(60003, e.getCode());
        verify(sourceReadingService, never()).content(anyLong(), any());
    }
}
