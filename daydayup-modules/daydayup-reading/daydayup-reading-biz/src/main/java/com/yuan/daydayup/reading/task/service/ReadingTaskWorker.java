package com.yuan.daydayup.reading.task.service;

import com.yuan.daydayup.reading.api.vo.ReadingTaskDrainVO;

/**
 * 阅读任务 worker。
 */
public interface ReadingTaskWorker {

    ReadingTaskDrainVO drain(int limit);
}
