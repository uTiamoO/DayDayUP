package com.yuan.daydayup.reading.pipeline.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 净化 Pipeline 平台全局规则配置（{@code reading.sanitization.*}，spec §7.3.1）。
 *
 * <p>第一期只做平台全局层；书源级 / 内容异常修复规则（§7.3.2/§7.3.3）留待后续切片。
 * 默认给出常见广告/尾巴/敏感词，生产可经 Nacos 覆盖。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "reading.sanitization")
public class ReadingSanitizationProperties {

    /** 广告 / 引流关键词：命中的整段删除 */
    private List<String> adKeywords = List.of(
            "本站", "笔趣", "请记住", "最新章节", "手机阅读", "求收藏", "求推荐票",
            "加群", "微信公众号", "关注公众号", "txt下载", "免费阅读", "www.", "http://", "https://", ".com", ".net");

    /** 分页尾巴 / 模板残留正则：命中的整段删除 */
    private List<String> tailPatterns = List.of(
            "^\\(本章未完.*", ".*未完待续.*", "^第?\\s*\\d+\\s*/\\s*\\d+\\s*页$", ".*点击下一页.*");

    /** 敏感词替换（key→value），value 一般为脱敏符或修复词 */
    private Map<String, String> sensitiveReplacements = new LinkedHashMap<>();

    /** 质量：判定合格的最小字数 */
    private int minAcceptLength = 200;

    /** 质量：accepted 的最低分 */
    private int acceptScore = 70;

    /** 质量：degraded 的最低分（低于此为 rejected） */
    private int degradedScore = 40;
}
