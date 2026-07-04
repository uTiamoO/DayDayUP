package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;

import java.util.List;

/**
 * 章节装配服务（content-repository，spec §5.3）：把某来源目录物化为
 * 统一章节（仅主来源）+ 来源章节绑定。
 */
public interface ChapterAssemblyService {

    /**
     * 目录同步（先清后建，避免 uk 冲突）。
     *
     * @param workId        统一作品 id
     * @param sourceId      来源书源 id
     * @param primarySource 该来源是否主来源（主来源才建/重建统一 Chapter）
     * @param entries       归一化章节条目（目录序）
     */
    TocSyncResultVO syncToc(Long workId, Long sourceId, boolean primarySource, List<TocEntry> entries);
}
