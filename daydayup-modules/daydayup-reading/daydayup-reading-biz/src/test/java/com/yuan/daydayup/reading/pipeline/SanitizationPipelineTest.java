package com.yuan.daydayup.reading.pipeline;

import com.yuan.daydayup.reading.pipeline.config.ReadingSanitizationProperties;
import com.yuan.daydayup.reading.pipeline.model.SanitizationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SanitizationPipeline} 单测：广告删除、尾巴删除、敏感词替换、相邻去重、质量/发布判定。
 */
class SanitizationPipelineTest {

    private SanitizationPipeline pipeline(ReadingSanitizationProperties props) {
        return new SanitizationPipeline(props);
    }

    /** 生成一段足够长的干净正文，保证 accepted 阈值 */
    private String longBody() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("这是第").append(i).append("段正文内容，讲述了主角的修炼历程与心境变化，文字通顺自然。\n");
        }
        return sb.toString();
    }

    @Test
    void removesAdAndTailLines() {
        ReadingSanitizationProperties props = new ReadingSanitizationProperties();
        String input = longBody()
                + "请记住本站网址 www.piaotian.com，最新章节抢先看\n"
                + "(本章未完，请点击下一页继续阅读)";
        SanitizationResult r = pipeline(props).run(input);

        assertFalse(r.getSanitizedContent().contains("piaotian"));
        assertFalse(r.getSanitizedContent().contains("本章未完"));
        assertTrue(r.getRemovedSegments().size() >= 2);
        assertEquals("accepted", r.getRunStatus());
    }

    @Test
    void replacesSensitiveTerms() {
        ReadingSanitizationProperties props = new ReadingSanitizationProperties();
        props.setSensitiveReplacements(Map.of("坏蛋", "**"));
        SanitizationResult r = pipeline(props).run(longBody() + "他是个坏蛋。");

        assertTrue(r.getSanitizedContent().contains("**"));
        assertFalse(r.getSanitizedContent().contains("坏蛋"));
        assertEquals(1, r.getReplacedTerms().size());
    }

    @Test
    void dedupsAdjacentDuplicateParagraphs() {
        ReadingSanitizationProperties props = new ReadingSanitizationProperties();
        String dup = "完全一样的重复段落内容。";
        SanitizationResult r = pipeline(props).run(longBody() + dup + "\n" + dup);

        long occurrences = r.getSanitizedContent().lines().filter(l -> l.equals(dup)).count();
        assertEquals(1, occurrences);
    }

    @Test
    void shortContentDegradedOrRejected() {
        ReadingSanitizationProperties props = new ReadingSanitizationProperties();
        SanitizationResult r = pipeline(props).run("太短了。");
        assertFalse("accepted".equals(r.getRunStatus()));
    }

    @Test
    void emptyInputRejected() {
        SanitizationResult r = pipeline(new ReadingSanitizationProperties()).run("   ");
        assertEquals("rejected", r.getRunStatus());
        assertEquals(0, r.getQualityScore());
    }

    @Test
    void traceCoversAllStages() {
        SanitizationResult r = pipeline(new ReadingSanitizationProperties()).run(longBody());
        String trace = String.join("|", r.getTrace());
        assertTrue(trace.contains("normalize-structure"));
        assertTrue(trace.contains("detect-noise"));
        assertTrue(trace.contains("apply-transformations"));
        assertTrue(trace.contains("evaluate-quality"));
        assertTrue(trace.contains("publish-decision"));
    }
}
