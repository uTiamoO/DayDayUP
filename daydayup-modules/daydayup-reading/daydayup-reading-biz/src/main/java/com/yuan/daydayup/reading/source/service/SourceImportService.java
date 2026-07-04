package com.yuan.daydayup.reading.source.service;

import com.yuan.daydayup.reading.api.vo.SourceImportResultVO;

/**
 * 书源导入服务（source-center）
 *
 * <p>扫描目录下的 Legado 书源 JSON，按 {@code bookSourceUrl} 幂等落库为 SourceDefinition。
 * 第一期只负责「导入 + 保存原始内容」，编译（→ RuleModel）在后续切片。</p>
 */
public interface SourceImportService {

    /**
     * 从指定目录导入 Legado 书源 JSON。
     *
     * @param dir 目录（绝对路径，或相对于进程工作目录）；为空时使用配置默认目录
     * @return 导入统计
     */
    SourceImportResultVO importFromDirectory(String dir);
}
