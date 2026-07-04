package com.yuan.daydayup.reading.ops.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.CompileBatchReportVO;
import com.yuan.daydayup.reading.api.vo.CompileReportVO;
import com.yuan.daydayup.reading.api.vo.SourceImportResultVO;
import com.yuan.daydayup.reading.compiler.service.RuleCompileService;
import com.yuan.daydayup.reading.source.mapper.SourceDefinitionMapper;
import com.yuan.daydayup.reading.source.service.SourceImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 书源运营 / 治理接口（仅内网可达）。
 *
 * <p>路径前缀 {@code /api/v1/internal/**}，网关不配置对外路由。</p>
 */
@Tag(name = "书源运营", description = "书源导入 / 编译 / 治理（内网）")
@RestController
@RequestMapping("/api/v1/internal/ops/sources")
public class SourceOpsController {

    private final SourceImportService sourceImportService;
    private final RuleCompileService ruleCompileService;
    private final SourceDefinitionMapper sourceDefinitionMapper;

    public SourceOpsController(SourceImportService sourceImportService,
                               RuleCompileService ruleCompileService,
                               SourceDefinitionMapper sourceDefinitionMapper) {
        this.sourceImportService = sourceImportService;
        this.ruleCompileService = ruleCompileService;
        this.sourceDefinitionMapper = sourceDefinitionMapper;
    }

    @Operation(summary = "导入 Legado 书源", description = "扫描目录下的 Legado 书源 JSON，按 bookSourceUrl 幂等 upsert")
    @PostMapping("/import")
    public R<SourceImportResultVO> importSources(
            @RequestParam(name = "dir", required = false) String dir) {
        return R.ok(sourceImportService.importFromDirectory(dir));
    }

    @Operation(summary = "编译单个书源", description = "将指定书源的 Legado 规则编译为内部 RuleModel，产出编译体检")
    @PostMapping("/{id}/compile")
    public R<CompileReportVO> compile(@PathVariable("id") Long id) {
        return R.ok(ruleCompileService.compileOne(id));
    }

    @Operation(summary = "编译全部启用书源", description = "批量编译，产出 full/degraded/rejected 覆盖率报告")
    @PostMapping("/compile-all")
    public R<CompileBatchReportVO> compileAll() {
        return R.ok(ruleCompileService.compileAllEnabled());
    }

    @Operation(summary = "书源总数", description = "当前已入库的书源数量")
    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(sourceDefinitionMapper.selectCount(null));
    }
}
