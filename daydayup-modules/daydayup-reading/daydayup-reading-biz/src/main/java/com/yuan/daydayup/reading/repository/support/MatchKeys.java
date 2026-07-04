package com.yuan.daydayup.reading.repository.support;

import java.text.Normalizer;
import java.util.Map;

/**
 * 作品保守归并键与候选归一化助手。
 *
 * <p>归并键 = 规整(标题) + 分隔符 + 规整(作者)。规整策略保守：仅吸收
 * 大小写 / 全半角 / 空白 / 标点差异，不做同义词、别名、模糊相似度合并
 * （spec §5.2：作品归并必须保守，宁可拆分不可错并）。</p>
 */
public final class MatchKeys {

    /** 归并键内部分隔符（不可见控制字符 SOH，避免与标题/作者内容冲突） */
    private static final String SEP = "";

    private MatchKeys() {
    }

    /** 生成保守归并键 */
    public static String of(String title, String author) {
        return normalize(title) + SEP + normalize(author);
    }

    /** 规整：NFKC（全角→半角）+ 去空白 + 去 Unicode 标点/符号 + 小写 */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        n = n.replaceAll("[\\s\\p{P}\\p{S}]+", "");
        return n.toLowerCase();
    }

    /** 从抽取记录取值（多候选字段名，取首个非空） */
    public static String pick(Map<String, String> record, String... fields) {
        for (String f : fields) {
            String v = record.get(f);
            if (v != null && !v.isBlank()) {
                return v.strip();
            }
        }
        return null;
    }
}
