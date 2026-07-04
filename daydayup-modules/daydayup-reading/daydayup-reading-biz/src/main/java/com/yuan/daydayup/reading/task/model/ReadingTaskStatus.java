package com.yuan.daydayup.reading.task.model;

/**
 * 阅读任务状态。
 */
public enum ReadingTaskStatus {

    PENDING("pending"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    PARTIAL_SUCCEEDED("partial_succeeded"),
    CANCELLED("cancelled");

    private final String code;

    ReadingTaskStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
