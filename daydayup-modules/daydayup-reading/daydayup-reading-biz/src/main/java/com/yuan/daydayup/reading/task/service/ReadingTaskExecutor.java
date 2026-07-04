package com.yuan.daydayup.reading.task.service;

import com.yuan.daydayup.reading.task.entity.ReadingTask;

/**
 * 阅读任务执行器。
 */
public interface ReadingTaskExecutor {

    /**
     * 执行任务。
     *
     * @return true 表示 succeeded，false 表示 partial_succeeded
     */
    boolean execute(ReadingTask task);
}
