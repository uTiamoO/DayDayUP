package com.yuan.daydayup.reading.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 单个书源编译报告。
 */
@Data
public class CompileReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sourceId;
    private String name;

    /** 编译等级：full / degraded / rejected */
    private String grade;

    /** 已识别的 action 数（search/detail/toc/content/explore） */
    private int actionCount;

    /** 编译警告 */
    private List<String> warnings;

    /** script 依赖明细（degraded 溯源） */
    private List<String> scriptDeps;

    /** webView 依赖明细（rejected 溯源） */
    private List<String> webviewDeps;
}
