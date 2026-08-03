package com.yuan.daydayup.reading.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.ReadingChapterVO;
import com.yuan.daydayup.reading.api.vo.ReadingContentVO;
import com.yuan.daydayup.reading.api.vo.ReadingFilterVO;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingSourceVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkDetailVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkVO;
import com.yuan.daydayup.reading.service.ReadingContentService;
import com.yuan.daydayup.reading.service.ReadingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统一阅读用户 API。
 */
@Tag(name = "统一阅读 API", description = "面向调用方的作品、来源、章节与正文接口")
@RestController
@RequestMapping("/api/v1/reading")
public class ReadingController {

    private final ReadingQueryService readingQueryService;
    private final ReadingContentService readingContentService;

    public ReadingController(ReadingQueryService readingQueryService,
                             ReadingContentService readingContentService) {
        this.readingQueryService = readingQueryService;
        this.readingContentService = readingContentService;
    }

    @Operation(summary = "阅读搜索", description = "aggregate 模式查统一作品；source 模式指定书源搜索并资产化")
    @GetMapping("/search")
    public R<ReadingPageVO<ReadingWorkVO>> search(
            @RequestParam(name = "mode", defaultValue = "aggregate") String mode,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "completionStatus", required = false) String completionStatus,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return R.ok(readingQueryService.search(mode, keyword, sourceId, category, completionStatus, page, pageSize));
    }

    @Operation(summary = "作品列表")
    @GetMapping("/works")
    public R<ReadingPageVO<ReadingWorkVO>> works(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return R.ok(readingQueryService.works(keyword, category, status, sourceId, sort, page, pageSize));
    }

    @Operation(summary = "作品详情", description = "默认优先读缓存，详情不完整时从主来源回源；force-refresh 可指定来源强制刷新")
    @GetMapping("/works/{workId}")
    public R<ReadingWorkDetailVO> detail(
            @PathVariable("workId") Long workId,
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "refreshPolicy", defaultValue = "cache-first") String refreshPolicy) {
        return R.ok(readingQueryService.detail(workId, sourceId, refreshPolicy));
    }

    @Operation(summary = "作品来源列表")
    @GetMapping("/works/{workId}/sources")
    public R<ReadingPageVO<ReadingSourceVO>> sources(
            @PathVariable("workId") Long workId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return R.ok(readingQueryService.sources(workId, page, pageSize));
    }

    @Operation(summary = "可用书源列表", description = "用于书源切换入口与筛选面板，只返回平台书源摘要，不暴露源站 URL")
    @GetMapping("/sources")
    public R<ReadingPageVO<ReadingSourceVO>> sourceDefinitions(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {
        return R.ok(readingQueryService.sourceDefinitions(page, pageSize));
    }

    @Operation(summary = "分类筛选元数据")
    @GetMapping("/categories")
    public R<List<ReadingFilterVO>> categories(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return R.ok(readingQueryService.categories(limit));
    }

    @Operation(summary = "筛选元数据", description = "type 支持 all/category/completionStatus")
    @GetMapping("/filters")
    public R<List<ReadingFilterVO>> filters(
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return R.ok(readingQueryService.filters(type, limit));
    }

    @Operation(summary = "作品章节列表")
    @GetMapping("/works/{workId}/chapters")
    public R<ReadingPageVO<ReadingChapterVO>> chapters(
            @PathVariable("workId") Long workId,
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "refreshPolicy", defaultValue = "cache-first") String refreshPolicy,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "50") int pageSize) {
        return R.ok(readingQueryService.chapters(workId, sourceId, refreshPolicy, page, pageSize));
    }

    @Operation(summary = "章节正文", description = "默认返回 sanitized；latest 等价于 sanitized；force-refresh 强制回源并重新净化")
    @GetMapping("/chapters/{chapterId}/content")
    public R<ReadingContentVO> content(
            @PathVariable("chapterId") Long chapterId,
            @RequestParam("sourceId") Long sourceId,
            @RequestParam(name = "contentVersion", defaultValue = "sanitized") String contentVersion,
            @RequestParam(name = "fetchPolicy", defaultValue = "cache-first") String fetchPolicy) {
        return R.ok(readingContentService.content(chapterId, sourceId, contentVersion, fetchPolicy));
    }
}
