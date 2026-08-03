package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.mapper.ChapterMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.service.ChapterAssemblyService;
import com.yuan.daydayup.reading.repository.service.TocEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节装配实现（spec §5.3）。
 *
 * <p>先清后建：删除本源旧目录绑定；主来源另删并重建统一 Chapter（chapterIndex = 目录序）
 * 并回填绑定的 chapterId。非主来源按“相同目录序 + 规整标题”优先、唯一标题兜底，
 * 惰性对齐到既有统一章节。</p>
 *
 * <p>空目录保护：抓取为空时不删除既有数据（避免瞬时解析失败摧毁已入库目录）。</p>
 */
@Slf4j
@Service
public class ChapterAssemblyServiceImpl implements ChapterAssemblyService {

    private final ChapterMapper chapterMapper;
    private final ChapterSourceBindingMapper bindingMapper;
    private final WorkMapper workMapper;

    public ChapterAssemblyServiceImpl(ChapterMapper chapterMapper,
                                      ChapterSourceBindingMapper bindingMapper,
                                      WorkMapper workMapper) {
        this.chapterMapper = chapterMapper;
        this.bindingMapper = bindingMapper;
        this.workMapper = workMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TocSyncResultVO syncToc(Long workId, Long sourceId, boolean primarySource, List<TocEntry> entries) {
        TocSyncResultVO result = new TocSyncResultVO();
        result.setWorkId(workId);
        result.setSourceId(sourceId);
        result.setPrimarySource(primarySource);
        result.setTocEntries(entries == null ? 0 : entries.size());

        if (entries == null || entries.isEmpty()) {
            log.warn("[chapter-assembly] 目录为空，跳过重建（保护既有数据） workId={} sourceId={}", workId, sourceId);
            return result;
        }

        // 主来源：重建统一章节，记录 目录序 -> chapterId
        Map<Integer, Long> orderToChapterId = new HashMap<>();
        if (primarySource) {
            chapterMapper.deleteByWorkId(workId);
            for (int i = 0; i < entries.size(); i++) {
                Chapter chapter = newChapter(workId, entries.get(i), i);
                chapterMapper.insert(chapter);
                orderToChapterId.put(i, chapter.getId());
            }
            result.setChaptersBuilt(entries.size());
            updateWorkLatest(workId, entries.get(entries.size() - 1).title());
        } else {
            alignExistingChapters(workId, entries, orderToChapterId);
        }

        // 所有来源：重建 per-source 目录绑定
        bindingMapper.deleteByWorkAndSource(workId, sourceId);
        for (int i = 0; i < entries.size(); i++) {
            ChapterSourceBinding binding = newBinding(workId, sourceId, entries.get(i), i,
                    orderToChapterId.get(i));
            bindingMapper.insert(binding);
        }
        result.setBindingsBuilt(entries.size());
        return result;
    }

    private void alignExistingChapters(Long workId, List<TocEntry> entries, Map<Integer, Long> orderToChapterId) {
        List<Chapter> existing = chapterMapper.selectByWorkId(workId);
        Map<String, List<Chapter>> chaptersByTitle = new HashMap<>();
        for (Chapter chapter : existing) {
            chaptersByTitle.computeIfAbsent(normalizeTitle(chapter.getChapterTitle()), key -> new ArrayList<>())
                    .add(chapter);
        }
        for (int i = 0; i < entries.size(); i++) {
            String title = normalizeTitle(entries.get(i).title());
            if (i < existing.size()) {
                Chapter sameOrder = existing.get(i);
                if (title.equals(normalizeTitle(sameOrder.getChapterTitle()))) {
                    orderToChapterId.put(i, sameOrder.getId());
                    continue;
                }
            }
            List<Chapter> sameTitle = chaptersByTitle.get(title);
            if (sameTitle != null && sameTitle.size() == 1) {
                orderToChapterId.put(i, sameTitle.get(0).getId());
            }
        }
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.replaceAll("[\\s\\p{Punct}，。！？：；、‘’“”《》（）【】]+", "");
    }

    private Chapter newChapter(Long workId, TocEntry entry, int index) {
        Chapter chapter = new Chapter();
        chapter.setWorkId(workId);
        chapter.setChapterTitle(entry.title());
        chapter.setChapterIndex(index);
        chapter.setIsVipChapter(0);
        chapter.setChapterStatus("active");
        return chapter;
    }

    private ChapterSourceBinding newBinding(Long workId, Long sourceId, TocEntry entry, int order, Long chapterId) {
        ChapterSourceBinding binding = new ChapterSourceBinding();
        binding.setWorkId(workId);
        binding.setSourceId(sourceId);
        binding.setChapterId(chapterId);
        binding.setSourceChapterUrl(entry.url());
        binding.setSourceChapterTitle(entry.title());
        binding.setSourceChapterOrder(order);
        binding.setBindingConfidence(chapterId != null ? 100 : 80);
        return binding;
    }

    private void updateWorkLatest(Long workId, String latestTitle) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setLatestChapterTitle(latestTitle);
            work.setLatestChapterUpdatedAt(LocalDateTime.now());
            workMapper.updateById(work);
        }
    }
}
