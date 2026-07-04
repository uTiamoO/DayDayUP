package com.yuan.daydayup.reading.task.model;

import java.util.Arrays;

/**
 * 阅读任务类型。
 */
public enum ReadingTaskType {

    SOURCE_IMPORT("source_import"),
    SOURCE_COMPILE("source_compile"),
    WORK_DISCOVERY("work_discovery"),
    TOC_SYNC("toc_sync"),
    CONTENT_FETCH("content_fetch"),
    CONTENT_SANITIZE("content_sanitize");

    private final String code;

    ReadingTaskType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ReadingTaskType of(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的任务类型: " + code));
    }
}
