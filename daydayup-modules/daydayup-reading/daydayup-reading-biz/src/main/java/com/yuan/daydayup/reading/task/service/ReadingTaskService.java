package com.yuan.daydayup.reading.task.service;

import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskPageQueryDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskSubmitDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskVO;
import com.yuan.daydayup.reading.task.entity.ReadingTask;

/**
 * 阅读任务服务。
 */
public interface ReadingTaskService {

    ReadingTaskVO submit(ReadingTaskSubmitDTO request);

    ReadingTaskVO get(Long taskId);

    ReadingPageVO<ReadingTaskVO> page(ReadingTaskPageQueryDTO query);

    ReadingTaskVO cancel(Long taskId);

    ReadingTask acquireOne(String workerId);

    void markSucceeded(Long taskId);

    void markPartial(Long taskId);

    boolean markFailed(Long taskId, Throwable throwable);
}
