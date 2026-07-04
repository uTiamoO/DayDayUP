package com.yuan.daydayup.reading.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.SanitizeResultVO;
import com.yuan.daydayup.reading.pipeline.SanitizationPipeline;
import com.yuan.daydayup.reading.pipeline.config.ReadingSanitizationProperties;
import com.yuan.daydayup.reading.pipeline.entity.ContentSanitizationRun;
import com.yuan.daydayup.reading.pipeline.mapper.ContentSanitizationRunMapper;
import com.yuan.daydayup.reading.pipeline.service.impl.ContentSanitizeServiceImpl;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ContentSanitizeService} 单测：accepted 写 sanitized+归档、rejected 不覆盖、缺快照 60301。
 */
class ContentSanitizeServiceTest {

    private final List<ContentSanitizationRun> runs = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(9000);

    private ChapterContentSnapshotMapper snapshotMapper;
    private ContentSanitizationRunMapper runMapper;
    private ContentSanitizeService service;

    @BeforeEach
    void setUp() {
        runs.clear();
        snapshotMapper = mock(ChapterContentSnapshotMapper.class);
        runMapper = mock(ContentSanitizationRunMapper.class);
        when(snapshotMapper.updateById(any(ChapterContentSnapshot.class))).thenReturn(1);
        doAnswer(inv -> {
            ContentSanitizationRun run = inv.getArgument(0);
            run.setId(seq.incrementAndGet());
            runs.add(run);
            return 1;
        }).when(runMapper).insert(any(ContentSanitizationRun.class));

        SanitizationPipeline pipeline = new SanitizationPipeline(new ReadingSanitizationProperties());
        service = new ContentSanitizeServiceImpl(snapshotMapper, runMapper, pipeline, new ObjectMapper());
    }

    private ChapterContentSnapshot snapshot(String normalized) {
        ChapterContentSnapshot s = new ChapterContentSnapshot();
        s.setId(500L);
        s.setChapterId(1L);
        s.setSourceId(10L);
        s.setNormalizedContent(normalized);
        s.setContentStatus("normalized");
        return s;
    }

    private String longBody() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("这是第").append(i).append("段正文，情节推进自然，文字通顺完整。\n");
        }
        return sb.toString();
    }

    @Test
    void acceptedWritesSanitizedAndArchives() {
        ChapterContentSnapshot s = snapshot(longBody() + "请记住本站 www.ad.com");
        when(snapshotMapper.selectByChapterAndSource(1L, 10L)).thenReturn(s);

        SanitizeResultVO vo = service.sanitize(1L, 10L);

        assertEquals("accepted", vo.getRunStatus());
        assertEquals("sanitized", s.getContentStatus());
        assertEquals(SanitizationPipeline.PIPELINE_VERSION, s.getSanitizationPipelineVersion());
        // sanitized 层写入且不含广告
        org.junit.jupiter.api.Assertions.assertFalse(s.getSanitizedContent().contains("www.ad.com"));
        verify(snapshotMapper).updateById(any(ChapterContentSnapshot.class));
        assertEquals(1, runs.size());
        assertEquals("accepted", runs.get(0).getRunStatus());
    }

    @Test
    void rejectedDoesNotOverwriteButArchives() {
        // 全广告/尾巴行 → 净化后为空 → rejected
        ChapterContentSnapshot s = snapshot("请记住本站\nwww.piaotian.com\n最新章节抢先看");
        when(snapshotMapper.selectByChapterAndSource(1L, 10L)).thenReturn(s);

        SanitizeResultVO vo = service.sanitize(1L, 10L);

        assertEquals("rejected", vo.getRunStatus());
        // 未发布：sanitized 不写、状态不变
        assertNull(s.getSanitizedContent());
        assertEquals("normalized", s.getContentStatus());
        verify(snapshotMapper, never()).updateById(any(ChapterContentSnapshot.class));
        assertEquals(1, runs.size());
        assertEquals("rejected", runs.get(0).getRunStatus());
    }

    @Test
    void missingSnapshotThrows() {
        when(snapshotMapper.selectByChapterAndSource(9L, 10L)).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> service.sanitize(9L, 10L));
        assertEquals(60301, e.getCode());
    }
}
