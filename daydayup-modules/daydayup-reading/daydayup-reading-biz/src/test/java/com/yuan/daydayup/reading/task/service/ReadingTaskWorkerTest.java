package com.yuan.daydayup.reading.task.service;

import com.yuan.daydayup.reading.api.vo.ReadingTaskDrainVO;
import com.yuan.daydayup.reading.task.config.ReadingTaskProperties;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.service.impl.ReadingTaskWorkerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadingTaskWorkerTest {

    private ReadingTaskService taskService;
    private ReadingTaskExecutor taskExecutor;
    private ReadingTaskWorker worker;

    @BeforeEach
    void setUp() {
        taskService = mock(ReadingTaskService.class);
        taskExecutor = mock(ReadingTaskExecutor.class);
        ReadingTaskProperties properties = new ReadingTaskProperties();
        properties.setBatchSize(10);
        worker = new ReadingTaskWorkerImpl(taskService, taskExecutor, properties);
    }

    @Test
    void drainMarksSucceededAndStopsWhenNoMoreTask() {
        ReadingTask task = task(1L);
        when(taskService.acquireOne(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(task)
                .thenReturn(null);
        when(taskExecutor.execute(task)).thenReturn(true);

        ReadingTaskDrainVO result = worker.drain(5);

        assertEquals(1, result.getAcquired());
        assertEquals(1, result.getSucceeded());
        verify(taskService).markSucceeded(1L);
    }

    @Test
    void drainMarksPartialWhenExecutorReturnsFalse() {
        ReadingTask task = task(2L);
        when(taskService.acquireOne(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(task)
                .thenReturn(null);
        when(taskExecutor.execute(task)).thenReturn(false);

        ReadingTaskDrainVO result = worker.drain(5);

        assertEquals(1, result.getPartialSucceeded());
        verify(taskService).markPartial(2L);
    }

    @Test
    void drainRecordsRetryAndFinalFailure() {
        ReadingTask retryTask = task(3L);
        ReadingTask failedTask = task(4L);
        when(taskService.acquireOne(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(retryTask)
                .thenReturn(failedTask)
                .thenReturn(null);
        when(taskExecutor.execute(retryTask)).thenThrow(new IllegalStateException("retry"));
        when(taskExecutor.execute(failedTask)).thenThrow(new IllegalStateException("fail"));
        when(taskService.markFailed(org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(taskService.markFailed(org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.any())).thenReturn(false);

        ReadingTaskDrainVO result = worker.drain(5);

        assertEquals(2, result.getAcquired());
        assertEquals(1, result.getRetryScheduled());
        assertEquals(1, result.getFailed());
    }

    private ReadingTask task(Long id) {
        ReadingTask task = new ReadingTask();
        task.setId(id);
        task.setTaskType("source_compile");
        task.setPayload("{\"sourceId\":1}");
        return task;
    }
}
