package com.yuan.daydayup.reading.service;

import com.yuan.daydayup.reading.api.vo.ReadingContentVO;

/**
 * 统一阅读正文编排服务。
 */
public interface ReadingContentService {

    ReadingContentVO content(Long chapterId, Long sourceId, String contentVersion, String fetchPolicy);
}
