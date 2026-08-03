package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.impl.WorkDetailSyncServiceImpl;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkDetailSyncServiceTest {

    @Test
    void syncDetailFetchesSourceAndPersistsRichMetadata() {
        WorkMapper workMapper = mock(WorkMapper.class);
        WorkSourceBindingMapper bindingMapper = mock(WorkSourceBindingMapper.class);
        SourceReadingService sourceReadingService = mock(SourceReadingService.class);
        WorkDetailSyncService service = new WorkDetailSyncServiceImpl(
                workMapper, bindingMapper, sourceReadingService);

        Work work = new Work();
        work.setId(100L);
        work.setTitle("凡人修仙传");
        work.setAuthorName("忘语");
        work.setCompletionStatus("unknown");
        WorkSourceBinding binding = new WorkSourceBinding();
        binding.setId(300L);
        binding.setWorkId(100L);
        binding.setSourceId(10L);
        binding.setSourceBookUrl("/128/");
        binding.setIsPrimarySource(1);
        when(workMapper.selectById(100L)).thenReturn(work);
        when(bindingMapper.selectPrimaryByWorkId(100L)).thenReturn(binding);
        DirectedReadVO read = new DirectedReadVO();
        read.setRecord(Map.of(
                "name", "凡人修仙传",
                "author", "忘语",
                "kind", "仙侠小说",
                "coverUrl", "https://www.sudugu.org/files/cover/128.jpg",
                "intro", "一个普通山村小子的修仙故事",
                "status", "全本",
                "wordCount", "741.0万字",
                "lastChapter", "新书《玄界之门》",
                "updateTime", "2025-09-16 02:49:41",
                "tocUrl", "/128/#dir"));
        when(sourceReadingService.detail(10L, "/128/")).thenReturn(read);

        WorkDetailSyncResult result = service.syncDetail(100L, null);

        assertEquals(10L, result.sourceId());
        assertEquals("/128/#dir", result.tocUrl());
        assertEquals(7_410_000L, work.getWordCount());
        assertEquals("completed", work.getCompletionStatus());
        assertEquals("一个普通山村小子的修仙故事", work.getDescription());
        assertEquals("/128/", binding.getSourceBookUrl());
        verify(workMapper).updateById(work);
        verify(bindingMapper).updateById(binding);
    }

    @Test
    void syncDetailUsesRequestedSourceBinding() {
        WorkMapper workMapper = mock(WorkMapper.class);
        WorkSourceBindingMapper bindingMapper = mock(WorkSourceBindingMapper.class);
        SourceReadingService sourceReadingService = mock(SourceReadingService.class);
        WorkDetailSyncService service = new WorkDetailSyncServiceImpl(
                workMapper, bindingMapper, sourceReadingService);

        Work work = new Work();
        work.setId(100L);
        WorkSourceBinding binding = new WorkSourceBinding();
        binding.setWorkId(100L);
        binding.setSourceId(20L);
        binding.setSourceBookUrl("/book/100");
        when(workMapper.selectById(100L)).thenReturn(work);
        when(bindingMapper.selectByWorkAndSource(100L, 20L)).thenReturn(binding);
        DirectedReadVO read = new DirectedReadVO();
        read.setRecord(Map.of("name", "剑来", "tocUrl", "/catalog/100"));
        when(sourceReadingService.detail(20L, "/book/100")).thenReturn(read);

        WorkDetailSyncResult result = service.syncDetail(100L, 20L);

        assertEquals(20L, result.sourceId());
        assertEquals("/catalog/100", result.tocUrl());
        verify(bindingMapper).selectByWorkAndSource(100L, 20L);
        verify(sourceReadingService).detail(20L, "/book/100");
    }

    @Test
    void syncDetailFromSecondarySourceDoesNotOverwriteCanonicalWork() {
        WorkMapper workMapper = mock(WorkMapper.class);
        WorkSourceBindingMapper bindingMapper = mock(WorkSourceBindingMapper.class);
        SourceReadingService sourceReadingService = mock(SourceReadingService.class);
        WorkDetailSyncService service = new WorkDetailSyncServiceImpl(
                workMapper, bindingMapper, sourceReadingService);

        Work work = new Work();
        work.setId(100L);
        work.setTitle("主来源书名");
        WorkSourceBinding binding = new WorkSourceBinding();
        binding.setWorkId(100L);
        binding.setSourceId(20L);
        binding.setSourceBookUrl("/book/100");
        binding.setIsPrimarySource(0);
        when(workMapper.selectById(100L)).thenReturn(work);
        when(bindingMapper.selectByWorkAndSource(100L, 20L)).thenReturn(binding);
        DirectedReadVO read = new DirectedReadVO();
        read.setRecord(Map.of("name", "副来源书名", "tocUrl", "/catalog/100"));
        when(sourceReadingService.detail(20L, "/book/100")).thenReturn(read);

        service.syncDetail(100L, 20L);

        assertEquals("主来源书名", work.getTitle());
        assertEquals("副来源书名", binding.getSourceBookName());
        verify(workMapper, never()).updateById(work);
        verify(bindingMapper).updateById(binding);
    }
}
