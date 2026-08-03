package com.yuan.daydayup.reading.repository.service;

/**
 * 来源详情同步结果。
 *
 * @param workId        统一作品 ID
 * @param sourceId      实际使用的书源 ID
 * @param primarySource 是否为作品主来源
 * @param tocUrl        详情页解析出的目录 URL
 */
public record WorkDetailSyncResult(
        Long workId,
        Long sourceId,
        boolean primarySource,
        String tocUrl) {
}
