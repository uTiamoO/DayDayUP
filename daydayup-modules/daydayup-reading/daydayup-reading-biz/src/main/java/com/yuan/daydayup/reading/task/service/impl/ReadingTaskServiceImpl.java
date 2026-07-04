package com.yuan.daydayup.reading.task.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskPageQueryDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskSubmitDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskVO;
import com.yuan.daydayup.reading.task.config.ReadingTaskProperties;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.mapper.ReadingTaskMapper;
import com.yuan.daydayup.reading.task.model.ReadingTaskStatus;
import com.yuan.daydayup.reading.task.model.ReadingTaskType;
import com.yuan.daydayup.reading.task.service.ReadingTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 阅读任务服务实现。
 */
@Service
public class ReadingTaskServiceImpl implements ReadingTaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int CANDIDATE_LIMIT = 20;
    private static final int MAX_ERROR_LENGTH = 2000;

    private final ReadingTaskMapper taskMapper;
    private final ReadingTaskProperties properties;
    private final ObjectMapper objectMapper;

    public ReadingTaskServiceImpl(ReadingTaskMapper taskMapper,
                                  ReadingTaskProperties properties,
                                  ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReadingTaskVO submit(ReadingTaskSubmitDTO request) {
        if (request == null || !StringUtils.hasText(request.getTaskType())) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "taskType 不能为空");
        }
        ReadingTaskType type = parseType(request.getTaskType());
        String payload = serializePayload(request.getPayload());
        String bizKey = StringUtils.hasText(request.getBizKey())
                ? request.getBizKey().trim()
                : defaultBizKey(type, payload);
        ReadingTask active = taskMapper.selectActive(type.code(), bizKey);
        if (active != null) {
            return toVO(active);
        }
        LocalDateTime now = LocalDateTime.now();
        ReadingTask task = new ReadingTask();
        task.setTaskType(type.code());
        task.setBizKey(bizKey);
        task.setPayload(payload);
        task.setTaskStatus(ReadingTaskStatus.PENDING.code());
        task.setRetryCount(0);
        task.setMaxRetry(normalizeMaxRetry(request.getMaxRetry()));
        task.setNextRunAt(request.getNextRunAt() == null ? now : request.getNextRunAt());
        taskMapper.insert(task);
        return toVO(task);
    }

    @Override
    public ReadingTaskVO get(Long taskId) {
        return toVO(requiredTask(taskId));
    }

    @Override
    public ReadingPageVO<ReadingTaskVO> page(ReadingTaskPageQueryDTO query) {
        int page = normalizePage(query == null ? null : query.getPage());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        String taskType = null;
        if (query != null && StringUtils.hasText(query.getTaskType())) {
            taskType = parseType(query.getTaskType()).code();
        }
        String status = query == null ? null : trimToNull(query.getStatus());
        Page<ReadingTask> result = taskMapper.selectTaskPage(new Page<>(page, pageSize), taskType, status);
        List<ReadingTaskVO> list = result.getRecords().stream().map(this::toVO).toList();
        return ReadingPageVO.of(list, result.getTotal(), page, pageSize);
    }

    @Override
    public ReadingTaskVO cancel(Long taskId) {
        requiredTask(taskId);
        int updated = taskMapper.cancelPending(taskId, LocalDateTime.now());
        if (updated == 0) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "仅 pending 任务可取消");
        }
        return get(taskId);
    }

    @Override
    public ReadingTask acquireOne(String workerId) {
        String lockOwner = StringUtils.hasText(workerId) ? workerId : "reading-task-worker";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredBefore = now.minusNanos(properties.getLockTimeoutMs() * 1_000_000L);
        List<ReadingTask> candidates = taskMapper.selectAcquireCandidates(now, expiredBefore, CANDIDATE_LIMIT);
        for (ReadingTask candidate : candidates) {
            int updated = taskMapper.tryAcquire(candidate.getId(), lockOwner, now, expiredBefore);
            if (updated > 0) {
                return taskMapper.selectById(candidate.getId());
            }
        }
        return null;
    }

    @Override
    public void markSucceeded(Long taskId) {
        taskMapper.markFinished(taskId, ReadingTaskStatus.SUCCEEDED.code(), LocalDateTime.now());
    }

    @Override
    public void markPartial(Long taskId) {
        taskMapper.markFinished(taskId, ReadingTaskStatus.PARTIAL_SUCCEEDED.code(), LocalDateTime.now());
    }

    @Override
    public boolean markFailed(Long taskId, Throwable throwable) {
        ReadingTask task = requiredTask(taskId);
        LocalDateTime now = LocalDateTime.now();
        int nextRetryCount = Objects.requireNonNullElse(task.getRetryCount(), 0) + 1;
        int maxRetry = Objects.requireNonNullElse(task.getMaxRetry(), properties.getDefaultMaxRetry());
        String errorMessage = truncate(throwable == null ? "任务执行失败" : throwable.getMessage());
        if (nextRetryCount < maxRetry) {
            LocalDateTime nextRunAt = now.plusNanos(properties.getRetryBackoffMs() * 1_000_000L);
            taskMapper.markFailure(taskId, ReadingTaskStatus.PENDING.code(), nextRetryCount,
                    nextRunAt, errorMessage, null, now);
            return true;
        }
        taskMapper.markFailure(taskId, ReadingTaskStatus.FAILED.code(), nextRetryCount,
                null, errorMessage, now, now);
        return false;
    }

    private ReadingTask requiredTask(Long taskId) {
        if (taskId == null) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "taskId 不能为空");
        }
        ReadingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "任务不存在: " + taskId);
        }
        return task;
    }

    private ReadingTaskType parseType(String code) {
        try {
            return ReadingTaskType.of(code);
        } catch (IllegalArgumentException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, e.getMessage());
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : new LinkedHashMap<>(payload);
        try {
            return objectMapper.writeValueAsString(safePayload);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "payload 序列化失败: " + e.getMessage());
        }
    }

    private String defaultBizKey(ReadingTaskType type, String payload) {
        return type.code() + ":" + Integer.toHexString(payload == null ? 0 : payload.hashCode());
    }

    private int normalizeMaxRetry(Integer maxRetry) {
        int value = maxRetry == null ? properties.getDefaultMaxRetry() : maxRetry;
        if (value < 1) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "maxRetry 必须大于等于 1");
        }
        return value;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 1;
        }
        if (page < 1) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "page 必须大于等于 1");
        }
        return page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 20;
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "pageSize 必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        return pageSize;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String message) {
        String safe = StringUtils.hasText(message) ? message : "任务执行失败";
        return safe.length() > MAX_ERROR_LENGTH ? safe.substring(0, MAX_ERROR_LENGTH) : safe;
    }

    private ReadingTaskVO toVO(ReadingTask task) {
        ReadingTaskVO vo = new ReadingTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setBizKey(task.getBizKey());
        vo.setPayload(task.getPayload());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setNextRunAt(task.getNextRunAt());
        vo.setLockedBy(task.getLockedBy());
        vo.setLockedAt(task.getLockedAt());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }
}
