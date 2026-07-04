package com.yuan.daydayup.reading.task.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.compiler.service.RuleCompileService;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import com.yuan.daydayup.reading.source.service.SourceImportService;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.model.ReadingTaskType;
import com.yuan.daydayup.reading.task.service.ReadingTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 阅读任务执行器实现。
 */
@Service
public class ReadingTaskExecutorImpl implements ReadingTaskExecutor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SourceImportService sourceImportService;
    private final RuleCompileService ruleCompileService;
    private final ContentDiscoveryService contentDiscoveryService;
    private final ChapterSyncService chapterSyncService;
    private final ContentFetchService contentFetchService;
    private final ContentSanitizeService contentSanitizeService;
    private final ObjectMapper objectMapper;

    public ReadingTaskExecutorImpl(SourceImportService sourceImportService,
                                   RuleCompileService ruleCompileService,
                                   ContentDiscoveryService contentDiscoveryService,
                                   ChapterSyncService chapterSyncService,
                                   ContentFetchService contentFetchService,
                                   ContentSanitizeService contentSanitizeService,
                                   ObjectMapper objectMapper) {
        this.sourceImportService = sourceImportService;
        this.ruleCompileService = ruleCompileService;
        this.contentDiscoveryService = contentDiscoveryService;
        this.chapterSyncService = chapterSyncService;
        this.contentFetchService = contentFetchService;
        this.contentSanitizeService = contentSanitizeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean execute(ReadingTask task) {
        if (task == null || !StringUtils.hasText(task.getTaskType())) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "任务类型不能为空");
        }
        Map<String, Object> payload = payload(task.getPayload());
        ReadingTaskType type = parseType(task.getTaskType());
        switch (type) {
            case SOURCE_IMPORT -> sourceImportService.importFromDirectory(optionalString(payload, "dir"));
            case SOURCE_COMPILE -> executeCompile(payload);
            case WORK_DISCOVERY -> contentDiscoveryService.discover(
                    requiredLong(payload, "sourceId"), requiredString(payload, "keyword"), optionalInt(payload, "page", 1));
            case TOC_SYNC -> chapterSyncService.syncToc(requiredLong(payload, "workId"), requiredLong(payload, "sourceId"));
            case CONTENT_FETCH -> contentFetchService.fetchAndStore(requiredLong(payload, "chapterId"),
                    requiredLong(payload, "sourceId"), optionalBoolean(payload, "forceRefresh", false));
            case CONTENT_SANITIZE -> contentSanitizeService.sanitize(requiredLong(payload, "chapterId"),
                    requiredLong(payload, "sourceId"));
            default -> throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "不支持的任务类型: " + task.getTaskType());
        }
        return true;
    }

    private void executeCompile(Map<String, Object> payload) {
        if (optionalBoolean(payload, "all", false)) {
            ruleCompileService.compileAllEnabled();
            return;
        }
        ruleCompileService.compileOne(requiredLong(payload, "sourceId"));
    }

    private ReadingTaskType parseType(String code) {
        try {
            return ReadingTaskType.of(code);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, e.getMessage());
        }
    }

    private Map<String, Object> payload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload JSON 非法: " + e.getMessage());
        }
    }

    private Long requiredLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload 缺少字段: " + key);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload 字段必须为数字: " + key);
        }
    }

    private String requiredString(Map<String, Object> payload, String key) {
        String value = optionalString(payload, key);
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload 缺少字段: " + key);
        }
        return value;
    }

    private String optionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private int optionalInt(Map<String, Object> payload, String key, int defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload 字段必须为整数: " + key);
        }
    }

    private boolean optionalBoolean(Map<String, Object> payload, String key, boolean defaultValue) {
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
