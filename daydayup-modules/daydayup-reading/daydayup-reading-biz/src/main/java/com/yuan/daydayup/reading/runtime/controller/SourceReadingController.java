package com.yuan.daydayup.reading.runtime.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 书源定向读取接口（内网调试 / 灰度，PRD §4.2）。
 *
 * <p>路径前缀 {@code /api/v1/internal/**}，网关不配置对外路由。</p>
 */
@Tag(name = "书源定向读取", description = "指定 sourceId 直取搜索 / 详情（内网调试）")
@RestController
@RequestMapping("/api/v1/internal/source-reading")
public class SourceReadingController {

    private final SourceReadingService sourceReadingService;

    public SourceReadingController(SourceReadingService sourceReadingService) {
        this.sourceReadingService = sourceReadingService;
    }

    @Operation(summary = "指定书源搜索", description = "按编译产物 RuleModel 构造搜索请求并抽取结果列表")
    @GetMapping("/{id}/search")
    public R<DirectedReadVO> search(@PathVariable("id") Long id,
                                    @RequestParam("keyword") String keyword,
                                    @RequestParam(name = "page", defaultValue = "1") int page) {
        return R.ok(sourceReadingService.search(id, keyword, page));
    }

    @Operation(summary = "指定书源详情", description = "bookUrl 为搜索结果产出的书籍 URL（相对或绝对，须与书源同域）")
    @GetMapping("/{id}/detail")
    public R<DirectedReadVO> detail(@PathVariable("id") Long id,
                                    @RequestParam("bookUrl") String bookUrl) {
        return R.ok(sourceReadingService.detail(id, bookUrl));
    }
}
