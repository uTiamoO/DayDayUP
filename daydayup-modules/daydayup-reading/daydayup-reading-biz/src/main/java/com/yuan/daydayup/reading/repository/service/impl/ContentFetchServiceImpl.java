package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.api.vo.ContentSnapshotVO;
import com.yuan.daydayup.reading.api.vo.DirectedReadVO;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.ChapterContentSnapshotMapper;
import com.yuan.daydayup.reading.repository.mapper.ChapterSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.ContentFetchService;
import com.yuan.daydayup.reading.repository.support.ContentNormalizer;
import com.yuan.daydayup.reading.runtime.service.SourceReadingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 正文抓取与快照实现（spec §5.4，子片 1：raw + normalized）。
 *
 * <p>流程：定位 {@code (chapterId, sourceId)} 绑定取正文 URL → cache-first（命中未变更直接返回）
 * → 定向抓 raw → 标准化 normalized → 按 {@code (chapterId, sourceId)} 幂等 upsert 快照。
 * 空正文落 {@code empty} 状态并抛 {@link ErrorCode#READING_CONTENT_EMPTY}。</p>
 */
@Slf4j
@Service
public class ContentFetchServiceImpl implements ContentFetchService {

    private final ChapterSourceBindingMapper bindingMapper;
    private final ChapterContentSnapshotMapper snapshotMapper;
    private final SourceReadingService sourceReadingService;

    public ContentFetchServiceImpl(ChapterSourceBindingMapper bindingMapper,
                                   ChapterContentSnapshotMapper snapshotMapper,
                                   SourceReadingService sourceReadingService) {
        this.bindingMapper = bindingMapper;
        this.snapshotMapper = snapshotMapper;
        this.sourceReadingService = sourceReadingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentSnapshotVO fetchAndStore(Long chapterId, Long sourceId, boolean forceRefresh) {
        ChapterSourceBinding binding = bindingMapper.selectByChapterAndSource(chapterId, sourceId);
        if (binding == null || !StringUtils.hasText(binding.getSourceChapterUrl())) {
            throw new BizException(ErrorCode.READING_SOURCE_NOT_AVAILABLE,
                    "章节未绑定该来源或缺少正文 URL: chapterId=" + chapterId + " sourceId=" + sourceId);
        }

        ChapterContentSnapshot existing = snapshotMapper.selectByChapterAndSource(chapterId, sourceId);
        if (!forceRefresh && existing != null && !"empty".equals(existing.getContentStatus())
                && StringUtils.hasText(existing.getNormalizedContent())) {
            return toVo(existing, false);
        }

        DirectedReadVO read = sourceReadingService.content(sourceId, binding.getSourceChapterUrl());
        String raw = read.getRecord() == null ? null : read.getRecord().get("content");
        String normalized = ContentNormalizer.normalize(raw);

        ChapterContentSnapshot snapshot = existing != null ? existing : new ChapterContentSnapshot();
        snapshot.setChapterId(chapterId);
        snapshot.setSourceId(sourceId);
        snapshot.setRawContent(raw);
        snapshot.setNormalizedContent(normalized);
        snapshot.setContentHash(sha256(raw));
        snapshot.setFetchedAt(LocalDateTime.now());
        snapshot.setProcessedAt(LocalDateTime.now());
        snapshot.setContentStatus(normalized.isEmpty() ? "empty" : "normalized");
        persist(snapshot, existing != null);

        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.READING_CONTENT_EMPTY,
                    "正文为空: chapterId=" + chapterId + " sourceId=" + sourceId);
        }
        return toVo(snapshot, true);
    }

    private void persist(ChapterContentSnapshot snapshot, boolean update) {
        if (update) {
            snapshotMapper.updateById(snapshot);
        } else {
            snapshotMapper.insert(snapshot);
        }
    }

    private ContentSnapshotVO toVo(ChapterContentSnapshot s, boolean fresh) {
        ContentSnapshotVO vo = new ContentSnapshotVO();
        vo.setSnapshotId(s.getId());
        vo.setChapterId(s.getChapterId());
        vo.setSourceId(s.getSourceId());
        vo.setContentStatus(s.getContentStatus());
        vo.setContent(s.getNormalizedContent());
        vo.setLength(s.getNormalizedContent() == null ? 0 : s.getNormalizedContent().length());
        vo.setFreshlyFetched(fresh);
        return vo;
    }

    private static String sha256(String s) {
        if (s == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
