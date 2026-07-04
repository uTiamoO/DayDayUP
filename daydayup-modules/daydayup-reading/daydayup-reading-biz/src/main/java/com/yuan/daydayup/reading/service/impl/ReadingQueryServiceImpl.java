package com.yuan.daydayup.reading.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingChapterVO;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingSourceVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkDetailVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkVO;
import com.yuan.daydayup.reading.api.vo.WorkBriefVO;
import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.ChapterMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import com.yuan.daydayup.reading.service.ReadingQueryService;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一阅读查询编排实现。
 */
@Service
public class ReadingQueryServiceImpl implements ReadingQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String POLICY_CACHE_FIRST = "cache-first";
    private static final String POLICY_FORCE_REFRESH = "force-refresh";

    private final WorkMapper workMapper;
    private final WorkSourceBindingMapper workSourceBindingMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterSourceBindingMapper chapterSourceBindingMapper;
    private final SourceDefinitionMapper sourceDefinitionMapper;
    private final ContentDiscoveryService contentDiscoveryService;
    private final ChapterSyncService chapterSyncService;

    public ReadingQueryServiceImpl(WorkMapper workMapper,
                                   WorkSourceBindingMapper workSourceBindingMapper,
                                   ChapterMapper chapterMapper,
                                   ChapterSourceBindingMapper chapterSourceBindingMapper,
                                   SourceDefinitionMapper sourceDefinitionMapper,
                                   ContentDiscoveryService contentDiscoveryService,
                                   ChapterSyncService chapterSyncService) {
        this.workMapper = workMapper;
        this.workSourceBindingMapper = workSourceBindingMapper;
        this.chapterMapper = chapterMapper;
        this.chapterSourceBindingMapper = chapterSourceBindingMapper;
        this.sourceDefinitionMapper = sourceDefinitionMapper;
        this.contentDiscoveryService = contentDiscoveryService;
        this.chapterSyncService = chapterSyncService;
    }

    @Override
    public ReadingPageVO<ReadingWorkVO> search(String mode, String keyword, Long sourceId, String category,
                                                String completionStatus, int page, int pageSize) {
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        String safeMode = StringUtils.hasText(mode) ? mode : "aggregate";
        if ("source".equals(safeMode)) {
            if (sourceId == null) {
                throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "source 模式必须提供 sourceId");
            }
            if (!StringUtils.hasText(keyword)) {
                throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "keyword 不能为空");
            }
            WorkDiscoveryResultVO result = contentDiscoveryService.discover(sourceId, keyword, safePage);
            List<ReadingWorkVO> list = result.getWorks().stream().map(this::toReadingWork).toList();
            long total = result.getTotal() > 0 ? result.getTotal() : list.size();
            return ReadingPageVO.of(list, total, safePage, safePageSize);
        }
        if (!"aggregate".equals(safeMode)) {
            throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的搜索模式: " + safeMode);
        }
        return works(keyword, category, completionStatus, sourceId, null, safePage, safePageSize);
    }

    @Override
    public ReadingPageVO<ReadingWorkVO> works(String keyword, String category, String status, Long sourceId,
                                              String sort, int page, int pageSize) {
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        Page<Work> workPage = workMapper.selectReadingPage(new Page<>(safePage, safePageSize),
                trimToNull(keyword), trimToNull(category), trimToNull(status), sourceId, trimToNull(sort));
        List<ReadingWorkVO> list = workPage.getRecords().stream().map(this::toReadingWork).toList();
        return ReadingPageVO.of(list, workPage.getTotal(), safePage, safePageSize);
    }

    @Override
    public ReadingWorkDetailVO detail(Long workId) {
        Work work = requireWork(workId);
        ReadingWorkDetailVO vo = new ReadingWorkDetailVO();
        vo.setWorkId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setAuthorName(work.getAuthorName());
        vo.setCategoryName(work.getCategoryName());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setDescription(work.getDescription());
        vo.setCompletionStatus(work.getCompletionStatus());
        vo.setWordCount(work.getWordCount());
        vo.setLatestChapterTitle(work.getLatestChapterTitle());
        vo.setLatestChapterUpdatedAt(work.getLatestChapterUpdatedAt());
        vo.setAggregationStatus(work.getAggregationStatus());
        vo.setSourceCount(resolveSourceCount(work));
        WorkSourceBinding primary = workSourceBindingMapper.selectPrimaryByWorkId(workId);
        vo.setPrimarySource(toReadingSource(primary));
        Chapter latest = chapterMapper.selectLatestByWorkId(workId);
        vo.setLatestChapter(toReadingChapter(latest, null));
        return vo;
    }

    @Override
    public ReadingPageVO<ReadingSourceVO> sources(Long workId, int page, int pageSize) {
        requireWork(workId);
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        List<WorkSourceBinding> bindings = workSourceBindingMapper.selectActiveByWorkId(workId);
        int from = Math.min((safePage - 1) * safePageSize, bindings.size());
        int to = Math.min(from + safePageSize, bindings.size());
        List<ReadingSourceVO> list = bindings.subList(from, to).stream().map(this::toReadingSource).toList();
        return ReadingPageVO.of(list, bindings.size(), safePage, safePageSize);
    }

    @Override
    public ReadingPageVO<ReadingChapterVO> chapters(Long workId, Long sourceId, String refreshPolicy, int page, int pageSize) {
        requireWork(workId);
        String policy = normalizeRefreshPolicy(refreshPolicy);
        if (POLICY_FORCE_REFRESH.equals(policy)) {
            if (sourceId == null) {
                throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "force-refresh 需要提供 sourceId");
            }
            chapterSyncService.syncToc(workId, sourceId);
        }
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        Page<Chapter> chapterPage = chapterMapper.selectPageByWorkId(new Page<>(safePage, safePageSize), workId);
        Map<Long, ChapterSourceBinding> bindingMap = Collections.emptyMap();
        if (sourceId != null && !chapterPage.getRecords().isEmpty()) {
            Set<Long> chapterIds = chapterPage.getRecords().stream().map(Chapter::getId).collect(Collectors.toSet());
            bindingMap = chapterSourceBindingMapper.selectByWorkAndSource(workId, sourceId).stream()
                    .filter(binding -> binding.getChapterId() != null && chapterIds.contains(binding.getChapterId()))
                    .collect(Collectors.toMap(ChapterSourceBinding::getChapterId, Function.identity(), (a, b) -> a));
        }
        Map<Long, ChapterSourceBinding> finalBindingMap = bindingMap;
        List<ReadingChapterVO> list = chapterPage.getRecords().stream()
                .map(chapter -> toReadingChapter(chapter, finalBindingMap.get(chapter.getId())))
                .toList();
        return ReadingPageVO.of(list, chapterPage.getTotal(), safePage, safePageSize);
    }

    private String normalizeRefreshPolicy(String refreshPolicy) {
        String normalized = StringUtils.hasText(refreshPolicy) ? refreshPolicy.trim() : POLICY_CACHE_FIRST;
        if (!POLICY_CACHE_FIRST.equals(normalized) && !POLICY_FORCE_REFRESH.equals(normalized)) {
            throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的目录刷新策略: " + normalized);
        }
        return normalized;
    }

    private Work requireWork(Long workId) {
        if (workId == null) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "workId 不能为空");
        }
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException(ErrorCode.READING_WORK_NOT_FOUND, "作品不存在: " + workId);
        }
        return work;
    }

    private ReadingWorkVO toReadingWork(Work work) {
        ReadingWorkVO vo = new ReadingWorkVO();
        vo.setWorkId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setAuthorName(work.getAuthorName());
        vo.setCategoryName(work.getCategoryName());
        vo.setCoverUrl(work.getCoverUrl());
        vo.setCompletionStatus(work.getCompletionStatus());
        vo.setLatestChapterTitle(work.getLatestChapterTitle());
        vo.setAggregationStatus(work.getAggregationStatus());
        vo.setSourceCount(resolveSourceCount(work));
        return vo;
    }

    private ReadingWorkVO toReadingWork(WorkBriefVO work) {
        ReadingWorkVO vo = new ReadingWorkVO();
        vo.setWorkId(work.getWorkId());
        vo.setTitle(work.getTitle());
        vo.setAuthorName(work.getAuthor());
        vo.setAggregationStatus(work.getAggregationStatus());
        vo.setSourceCount(work.getSourceCount());
        return vo;
    }

    private ReadingSourceVO toReadingSource(WorkSourceBinding binding) {
        if (binding == null) {
            return null;
        }
        ReadingSourceVO vo = new ReadingSourceVO();
        vo.setSourceId(binding.getSourceId());
        vo.setPrimarySource(Objects.equals(binding.getIsPrimarySource(), 1));
        vo.setBindingStatus(binding.getBindingStatus());
        vo.setSourceBookName(binding.getSourceBookName());
        vo.setSourceAuthorName(binding.getSourceAuthorName());
        vo.setMatchConfidence(binding.getMatchConfidence());
        SourceDefinition source = sourceDefinitionMapper.selectById(binding.getSourceId());
        if (source != null) {
            vo.setSourceName(source.getName());
            vo.setSiteName(source.getSiteName());
            vo.setStatus(source.getStatus());
            vo.setPriority(source.getPriority());
        }
        return vo;
    }

    private ReadingChapterVO toReadingChapter(Chapter chapter, ChapterSourceBinding binding) {
        if (chapter == null) {
            return null;
        }
        ReadingChapterVO vo = new ReadingChapterVO();
        vo.setChapterId(chapter.getId());
        vo.setWorkId(chapter.getWorkId());
        vo.setChapterTitle(chapter.getChapterTitle());
        vo.setChapterIndex(chapter.getChapterIndex());
        vo.setVolumeName(chapter.getVolumeName());
        vo.setVipChapter(Objects.equals(chapter.getIsVipChapter(), 1));
        vo.setChapterStatus(chapter.getChapterStatus());
        if (binding != null) {
            vo.setSourceAvailable(true);
            vo.setSourceId(binding.getSourceId());
            vo.setSourceChapterTitle(binding.getSourceChapterTitle());
        }
        return vo;
    }

    private int resolveSourceCount(Work work) {
        if (work.getSourceCount() != null) {
            return work.getSourceCount();
        }
        return workSourceBindingMapper.countActiveByWorkId(work.getId());
    }

    private int validatePage(int page) {
        if (page < 1) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "page 必须大于等于 1");
        }
        return page;
    }

    private int validatePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "pageSize 必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
