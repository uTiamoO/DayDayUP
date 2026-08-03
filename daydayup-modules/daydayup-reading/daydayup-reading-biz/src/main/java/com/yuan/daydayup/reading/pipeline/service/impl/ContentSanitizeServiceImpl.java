package com.yuan.daydayup.reading.pipeline.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.SanitizeResultVO;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import com.yuan.daydayup.reading.pipeline.SanitizationPipeline;
import com.yuan.daydayup.reading.pipeline.entity.ContentSanitizationRun;
import com.yuan.daydayup.reading.pipeline.mapper.ContentSanitizationRunMapper;
import com.yuan.daydayup.reading.pipeline.model.SanitizationResult;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 正文净化实现（spec §7）。
 *
 * <p>raw-input（快照已存）→ 跑七段 Pipeline → publish-decision：accepted/degraded 写 sanitized 层并置
 * {@code content_status=sanitized}；rejected 不覆盖 sanitized（保留既有可用正文）。archive-run 落
 * {@link ContentSanitizationRun}。净化失败不等于抓取失败——异常单独归档 failed，不动快照。</p>
 */
@Slf4j
@Service
public class ContentSanitizeServiceImpl implements ContentSanitizeService {

    private final ChapterContentSnapshotMapper snapshotMapper;
    private final ContentSanitizationRunMapper runMapper;
    private final SanitizationPipeline pipeline;
    private final ObjectMapper objectMapper;
    private final ReadingMetrics metrics;

    public ContentSanitizeServiceImpl(ChapterContentSnapshotMapper snapshotMapper,
                                      ContentSanitizationRunMapper runMapper,
                                      SanitizationPipeline pipeline,
                                      ObjectMapper objectMapper,
                                      ReadingMetrics metrics) {
        this.snapshotMapper = snapshotMapper;
        this.runMapper = runMapper;
        this.pipeline = pipeline;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SanitizeResultVO sanitize(Long chapterId, Long sourceId) {
        ChapterContentSnapshot snapshot = snapshotMapper.selectByChapterAndSource(chapterId, sourceId);
        if (snapshot == null) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "无正文快照，先抓取: chapterId=" + chapterId + " sourceId=" + sourceId);
        }
        String normalized = StringUtils.hasText(snapshot.getNormalizedContent())
                ? snapshot.getNormalizedContent() : snapshot.getRawContent();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY, "快照正文为空，无法净化");
        }

        SanitizationResult result;
        try {
            result = pipeline.run(normalized);
        } catch (Exception e) {
            archive(snapshot.getId(), null, "failed", List.of("pipeline 异常: " + e.getMessage()));
            metrics.recordSanitize("failed", null);
            throw new BizException(ErrorCode.READING_CONTENT_SANITIZATION_FAILED, "净化失败: " + e.getMessage());
        }
        metrics.recordSanitize(result.getRunStatus(), result.getQualityScore());

        // publish-decision：accepted/degraded 发布 sanitized；rejected 不覆盖
        boolean published = !"rejected".equals(result.getRunStatus());
        if (published) {
            snapshot.setSanitizedContent(result.getSanitizedContent());
            snapshot.setContentStatus("sanitized");
            snapshot.setSanitizationPipelineVersion(SanitizationPipeline.PIPELINE_VERSION);
            snapshot.setProcessedAt(LocalDateTime.now());
            snapshotMapper.updateById(snapshot);
        }

        ContentSanitizationRun run = archive(snapshot.getId(), result, result.getRunStatus(), result.getTrace());
        return toVo(chapterId, sourceId, run, result);
    }

    private ContentSanitizationRun archive(Long snapshotId, SanitizationResult result,
                                           String status, List<String> trace) {
        ContentSanitizationRun run = new ContentSanitizationRun();
        run.setContentSnapshotId(snapshotId);
        run.setPipelineDefinitionVersion(SanitizationPipeline.PIPELINE_VERSION);
        run.setPipelineExecutionTrace(toJson(trace));
        run.setRunStatus(status);
        run.setRunAt(LocalDateTime.now());
        if (result != null) {
            run.setRemovedSegments(toJson(result.getRemovedSegments()));
            run.setReplacedTerms(toJson(result.getReplacedTerms()));
            run.setQualityScore(result.getQualityScore());
        }
        runMapper.insert(run);
        return run;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private SanitizeResultVO toVo(Long chapterId, Long sourceId, ContentSanitizationRun run,
                                  SanitizationResult result) {
        SanitizeResultVO vo = new SanitizeResultVO();
        vo.setChapterId(chapterId);
        vo.setSourceId(sourceId);
        vo.setSanitizationRunId(run.getId());
        vo.setPipelineVersion(SanitizationPipeline.PIPELINE_VERSION);
        vo.setRunStatus(result.getRunStatus());
        vo.setQualityScore(result.getQualityScore());
        vo.setRemovedCount(result.getRemovedSegments().size());
        vo.setReplacedCount(result.getReplacedTerms().size());
        vo.setSanitizedContent(result.getSanitizedContent());
        vo.setTrace(result.getTrace());
        return vo;
    }
}
