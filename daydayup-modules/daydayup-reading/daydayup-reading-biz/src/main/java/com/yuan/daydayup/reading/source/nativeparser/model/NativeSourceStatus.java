package com.yuan.daydayup.reading.source.nativeparser.model;

/**
 * 原生书源单次动作的诊断状态（对齐 reading-api-contracts v3.0 per-source diagnostics）。
 *
 * <p>与 {@code source_time_cost} 语义一致：每次 search/discovery/detail/toc/content 都归一化到
 * 一个明确状态，供聚合诊断与来源健康使用；challenge 命中不得被当作普通 parse_error。</p>
 */
public enum NativeSourceStatus {

    /** 解析成功且有结果 */
    OK,

    /** 命中反爬校验（Cloudflare / Turnstile 等），需人工验证，禁止自动绕过 */
    VERIFICATION_REQUIRED,

    /** 被上游安全策略拦截（HTTP 403 等） */
    BLOCKED,

    /** 抓取超时 */
    TIMEOUT,

    /** 抓取失败（网络 / 非 2xx 非拦截） */
    FETCH_ERROR,

    /** 抓到内容但解析不出目标结构 */
    PARSE_ERROR,

    /** 正文 / 列表为空 */
    EMPTY
}
