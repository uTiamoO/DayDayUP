package com.yuan.daydayup.reading.repository.service;

/**
 * 来源作品详情同步服务。
 */
public interface WorkDetailSyncService {

    default WorkDetailSyncResult syncDetail(Long workId, Long sourceId) {
        return syncDetail(workId, sourceId, true);
    }

    WorkDetailSyncResult syncDetail(Long workId, Long sourceId, boolean updateWork);
}
