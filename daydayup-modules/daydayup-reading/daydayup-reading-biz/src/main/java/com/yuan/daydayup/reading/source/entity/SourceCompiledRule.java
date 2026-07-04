package com.yuan.daydayup.reading.source.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 书源编译产物（rule-compiler 输出，对应 spec 3.2.2 SourceCompiledRule）。
 *
 * <p>保存某书源被编译后的内部 RuleModel（JSON）、编译等级与体检信息。一书源保留最新一版
 * （按 {@code sourceId} 幂等），历史版本化留待后续切片。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reading_source_compiled_rule")
public class SourceCompiledRule extends BaseEntity {

    /** 关联书源 id */
    private Long sourceId;

    /** 编译器版本 */
    private String compilerVersion;

    /** RuleModel schema 版本 */
    private String dslSchemaVersion;

    /** 编译等级：full / degraded / rejected */
    private String compileStatus;

    /** 编译警告（JSON 数组） */
    private String compileWarnings;

    /** script 依赖明细（JSON 数组，degraded 溯源） */
    private String scriptDeps;

    /** 编译产物：RuleModel 序列化 JSON */
    private String compiledContent;

    /** 编译时间 */
    private LocalDateTime compiledAt;
}
