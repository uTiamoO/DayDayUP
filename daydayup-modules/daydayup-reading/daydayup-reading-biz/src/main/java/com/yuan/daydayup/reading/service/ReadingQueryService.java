package com.yuan.daydayup.reading.service;

import com.yuan.daydayup.reading.api.vo.ReadingChapterVO;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingSourceVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkDetailVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkVO;

/**
 * 统一阅读查询编排服务。
 */
public interface ReadingQueryService {

    ReadingPageVO<ReadingWorkVO> search(String mode, String keyword, Long sourceId, String category,
                                         String completionStatus, int page, int pageSize);

    ReadingPageVO<ReadingWorkVO> works(String keyword, String category, String status, Long sourceId,
                                       String sort, int page, int pageSize);

    ReadingWorkDetailVO detail(Long workId);

    ReadingPageVO<ReadingSourceVO> sources(Long workId, int page, int pageSize);

    ReadingPageVO<ReadingChapterVO> chapters(Long workId, Long sourceId, String refreshPolicy, int page, int pageSize);
}
