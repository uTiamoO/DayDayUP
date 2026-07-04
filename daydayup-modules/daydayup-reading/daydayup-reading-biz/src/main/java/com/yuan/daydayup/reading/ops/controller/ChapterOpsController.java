package com.yuan.daydayup.reading.ops.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.ContentSnapshotVO;
import com.yuan.daydayup.reading.api.vo.SanitizeResultVO;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 章节运营 / 治理接口（仅内网可达）。
 *
 * <p>路径前缀 {@code /api/v1/internal/**}，网关不配置对外路由。</p>
 */
@Tag(name = "章节运营", description = "章节正文抓取 / 净化 / 快照（内网）")
@RestController
@RequestMapping("/api/v1/internal/ops/chapters")
public class ChapterOpsController {

    private final ContentFetchService contentFetchService;
    private final ContentSanitizeService contentSanitizeService;

    public ChapterOpsController(ContentFetchService contentFetchService,
                               ContentSanitizeService contentSanitizeService) {
        this.contentFetchService = contentFetchService;
        this.contentSanitizeService = contentSanitizeService;
    }

    @Operation(summary = "抓取章节正文", description = "抓取指定来源正文并落 raw + normalized 快照（cache-first，可强制回源）")
    @PostMapping("/{chapterId}/content-fetch")
    public R<ContentSnapshotVO> contentFetch(@PathVariable("chapterId") Long chapterId,
                                             @RequestParam("sourceId") Long sourceId,
                                             @RequestParam(name = "forceRefresh", defaultValue = "false")
                                             boolean forceRefresh) {
        return R.ok(contentFetchService.fetchAndStore(chapterId, sourceId, forceRefresh));
    }

    @Operation(summary = "净化章节正文", description = "对最新快照跑七段净化 Pipeline，写 sanitized 层并归档运行记录")
    @PostMapping("/{chapterId}/sanitize")
    public R<SanitizeResultVO> sanitize(@PathVariable("chapterId") Long chapterId,
                                        @RequestParam("sourceId") Long sourceId) {
        return R.ok(contentSanitizeService.sanitize(chapterId, sourceId));
    }
}
