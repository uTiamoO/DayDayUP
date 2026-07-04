package com.yuan.daydayup.reading.compiler.service;

import com.yuan.daydayup.reading.api.vo.CompileBatchReportVO;
import com.yuan.daydayup.reading.api.vo.CompileReportVO;

/**
 * 书源编译服务（rule-compiler 门面）。
 *
 * <p>把 {@code SourceDefinition} 的原始 Legado JSON 编译为内部 RuleModel 并持久化，产出编译报告。</p>
 */
public interface RuleCompileService {

    /** 编译单个书源 */
    CompileReportVO compileOne(Long sourceId);

    /** 编译全部启用书源，产出覆盖率报告 */
    CompileBatchReportVO compileAllEnabled();
}
