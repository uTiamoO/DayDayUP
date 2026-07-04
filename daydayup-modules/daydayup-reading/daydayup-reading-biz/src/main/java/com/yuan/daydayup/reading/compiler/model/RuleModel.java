package com.yuan.daydayup.reading.compiler.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 归一化规则模型（rulemodel §4 SourceModel）。
 *
 * <p>Legado 书源编译产物：忠实复现 Legado 抽取语义的内存对象，可序列化为 JSON 持久化供回放。
 * 第一期为 Legado 专用（origin=legado），非跨格式 DSL。</p>
 */
@Data
public class RuleModel {

    private String dslVersion = "rulemodel-1";

    private Identity identity = new Identity();

    /** 书源级 HTTP 出站配置（rulemodel §4 http） */
    private Http http = new Http();

    /** action 名 -> 规则集合（search/detail/toc/content/explore） */
    private Map<String, ActionRule> actions = new LinkedHashMap<>();

    private Health health = new Health();

    public void putAction(String name, ActionRule rule) {
        actions.put(name, rule);
    }

    /** 书源身份（rulemodel §4 identity） */
    @Data
    public static class Identity {
        private String key;          // bookSourceUrl（去 ## 注释后）
        private String name;
        private String originType = "legado";
        private String baseUrl;
        private String bookType = "text";
        private String group;
        private boolean enabled = true;
    }

    /** 编译体检结果（rulemodel §4/§5.2 health） */
    @Data
    public static class Health {
        private CompileGrade grade = CompileGrade.FULL;
        private List<String> warnings = new ArrayList<>();
        private List<String> scriptDeps = new ArrayList<>();
        private List<String> webviewDeps = new ArrayList<>();
        private List<String> unknownBridges = new ArrayList<>();
    }

    /** 书源级 HTTP 出站配置（Legado header / concurrentRate） */
    @Data
    public static class Http {
        /** 书源级静态请求头（Legado header 字段，宽松 JSON 解析产物） */
        private Map<String, String> headers = new LinkedHashMap<>();

        /** Legado 并发率原文："N/M"=M 毫秒内最多 N 次；纯数字 "N"=每次间隔 N 毫秒 */
        private String concurrentRate;
    }
}
