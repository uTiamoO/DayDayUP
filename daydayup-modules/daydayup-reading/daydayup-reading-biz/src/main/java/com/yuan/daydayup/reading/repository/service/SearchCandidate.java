package com.yuan.daydayup.reading.repository.service;

/**
 * 搜索结果归一化候选（来源无关的作品候选视图）。
 *
 * <p>由 {@code Map<String,String>}（RuleExecutor 抽取产物，字段名为 Legado ruleSearch 字段）
 * 映射而来，喂给 {@link WorkAssemblyService} 做作品入库与保守归并。</p>
 *
 * @param title             书名（展示用）
 * @param author            作者
 * @param category          分类（kind）
 * @param coverUrl          封面
 * @param description       简介（intro）
 * @param latestChapter     最新章节标题（lastChapter）
 * @param sourceBookUrl     来源书籍详情 URL（bookUrl；该源内幂等键）
 */
public record SearchCandidate(
        String title,
        String author,
        String category,
        String coverUrl,
        String description,
        String latestChapter,
        String sourceBookUrl) {
}
