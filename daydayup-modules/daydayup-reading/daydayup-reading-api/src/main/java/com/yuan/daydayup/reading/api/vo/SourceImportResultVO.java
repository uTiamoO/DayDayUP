package com.yuan.daydayup.reading.api.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 书源导入结果
 *
 * <p>source-center 导入的统计与错误明细。导入按 {@code bookSourceUrl} 幂等 upsert：
 * 新增、指纹变更则更新、指纹一致则跳过，单条失败隔离不影响整批。</p>
 */
@Data
public class SourceImportResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 扫描到的书源条目总数 */
    private int total;

    /** 新增数 */
    private int inserted;

    /** 更新数（指纹变更） */
    private int updated;

    /** 跳过数（指纹一致，无变更） */
    private int skipped;

    /** 失败数（解析或落库异常，已隔离） */
    private int failed;

    /** 失败明细（截断，便于排查） */
    private List<String> errors = new ArrayList<>();
}
