package com.yuan.daydayup.reading.runtime.service;

import com.yuan.daydayup.reading.api.vo.DirectedReadVO;

/**
 * 书源定向读取服务（指定 sourceId 直取，PRD §4.2）。
 */
public interface SourceReadingService {

    /** 指定书源搜索：编译产物 RuleModel → 构造搜索请求 → 抓取 → 抽取记录列表 */
    DirectedReadVO search(Long sourceId, String keyword, int page);

    /** 指定书源详情：bookUrl（搜索结果产出，相对或绝对）→ 抓取 → ruleBookInfo 抽取 */
    DirectedReadVO detail(Long sourceId, String bookUrl);

    /** 指定书源目录：tocUrl（详情页/章节列表页 URL）→ 抓取 → ruleToc 抽取章节列表 */
    DirectedReadVO toc(Long sourceId, String tocUrl);

    /** 指定书源正文：contentUrl（章节正文 URL）→ 抓取 → ruleContent 抽取正文（record.content） */
    DirectedReadVO content(Long sourceId, String contentUrl);
}
