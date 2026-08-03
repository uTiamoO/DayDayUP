package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncResult;
import com.yuan.daydayup.reading.repository.service.WorkDetailSyncService;
import com.yuan.daydayup.reading.repository.support.MatchKeys;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 来源作品详情同步实现。
 */
@Service
public class WorkDetailSyncServiceImpl implements WorkDetailSyncService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WorkMapper workMapper;
    private final WorkSourceBindingMapper bindingMapper;
    private final SourceReadingService sourceReadingService;

    public WorkDetailSyncServiceImpl(WorkMapper workMapper,
                                     WorkSourceBindingMapper bindingMapper,
                                     SourceReadingService sourceReadingService) {
        this.workMapper = workMapper;
        this.bindingMapper = bindingMapper;
        this.sourceReadingService = sourceReadingService;
    }

    @Override
    public WorkDetailSyncResult syncDetail(Long workId, Long sourceId, boolean updateWork) {
        Work work = requireWork(workId);
        WorkSourceBinding binding = resolveBinding(workId, sourceId);
        if (!StringUtils.hasText(binding.getSourceBookUrl())) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "来源绑定缺少书籍详情 URL");
        }

        DirectedReadVO read = sourceReadingService.detail(binding.getSourceId(), binding.getSourceBookUrl());
        Map<String, String> record = read == null ? null : read.getRecord();
        if (record == null || record.isEmpty()) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "来源详情抓取结果为空");
        }

        boolean primarySource = Integer.valueOf(1).equals(binding.getIsPrimarySource());
        if (updateWork && primarySource) {
            applyWorkFields(work, record);
            work.setMatchKey(MatchKeys.of(work.getTitle(), work.getAuthorName()));
            workMapper.updateById(work);
        }
        applyBindingFields(binding, record);
        bindingMapper.updateById(binding);

        String tocUrl = text(record, "tocUrl");
        if (!StringUtils.hasText(tocUrl)) {
            tocUrl = binding.getSourceBookUrl();
        }
        return new WorkDetailSyncResult(workId, binding.getSourceId(), primarySource, tocUrl);
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

    private WorkSourceBinding resolveBinding(Long workId, Long sourceId) {
        WorkSourceBinding binding = sourceId == null
                ? bindingMapper.selectPrimaryByWorkId(workId)
                : bindingMapper.selectByWorkAndSource(workId, sourceId);
        if (binding == null) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                    sourceId == null ? "作品没有可用主来源" : "作品未绑定该来源: " + sourceId);
        }
        return binding;
    }

    private void applyWorkFields(Work work, Map<String, String> record) {
        setText(record, "name", work::setTitle);
        setText(record, "author", work::setAuthorName);
        setText(record, "kind", work::setCategoryName);
        setText(record, "coverUrl", work::setCoverUrl);
        setText(record, "intro", work::setDescription);
        setText(record, "status", value -> work.setCompletionStatus(normalizeStatus(value)));
        setText(record, "lastChapter", work::setLatestChapterTitle);

        Long wordCount = parseWordCount(text(record, "wordCount"));
        if (wordCount != null) {
            work.setWordCount(wordCount);
        }
        LocalDateTime updatedAt = parseDateTime(text(record, "updateTime"));
        if (updatedAt != null) {
            work.setLatestChapterUpdatedAt(updatedAt);
        }
    }

    private void applyBindingFields(WorkSourceBinding binding, Map<String, String> record) {
        setText(record, "name", binding::setSourceBookName);
        setText(record, "author", binding::setSourceAuthorName);
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase();
        if (normalized.contains("完结") || normalized.contains("完本") || normalized.contains("全本")
                || normalized.contains("completed") || normalized.contains("finish")) {
            return "completed";
        }
        if (normalized.contains("连载") || normalized.contains("serial") || normalized.contains("ongoing")) {
            return "serial";
        }
        return "unknown";
    }

    private Long parseWordCount(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace(",", "").replace("字", "");
        BigDecimal multiplier = BigDecimal.ONE;
        if (normalized.endsWith("万")) {
            multiplier = BigDecimal.valueOf(10_000L);
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("亿")) {
            multiplier = BigDecimal.valueOf(100_000_000L);
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("千")) {
            multiplier = BigDecimal.valueOf(1_000L);
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            return new BigDecimal(normalized).multiply(multiplier)
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String text(Map<String, String> record, String key) {
        String value = record.get(key);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void setText(Map<String, String> record, String key, java.util.function.Consumer<String> setter) {
        String value = text(record, key);
        if (value != null) {
            setter.accept(value);
        }
    }
}
