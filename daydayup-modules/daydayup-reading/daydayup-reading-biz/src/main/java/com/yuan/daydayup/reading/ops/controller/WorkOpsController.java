package com.yuan.daydayup.reading.ops.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.TocSyncResultVO;
import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.repository.service.ChapterSyncService;
import com.yuan.daydayup.reading.repository.service.ContentDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作品运营 / 治理接口（仅内网可达）。
 *
 * <p>路径前缀 {@code /api/v1/internal/**}，网关不配置对外路由。</p>
 */
@Tag(name = "作品运营", description = "内容发现 / 作品入库 / 目录同步（内网）")
@RestController
@RequestMapping("/api/v1/internal/ops/works")
public class WorkOpsController {

    private final ContentDiscoveryService contentDiscoveryService;
    private final ChapterSyncService chapterSyncService;

    public WorkOpsController(ContentDiscoveryService contentDiscoveryService,
                            ChapterSyncService chapterSyncService) {
        this.contentDiscoveryService = contentDiscoveryService;
        this.chapterSyncService = chapterSyncService;
    }

    @Operation(summary = "内容发现", description = "指定书源搜索并把候选入库为统一作品 + 来源绑定（保守归并）")
    @PostMapping("/discover")
    public R<WorkDiscoveryResultVO> discover(@RequestParam("sourceId") Long sourceId,
                                             @RequestParam("keyword") String keyword,
                                             @RequestParam(name = "page", defaultValue = "1") int page) {
        return R.ok(contentDiscoveryService.discover(sourceId, keyword, page));
    }

    @Operation(summary = "目录同步", description = "抓取指定来源目录并资产化为统一章节（主来源）+ 来源章节绑定")
    @PostMapping("/{workId}/toc-sync")
    public R<TocSyncResultVO> tocSync(@PathVariable("workId") Long workId,
                                      @RequestParam("sourceId") Long sourceId) {
        return R.ok(chapterSyncService.syncToc(workId, sourceId));
    }
}
