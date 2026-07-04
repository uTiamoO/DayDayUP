package com.yuan.daydayup.reading.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingTaskPageQueryDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskSubmitDTO;
import com.yuan.daydayup.reading.task.config.ReadingTaskProperties;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.mapper.ReadingTaskMapper;
import com.yuan.daydayup.reading.task.model.ReadingTaskStatus;
import com.yuan.daydayup.reading.task.service.impl.ReadingTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadingTaskServiceTest {

    private ReadingTaskMapper mapper;
    private ReadingTaskService service;
    private final ReadingTaskProperties properties = new ReadingTaskProperties();

    @BeforeEach
    void setUp() {
        mapper = mock(ReadingTaskMapper.class);
        properties.setDefaultMaxRetry(3);
        properties.setRetryBackoffMs(1000);
        properties.setLockTimeoutMs(300000);
        service = new ReadingTaskServiceImpl(mapper, properties, new ObjectMapper());
    }

    @Test
    void submitReturnsActiveTaskWhenDuplicate() {
        ReadingTask active = task(1L, ReadingTaskStatus.PENDING.code());
        when(mapper.selectActive("toc_sync", "work:1:source:2")).thenReturn(active);
        ReadingTaskSubmitDTO request = new ReadingTaskSubmitDTO();
        request.setTaskType("toc_sync");
        request.setBizKey("work:1:source:2");
        request.setPayload(Map.of("workId", 1, "sourceId", 2));

        assertEquals(1L, service.submit(request).getId());
    }

    @Test
    void submitInsertsPendingTaskWhenNoActiveTask() {
        doAnswer(invocation -> {
            ReadingTask task = invocation.getArgument(0);
            task.setId(10L);
            return 1;
        }).when(mapper).insert(any(ReadingTask.class));
        ReadingTaskSubmitDTO request = new ReadingTaskSubmitDTO();
        request.setTaskType("content_fetch");
        request.setBizKey("chapter:1:source:2");
        request.setPayload(Map.of("chapterId", 1, "sourceId", 2));

        assertEquals(10L, service.submit(request).getId());
        verify(mapper).insert(any(ReadingTask.class));
    }

    @Test
    void cancelOnlyAllowsPendingTasks() {
        when(mapper.selectById(1L)).thenReturn(task(1L, ReadingTaskStatus.RUNNING.code()));
        when(mapper.cancelPending(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        assertThrows(BizException.class, () -> service.cancel(1L));
    }

    @Test
    void acquireSkipsFutureTasksAndAcquiresCandidateByConditionalUpdate() {
        ReadingTask candidate = task(2L, ReadingTaskStatus.PENDING.code());
        when(mapper.selectAcquireCandidates(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(candidate));
        when(mapper.tryAcquire(eq(2L), eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        ReadingTask running = task(2L, ReadingTaskStatus.RUNNING.code());
        running.setLockedBy("worker-1");
        when(mapper.selectById(2L)).thenReturn(running);

        ReadingTask acquired = service.acquireOne("worker-1");

        assertNotNull(acquired);
        assertEquals(2L, acquired.getId());
    }

    @Test
    void acquireReturnsNullWhenNoDueCandidates() {
        when(mapper.selectAcquireCandidates(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of());

        assertNull(service.acquireOne("worker-1"));
    }

    @Test
    void markSuccessAndPartialFinishTask() {
        service.markSucceeded(1L);
        service.markPartial(2L);

        verify(mapper).markFinished(eq(1L), eq("succeeded"), any(LocalDateTime.class));
        verify(mapper).markFinished(eq(2L), eq("partial_succeeded"), any(LocalDateTime.class));
    }

    @Test
    void markFailedRetriesBeforeMaxRetryAndFailsWhenExhausted() {
        ReadingTask retryTask = task(1L, ReadingTaskStatus.RUNNING.code());
        retryTask.setRetryCount(0);
        retryTask.setMaxRetry(2);
        when(mapper.selectById(1L)).thenReturn(retryTask);

        assertTrue(service.markFailed(1L, new IllegalStateException("boom")));
        verify(mapper).markFailure(eq(1L), eq("pending"), eq(1), any(LocalDateTime.class), eq("boom"), eq(null), any(LocalDateTime.class));

        ReadingTask failTask = task(2L, ReadingTaskStatus.RUNNING.code());
        failTask.setRetryCount(1);
        failTask.setMaxRetry(2);
        when(mapper.selectById(2L)).thenReturn(failTask);

        assertEquals(false, service.markFailed(2L, new IllegalStateException("bad")));
        verify(mapper).markFailure(eq(2L), eq("failed"), eq(2), eq(null), eq("bad"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void pageNormalizesQuery() {
        Page<ReadingTask> page = new Page<>(1, 20);
        page.setRecords(List.of(task(1L, "pending")));
        page.setTotal(1);
        when(mapper.selectTaskPage(any(Page.class), eq("source_compile"), eq("pending"))).thenReturn(page);
        ReadingTaskPageQueryDTO query = new ReadingTaskPageQueryDTO();
        query.setTaskType("source_compile");
        query.setStatus("pending");

        assertEquals(1, service.page(query).getTotal());
    }

    private ReadingTask task(Long id, String status) {
        ReadingTask task = new ReadingTask();
        task.setId(id);
        task.setTaskType("toc_sync");
        task.setBizKey("key");
        task.setPayload("{}");
        task.setTaskStatus(status);
        task.setRetryCount(0);
        task.setMaxRetry(3);
        return task;
    }
}
