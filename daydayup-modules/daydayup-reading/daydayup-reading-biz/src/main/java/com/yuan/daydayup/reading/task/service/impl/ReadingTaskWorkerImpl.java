package com.yuan.daydayup.reading.task.service.impl;

import com.yuan.daydayup.reading.api.vo.ReadingTaskDrainVO;
import com.yuan.daydayup.reading.task.config.ReadingTaskProperties;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import com.yuan.daydayup.reading.task.service.ReadingTaskExecutor;
import com.yuan.daydayup.reading.task.service.ReadingTaskService;
import com.yuan.daydayup.reading.task.service.ReadingTaskWorker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 阅读任务 worker。
 */
@Service
public class ReadingTaskWorkerImpl implements ReadingTaskWorker {

    private final ReadingTaskService taskService;
    private final ReadingTaskExecutor taskExecutor;
    private final ReadingTaskProperties properties;
    private final String workerId;

    public ReadingTaskWorkerImpl(ReadingTaskService taskService,
                                 ReadingTaskExecutor taskExecutor,
                                 ReadingTaskProperties properties) {
        this.taskService = taskService;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.workerId = buildWorkerId();
    }

    @Override
    public ReadingTaskDrainVO drain(int limit) {
        int safeLimit = limit > 0 ? limit : properties.getBatchSize();
        ReadingTaskDrainVO vo = new ReadingTaskDrainVO();
        vo.setLimit(safeLimit);
        for (int i = 0; i < safeLimit; i++) {
            ReadingTask task = taskService.acquireOne(workerId);
            if (task == null) {
                break;
            }
            vo.setAcquired(vo.getAcquired() + 1);
            vo.getTaskIds().add(task.getId());
            try {
                boolean success = taskExecutor.execute(task);
                if (success) {
                    taskService.markSucceeded(task.getId());
                    vo.setSucceeded(vo.getSucceeded() + 1);
                } else {
                    taskService.markPartial(task.getId());
                    vo.setPartialSucceeded(vo.getPartialSucceeded() + 1);
                }
            } catch (Exception e) {
                boolean retry = taskService.markFailed(task.getId(), e);
                if (retry) {
                    vo.setRetryScheduled(vo.getRetryScheduled() + 1);
                } else {
                    vo.setFailed(vo.getFailed() + 1);
                }
            }
        }
        return vo;
    }

    @Scheduled(fixedDelayString = "${reading.task.worker-interval-ms:5000}")
    public void scheduledDrain() {
        if (properties.isWorkerEnabled()) {
            drain(properties.getBatchSize());
        }
    }

    private String buildWorkerId() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }
        return "daydayup-reading:" + host + ":" + ManagementFactory.getRuntimeMXBean().getName();
    }
}
