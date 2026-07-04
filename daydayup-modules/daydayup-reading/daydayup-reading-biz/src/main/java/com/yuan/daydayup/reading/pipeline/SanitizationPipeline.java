package com.yuan.daydayup.reading.pipeline;

import com.yuan.daydayup.reading.pipeline.config.ReadingSanitizationProperties;
import com.yuan.daydayup.reading.pipeline.model.SanitizationResult;
import com.yuan.daydayup.reading.repository.support.ContentNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 七段式内容净化 Pipeline（spec §7.2）。
 *
 * <p>输入标准化正文，顺序执行：normalize-structure → detect-noise → apply-transformations
 * → evaluate-quality → publish-decision（raw-input/archive-run 由调用方负责快照与归档）。
 * 第一期只落平台全局规则（§7.3.1），可编排/书源级/异常修复留待后续。</p>
 */
@Component
public class SanitizationPipeline {

    /** Pipeline 定义版本，规则/算法变更时递增，供重净化判断与回放 */
    public static final String PIPELINE_VERSION = "1.0";

    private final ReadingSanitizationProperties props;
    private final List<Pattern> tailPatterns;

    public SanitizationPipeline(ReadingSanitizationProperties props) {
        this.props = props;
        this.tailPatterns = props.getTailPatterns().stream().map(Pattern::compile).toList();
    }

    public SanitizationResult run(String normalizedInput) {
        SanitizationResult result = new SanitizationResult();

        // ① normalize-structure（幂等再规整，兜底非 normalized 输入）
        String text = ContentNormalizer.normalize(normalizedInput);
        List<String> lines = new ArrayList<>(List.of(text.isEmpty() ? new String[0] : text.split("\n")));
        int inputLines = lines.size();
        result.getTrace().add("normalize-structure: " + inputLines + " 段");

        // ② detect-noise + ③ apply-transformations：删广告/尾巴段
        List<String> kept = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (isNoise(line)) {
                result.getRemovedSegments().add(line);
            } else {
                kept.add(line);
            }
        }
        result.getTrace().add("detect-noise: 删除 " + result.getRemovedSegments().size() + " 段");

        // ③ 敏感词修复
        applySensitive(kept, result);
        // ③ 相邻重复段合并
        List<String> deduped = dedup(kept, result);
        result.getTrace().add("apply-transformations: 替换 " + result.getReplacedTerms().size()
                + " 处，去重后 " + deduped.size() + " 段");

        String sanitized = String.join("\n", deduped);
        result.setSanitizedContent(sanitized);

        // ④ evaluate-quality
        int score = evaluateQuality(sanitized, inputLines, result.getRemovedSegments().size());
        result.setQualityScore(score);
        result.getTrace().add("evaluate-quality: score=" + score + " 字数=" + sanitized.length());

        // ⑤ publish-decision
        String status = decide(sanitized, score);
        result.setRunStatus(status);
        result.getTrace().add("publish-decision: " + status);
        return result;
    }

    // ── 各段实现 ───────────────────────────────────────────────────

    private boolean isNoise(String line) {
        String t = line.strip();
        if (t.isEmpty()) {
            return false;
        }
        for (String kw : props.getAdKeywords()) {
            if (t.contains(kw)) {
                return true;
            }
        }
        for (Pattern p : tailPatterns) {
            if (p.matcher(t).find()) {
                return true;
            }
        }
        return false;
    }

    private void applySensitive(List<String> lines, SanitizationResult result) {
        Map<String, String> map = props.getSensitiveReplacements();
        if (map.isEmpty()) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (line.contains(e.getKey())) {
                    line = line.replace(e.getKey(), e.getValue());
                    result.getReplacedTerms().add(e.getKey() + "→" + e.getValue());
                }
            }
            lines.set(i, line);
        }
    }

    private List<String> dedup(List<String> lines, SanitizationResult result) {
        List<String> out = new ArrayList<>(lines.size());
        String prev = null;
        for (String line : lines) {
            if (line.equals(prev)) {
                result.getRemovedSegments().add("[重复] " + line);
                continue;
            }
            out.add(line);
            prev = line;
        }
        return out;
    }

    /** 质量评分：基础分 100，按噪音比例与长度不足扣分 */
    private int evaluateQuality(String sanitized, int inputLines, int removedCount) {
        int score = 100;
        int total = inputLines <= 0 ? 1 : inputLines;
        int noiseRatio = removedCount * 100 / total;
        score -= Math.min(40, noiseRatio);                       // 噪音占比最多扣 40
        if (sanitized.length() < props.getMinAcceptLength()) {
            score -= 35;                                         // 过短扣 35
        }
        if (sanitized.isBlank()) {
            score = 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String decide(String sanitized, int score) {
        if (sanitized.isBlank() || score < props.getDegradedScore()) {
            return "rejected";
        }
        return score >= props.getAcceptScore() ? "accepted" : "degraded";
    }
}
