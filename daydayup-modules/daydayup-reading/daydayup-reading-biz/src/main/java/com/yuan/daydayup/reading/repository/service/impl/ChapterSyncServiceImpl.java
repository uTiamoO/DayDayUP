package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.ChapterAssemblyService;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.TocEntry;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 目录同步编排实现：从 WorkSourceBinding 取来源书籍 URL 与主来源标记，
 * 定向抓目录后交给 {@link ChapterAssemblyService} 物化。
 */
@Service
public class ChapterSyncServiceImpl implements ChapterSyncService {

    private final WorkSourceBindingMapper bindingMapper;
    private final SourceReadingService sourceReadingService;
    private final ChapterAssemblyService chapterAssemblyService;

    public ChapterSyncServiceImpl(WorkSourceBindingMapper bindingMapper,
                                  SourceReadingService sourceReadingService,
                                  ChapterAssemblyService chapterAssemblyService) {
        this.bindingMapper = bindingMapper;
        this.sourceReadingService = sourceReadingService;
        this.chapterAssemblyService = chapterAssemblyService;
    }

    @Override
    public TocSyncResultVO syncToc(Long workId, Long sourceId) {
        WorkSourceBinding binding = bindingMapper.selectByWorkAndSource(workId, sourceId);
        if (binding == null) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                    "作品未绑定该来源: workId=" + workId + " sourceId=" + sourceId);
        }
        if (!StringUtils.hasText(binding.getSourceBookUrl())) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE, "来源绑定缺少书籍 URL");
        }

        long start = System.currentTimeMillis();
        DirectedReadVO read = sourceReadingService.toc(sourceId, binding.getSourceBookUrl());
        boolean primary = binding.getIsPrimarySource() != null && binding.getIsPrimarySource() == 1;

        TocSyncResultVO result = chapterAssemblyService.syncToc(
                workId, sourceId, primary, TocEntry.from(read.getRecords()));
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }
}
