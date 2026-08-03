package com.yuan.daydayup.reading.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingChapterVO;
import com.yuan.daydayup.reading.api.vo.ReadingFilterVO;
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
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncService;
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
    private final WorkDetailSyncService workDetailSyncService;

    public ReadingQueryServiceImpl(WorkMapper workMapper,
                                   WorkSourceBindingMapper workSourceBindingMapper,
                                   ChapterMapper chapterMapper,
                                   ChapterSourceBindingMapper chapterSourceBindingMapper,
                                   SourceDefinitionMapper sourceDefinitionMapper,
                                   ContentDiscoveryService contentDiscoveryService,
                                   ChapterSyncService chapterSyncService,
                                   WorkDetailSyncService workDetailSyncService) {
        this.workMapper = workMapper;
        this.workSourceBindingMapper = workSourceBindingMapper;
        this.chapterMapper = chapterMapper;
        this.chapterSourceBindingMapper = chapterSourceBindingMapper;
        this.sourceDefinitionMapper = sourceDefinitionMapper;
        this.contentDiscoveryService = contentDiscoveryService;
        this.chapterSyncService = chapterSyncService;
        this.workDetailSyncService = workDetailSyncService;
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
    public ReadingWorkDetailVO detail(Long workId, Long sourceId, String refreshPolicy) {
        Work work = requireWork(workId);
        String policy = normalizeRefreshPolicy(refreshPolicy, "详情");
        if (POLICY_FORCE_REFRESH.equals(policy)) {
            workDetailSyncService.syncDetail(workId, sourceId);
            work = requireWork(workId);
        } else if (isDetailCacheMiss(work)) {
            workDetailSyncService.syncDetail(workId, null);
            work = requireWork(workId);
        }
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
        Page<WorkSourceBinding> bindingPage = workSourceBindingMapper.selectActivePageByWorkId(
                new Page<>(safePage, safePageSize), workId);
        Map<Long, Map<String, Object>> sourceSummaryMap = bindingPage.getRecords().isEmpty()
                ? Collections.emptyMap()
                : workSourceBindingMapper.selectSourceSummariesByBindingIds(
                                bindingPage.getRecords().stream().map(WorkSourceBinding::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(row -> asLong(row.get("id")), Function.identity(), (a, b) -> a));
        List<ReadingSourceVO> list = bindingPage.getRecords().stream()
                .map(binding -> toReadingSource(binding, sourceSummaryMap.get(binding.getId())))
                .toList();
        return ReadingPageVO.of(list, bindingPage.getTotal(), safePage, safePageSize);
    }

    @Override
    public ReadingPageVO<ReadingSourceVO> sourceDefinitions(int page, int pageSize) {
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        Page<SourceDefinition> sourcePage = sourceDefinitionMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SourceDefinition>()
                        .eq(SourceDefinition::getStatus, 1)
                        .orderByDesc(SourceDefinition::getPriority)
                        .orderByAsc(SourceDefinition::getId));
        List<ReadingSourceVO> list = sourcePage.getRecords().stream().map(this::toReadingSource).toList();
        return ReadingPageVO.of(list, sourcePage.getTotal(), safePage, safePageSize);
    }

    @Override
    public List<ReadingFilterVO> categories(int limit) {
        return workMapper.selectCategoryCounts(validateLimit(limit)).stream()
                .map(row -> ReadingFilterVO.of(asString(row.get("name")), asString(row.get("name")), asLong(row.get("count"))))
                .toList();
    }

    @Override
    public List<ReadingFilterVO> filters(String type, int limit) {
        String safeType = StringUtils.hasText(type) ? type.trim() : "all";
        if ("category".equals(safeType)) {
            return categories(limit);
        }
        if ("completionStatus".equals(safeType) || "status".equals(safeType)) {
            return workMapper.selectCompletionStatusCounts().stream()
                    .map(row -> ReadingFilterVO.of(asString(row.get("name")), statusName(asString(row.get("name"))), asLong(row.get("count"))))
                    .toList();
        }
        if (!"all".equals(safeType)) {
            throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的筛选类型: " + safeType);
        }
        List<ReadingFilterVO> values = new java.util.ArrayList<>();
        values.addAll(categories(limit));
        values.addAll(workMapper.selectCompletionStatusCounts().stream()
                .map(row -> ReadingFilterVO.of("status:" + asString(row.get("name")), statusName(asString(row.get("name"))), asLong(row.get("count"))))
                .toList());
        return values;
    }

    @Override
    public ReadingPageVO<ReadingChapterVO> chapters(Long workId, Long sourceId, String refreshPolicy, int page, int pageSize) {
        requireWork(workId);
        String policy = normalizeRefreshPolicy(refreshPolicy, "目录");
        int safePage = validatePage(page);
        int safePageSize = validatePageSize(pageSize);
        if (POLICY_FORCE_REFRESH.equals(policy)) {
            if (sourceId == null) {
                throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "force-refresh 需要提供 sourceId");
            }
            chapterSyncService.syncToc(workId, sourceId);
        }
        Page<Chapter> chapterPage = chapterMapper.selectPageByWorkId(new Page<>(safePage, safePageSize), workId);
        List<ChapterSourceBinding> sourceBindings = Collections.emptyList();
        if (POLICY_CACHE_FIRST.equals(policy)) {
            Long syncSourceId = null;
            if (chapterPage.getTotal() == 0) {
                syncSourceId = sourceId != null ? sourceId : primarySourceId(workId);
            } else if (sourceId != null) {
                sourceBindings = chapterSourceBindingMapper.selectByWorkAndSource(workId, sourceId);
                if (sourceBindings.isEmpty()) {
                    syncSourceId = sourceId;
                }
            }
            if (syncSourceId != null) {
                chapterSyncService.syncToc(workId, syncSourceId);
                chapterPage = chapterMapper.selectPageByWorkId(new Page<>(safePage, safePageSize), workId);
                sourceBindings = Collections.emptyList();
            }
        }
        Map<Long, ChapterSourceBinding> bindingMap = Collections.emptyMap();
        if (sourceId != null && !chapterPage.getRecords().isEmpty()) {
            Set<Long> chapterIds = chapterPage.getRecords().stream().map(Chapter::getId).collect(Collectors.toSet());
            if (sourceBindings.isEmpty()) {
                sourceBindings = chapterSourceBindingMapper.selectByWorkAndSource(workId, sourceId);
            }
            bindingMap = sourceBindings.stream()
                    .filter(binding -> binding.getChapterId() != null && chapterIds.contains(binding.getChapterId()))
                    .collect(Collectors.toMap(ChapterSourceBinding::getChapterId, Function.identity(), (a, b) -> a));
        }
        Map<Long, ChapterSourceBinding> finalBindingMap = bindingMap;
        List<ReadingChapterVO> list = chapterPage.getRecords().stream()
                .map(chapter -> toReadingChapter(chapter, finalBindingMap.get(chapter.getId())))
                .toList();
        return ReadingPageVO.of(list, chapterPage.getTotal(), safePage, safePageSize);
    }

    private String normalizeRefreshPolicy(String refreshPolicy, String target) {
        String normalized = StringUtils.hasText(refreshPolicy) ? refreshPolicy.trim() : POLICY_CACHE_FIRST;
        if (!POLICY_CACHE_FIRST.equals(normalized) && !POLICY_FORCE_REFRESH.equals(normalized)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "不支持的" + target + "刷新策略: " + normalized);
        }
        return normalized;
    }

    private boolean isDetailCacheMiss(Work work) {
        return !StringUtils.hasText(work.getDescription())
                || !StringUtils.hasText(work.getCompletionStatus())
                || "unknown".equals(work.getCompletionStatus());
    }

    private Long primarySourceId(Long workId) {
        WorkSourceBinding primary = workSourceBindingMapper.selectPrimaryByWorkId(workId);
        if (primary == null || primary.getSourceId() == null) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "作品没有可用主来源");
        }
        return primary.getSourceId();
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

    private ReadingSourceVO toReadingSource(WorkSourceBinding binding, Map<String, Object> sourceSummary) {
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
        if (sourceSummary != null) {
            vo.setSourceName(asString(sourceSummary.get("sourceName")));
            vo.setSiteName(asString(sourceSummary.get("siteName")));
            vo.setStatus((int) asLong(sourceSummary.get("sourceStatus")));
            vo.setPriority((int) asLong(sourceSummary.get("sourcePriority")));
        }
        return vo;
    }

    private ReadingSourceVO toReadingSource(SourceDefinition source) {
        if (source == null) {
            return null;
        }
        ReadingSourceVO vo = new ReadingSourceVO();
        vo.setSourceId(source.getId());
        vo.setSourceName(source.getName());
        vo.setSiteName(source.getSiteName());
        vo.setStatus(source.getStatus());
        vo.setPriority(source.getPriority());
        vo.setBindingStatus(source.getCompileGrade());
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

    private int validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "limit 必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        return limit;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(asString(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String statusName(String status) {
        return switch (status) {
            case "serial" -> "连载";
            case "completed" -> "完结";
            case "unknown" -> "未知";
            default -> status;
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
