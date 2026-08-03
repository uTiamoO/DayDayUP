package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.mapper.ChapterMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.service.impl.ChapterAssemblyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ChapterAssemblyService} 单测：内存 map 假实现三表，覆盖主来源建统一章节+绑定、
 * 非主来源惰性对齐既有章节、空目录保护、先清后建幂等、TocEntry 归一化。
 */
class ChapterAssemblyServiceTest {

    private final List<Chapter> chapters = new ArrayList<>();
    private final List<ChapterSourceBinding> bindings = new ArrayList<>();
    private final List<Work> works = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(5000);

    private ChapterAssemblyService service;

    @BeforeEach
    void setUp() {
        chapters.clear();
        bindings.clear();
        works.clear();
        Work w = new Work();
        w.setId(1L);
        w.setTitle("凡人修仙传");
        works.add(w);

        ChapterMapper chapterMapper = mock(ChapterMapper.class);
        ChapterSourceBindingMapper bindingMapper = mock(ChapterSourceBindingMapper.class);
        WorkMapper workMapper = mock(WorkMapper.class);

        doAnswer(inv -> {
            Chapter c = inv.getArgument(0);
            c.setId(seq.incrementAndGet());
            chapters.add(c);
            return 1;
        }).when(chapterMapper).insert(any(Chapter.class));
        when(chapterMapper.deleteByWorkId(anyLong())).thenAnswer(inv -> {
            long wid = inv.getArgument(0);
            chapters.removeIf(c -> c.getWorkId().equals(wid));
            return 1;
        });
        when(chapterMapper.selectByWorkId(anyLong())).thenAnswer(inv -> {
            long wid = inv.getArgument(0);
            return chapters.stream().filter(c -> c.getWorkId().equals(wid)).toList();
        });

        doAnswer(inv -> {
            ChapterSourceBinding b = inv.getArgument(0);
            b.setId(seq.incrementAndGet());
            bindings.add(b);
            return 1;
        }).when(bindingMapper).insert(any(ChapterSourceBinding.class));
        when(bindingMapper.deleteByWorkAndSource(anyLong(), anyLong())).thenAnswer(inv -> {
            long wid = inv.getArgument(0);
            long sid = inv.getArgument(1);
            bindings.removeIf(b -> b.getWorkId().equals(wid) && b.getSourceId().equals(sid));
            return 1;
        });

        when(workMapper.selectById(anyLong())).thenAnswer(inv ->
                works.stream().filter(x -> x.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(workMapper.updateById(any(Work.class))).thenReturn(1);

        service = new ChapterAssemblyServiceImpl(chapterMapper, bindingMapper, workMapper);
    }

    private List<TocEntry> toc(int n) {
        List<TocEntry> entries = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            entries.add(new TocEntry("第" + i + "章", "/c/" + i));
        }
        return entries;
    }

    @Test
    void primarySourceBuildsChaptersAndBindings() {
        TocSyncResultVO r = service.syncToc(1L, 10L, true, toc(3));

        assertEquals(3, r.getChaptersBuilt());
        assertEquals(3, r.getBindingsBuilt());
        assertEquals(3, chapters.size());
        assertEquals(0, chapters.get(0).getChapterIndex());
        assertEquals("第1章", chapters.get(0).getChapterTitle());
        // 主来源绑定回填 chapterId
        assertNotNull(bindings.get(0).getChapterId());
        assertEquals(chapters.get(0).getId(), bindings.get(0).getChapterId());
        // Work 最新章节回写
        assertEquals("第3章", works.get(0).getLatestChapterTitle());
    }

    @Test
    void nonPrimaryAlignsBindingsToExistingChapters() {
        service.syncToc(1L, 10L, true, toc(2));
        bindings.clear();

        TocSyncResultVO r = service.syncToc(1L, 20L, false, toc(2));

        assertEquals(0, r.getChaptersBuilt());
        assertEquals(2, r.getBindingsBuilt());
        assertEquals(2, chapters.size());
        assertEquals(chapters.get(0).getId(), bindings.get(0).getChapterId());
        assertEquals(100, bindings.get(0).getBindingConfidence());
    }

    @Test
    void emptyTocDoesNotWipeExisting() {
        service.syncToc(1L, 10L, true, toc(3));
        // 再次同步返回空 → 不删除既有章节/绑定
        TocSyncResultVO r = service.syncToc(1L, 10L, true, List.of());

        assertEquals(0, r.getTocEntries());
        assertEquals(0, r.getChaptersBuilt());
        assertEquals(3, chapters.size());
        assertEquals(3, bindings.size());
    }

    @Test
    void reSyncRebuildsWithoutDuplication() {
        service.syncToc(1L, 10L, true, toc(3));
        service.syncToc(1L, 10L, true, toc(5));   // 目录增长到 5 章

        assertEquals(5, chapters.size());
        assertEquals(5, bindings.size());
        // 序号连续无重复
        for (int i = 0; i < 5; i++) {
            assertEquals(i, chapters.get(i).getChapterIndex());
        }
    }

    @Test
    void multiSourceKeepsPerSourceBindings() {
        service.syncToc(1L, 10L, true, toc(3));    // 主来源
        service.syncToc(1L, 20L, false, toc(2));   // 非主来源

        assertEquals(3, chapters.size());          // 章节只来自主来源
        assertEquals(5, bindings.size());          // 两源绑定并存
    }

    @Test
    void tocEntryMappingSkipsUrllessAndKeepsOrder() {
        List<TocEntry> entries = TocEntry.from(List.of(
                Map.of("chapterName", "第一章", "chapterUrl", "/c/1"),
                Map.of("chapterName", "无链接章"),                        // 无 url → 跳过
                Map.of("name", "第二章", "url", "/c/2")));                // 别名字段

        assertEquals(2, entries.size());
        assertEquals("第一章", entries.get(0).title());
        assertEquals("/c/2", entries.get(1).url());
    }
}
