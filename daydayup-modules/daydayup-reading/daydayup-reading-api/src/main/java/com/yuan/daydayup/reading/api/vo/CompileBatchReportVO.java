package com.yuan.daydayup.reading.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量编译报告：编译覆盖率（full/degraded/rejected 三级比例，对应 spec §11.1）。
 */
@Data
public class CompileBatchReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int total;
    private int full;
    private int degraded;
    private int rejected;
    private int failed;

    /** 每个书源的编译结果明细 */
    private List<CompileReportVO> details = new ArrayList<>();

    /** full+degraded 覆盖率（百分比，保留整数） */
    public int coveragePercent() {
        return total == 0 ? 0 : (full + degraded) * 100 / total;
    }
}
