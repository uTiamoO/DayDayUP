package com.yuan.daydayup.reading.service.impl;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ReadingContentVO;
import com.yuan.daydayup.reading.pipeline.entity.ContentSanitizationRun;
import com.yuan.daydayup.reading.pipeline.mapper.ContentSanitizationRunMapper;
import com.yuan.daydayup.reading.pipeline.service.ContentSanitizeService;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import com.yuan.daydayup.reading.service.ReadingContentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 统一阅读正文编排实现。
 */
@Service
public class ReadingContentServiceImpl implements ReadingContentService {

    private static final String VERSION_RAW = "raw";
    private static final String VERSION_NORMALIZED = "normalized";
    private static final String VERSION_SANITIZED = "sanitized";
    private static final String VERSION_LATEST = "latest";
    private static final String POLICY_CACHE_FIRST = "cache-first";
    private static final String POLICY_FORCE_REFRESH = "force-refresh";
    private static final Set<String> SUPPORTED_VERSIONS = Set.of(VERSION_RAW, VERSION_NORMALIZED, VERSION_SANITIZED, VERSION_LATEST);
    private static final Set<String> SUPPORTED_POLICIES = Set.of(POLICY_CACHE_FIRST, POLICY_FORCE_REFRESH);

    private final ChapterMapper chapterMapper;
    private final ChapterSourceBindingMapper chapterSourceBindingMapper;
    private final ChapterContentSnapshotMapper snapshotMapper;
    private final ContentSanitizationRunMapper runMapper;
    private final ContentFetchService contentFetchService;
    private final ContentSanitizeService contentSanitizeService;

    public ReadingContentServiceImpl(ChapterMapper chapterMapper,
                                     ChapterSourceBindingMapper chapterSourceBindingMapper,
                                     ChapterContentSnapshotMapper snapshotMapper,
                                     ContentSanitizationRunMapper runMapper,
                                     ContentFetchService contentFetchService,
                                     ContentSanitizeService contentSanitizeService) {
        this.chapterMapper = chapterMapper;
        this.chapterSourceBindingMapper = chapterSourceBindingMapper;
        this.snapshotMapper = snapshotMapper;
        this.runMapper = runMapper;
        this.contentFetchService = contentFetchService;
        this.contentSanitizeService = contentSanitizeService;
    }

    @Override
    public ReadingContentVO content(Long chapterId, Long sourceId, String contentVersion, String fetchPolicy) {
        validateIds(chapterId, sourceId);
        String version = normalizeVersion(contentVersion);
        String policy = normalizePolicy(fetchPolicy);
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BizException(ErrorCode.READING_CHAPTER_NOT_FOUND, "章节不存在: " + chapterId);
        }
        ChapterSourceBinding binding = chapterSourceBindingMapper.selectByChapterAndSource(chapterId, sourceId);
        if (binding == null) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                    "章节未绑定该来源: chapterId=" + chapterId + " sourceId=" + sourceId);
        }

        boolean forceRefresh = POLICY_FORCE_REFRESH.equals(policy);
        ChapterContentSnapshot snapshot = snapshotMapper.selectByChapterAndSource(chapterId, sourceId);
        boolean fresh = false;
        if (forceRefresh || shouldFetch(snapshot, version)) {
            contentFetchService.fetchAndStore(chapterId, sourceId, forceRefresh);
            fresh = true;
            snapshot = snapshotMapper.selectByChapterAndSource(chapterId, sourceId);
        }
        if (snapshot == null) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "正文快照不存在: chapterId=" + chapterId + " sourceId=" + sourceId);
        }
        if (requiresSanitized(version) && !StringUtils.hasText(snapshot.getSanitizedContent())) {
            contentSanitizeService.sanitize(chapterId, sourceId);
            fresh = true;
            snapshot = snapshotMapper.selectByChapterAndSource(chapterId, sourceId);
        }
        if (snapshot == null) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "正文快照不存在: chapterId=" + chapterId + " sourceId=" + sourceId);
        }
        return toVo(snapshot, version, fresh);
    }

    private boolean shouldFetch(ChapterContentSnapshot snapshot, String version) {
        if (snapshot == null) {
            return true;
        }
        return switch (version) {
            case VERSION_RAW -> !StringUtils.hasText(snapshot.getRawContent());
            case VERSION_NORMALIZED -> !StringUtils.hasText(snapshot.getNormalizedContent());
            case VERSION_SANITIZED, VERSION_LATEST -> !StringUtils.hasText(snapshot.getSanitizedContent())
                    && !StringUtils.hasText(snapshot.getRawContent());
            default -> false;
        };
    }

    private ReadingContentVO toVo(ChapterContentSnapshot snapshot, String version, boolean fresh) {
        String resolvedVersion = VERSION_LATEST.equals(version) ? VERSION_SANITIZED : version;
        String content = switch (resolvedVersion) {
            case VERSION_RAW -> snapshot.getRawContent();
            case VERSION_NORMALIZED -> snapshot.getNormalizedContent();
            case VERSION_SANITIZED -> snapshot.getSanitizedContent();
            default -> throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的正文版本: " + version);
        };
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "正文为空: chapterId=" + snapshot.getChapterId() + " sourceId=" + snapshot.getSourceId());
        }
        ReadingContentVO vo = new ReadingContentVO();
        vo.setChapterId(snapshot.getChapterId());
        vo.setSourceId(snapshot.getSourceId());
        vo.setContentSnapshotId(snapshot.getId());
        vo.setContentVersion(resolvedVersion);
        vo.setContent(content);
        vo.setContentStatus(snapshot.getContentStatus());
        vo.setFreshlyFetched(fresh);
        ContentSanitizationRun run = runMapper.selectLatestBySnapshotId(snapshot.getId());
        if (run != null) {
            vo.setQualityScore(run.getQualityScore());
            vo.setSanitizationRunId(run.getId());
        }
        return vo;
    }

    private boolean requiresSanitized(String version) {
        return VERSION_SANITIZED.equals(version) || VERSION_LATEST.equals(version);
    }

    private void validateIds(Long chapterId, Long sourceId) {
        if (chapterId == null || sourceId == null) {
            throw new BizException(ErrorCode.READING_INVALID_ARGUMENT, "chapterId 和 sourceId 不能为空");
        }
    }

    private String normalizeVersion(String version) {
        String normalized = StringUtils.hasText(version) ? version.trim() : VERSION_SANITIZED;
        if (!SUPPORTED_VERSIONS.contains(normalized)) {
            throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的正文版本: " + normalized);
        }
        return normalized;
    }

    private String normalizePolicy(String policy) {
        String normalized = StringUtils.hasText(policy) ? policy.trim() : POLICY_CACHE_FIRST;
        if (!SUPPORTED_POLICIES.contains(normalized)) {
            throw new BizException(ErrorCode.READING_UNSUPPORTED_MODE, "不支持的抓取策略: " + normalized);
        }
        return normalized;
    }
}
