package com.yuan.daydayup.reading.pipeline.service;

import com.yuan.daydayup.reading.api.vo.SanitizeResultVO;

/**
 * 正文净化服务（content-pipeline，spec §5.4/§7）。
 */
public interface ContentSanitizeService {

    /**
     * 对某章节某来源的最新快照执行净化 Pipeline，写 sanitized 层并归档运行记录。
     * accepted/degraded 发布为 sanitized；rejected 不覆盖已发布正文。
     */
    SanitizeResultVO sanitize(Long chapterId, Long sourceId);
}
