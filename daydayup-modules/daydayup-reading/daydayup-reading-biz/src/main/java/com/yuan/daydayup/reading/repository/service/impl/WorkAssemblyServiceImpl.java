package com.yuan.daydayup.reading.repository.service.impl;

import com.yuan.daydayup.reading.api.vo.WorkBriefVO;
import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.SearchCandidate;
import com.yuan.daydayup.reading.repository.service.WorkAssemblyService;
import com.yuan.daydayup.reading.repository.support.MatchKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作品装配实现（spec §5.2）。
 *
 * <p>单个候选流程：{@code (sourceId, sourceBookUrl)} 已绑定 → 更新绑定；否则按 matchKey
 * find-or-create Work，新建绑定（首个绑定即主来源），维护 {@code sourceCount / aggregationStatus}。</p>
 *
 * <p>保守归并（宁拆不错并）：仅 matchKey 完全一致才归并；跨源命中同 Work 时状态升为
 * {@code merged}，具体真伪由运营 API 复核（后续切片）。</p>
 */
@Slf4j
@Service
public class WorkAssemblyServiceImpl implements WorkAssemblyService {

    private final WorkMapper workMapper;
    private final WorkSourceBindingMapper bindingMapper;

    public WorkAssemblyServiceImpl(WorkMapper workMapper, WorkSourceBindingMapper bindingMapper) {
        this.workMapper = workMapper;
        this.bindingMapper = bindingMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkDiscoveryResultVO materialize(Long sourceId, List<SearchCandidate> candidates) {
        WorkDiscoveryResultVO result = new WorkDiscoveryResultVO();
        if (candidates == null) {
            return result;
        }
        result.setTotal(candidates.size());
        Map<Long, WorkBriefVO> touched = new LinkedHashMap<>();

        for (SearchCandidate c : candidates) {
            if (!StringUtils.hasText(c.title()) || !StringUtils.hasText(c.sourceBookUrl())) {
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            Work work = materializeOne(sourceId, c, result);
            if (work != null) {
                touched.put(work.getId(), brief(work));
            }
        }
        result.getWorks().addAll(touched.values());
        log.info("[work-assembly] sourceId={} total={} newWork={} matchWork={} newBind={} updBind={} skip={}",
                sourceId, result.getTotal(), result.getWorksCreated(), result.getWorksMatched(),
                result.getBindingsCreated(), result.getBindingsUpdated(), result.getSkipped());
        return result;
    }

    private Work materializeOne(Long sourceId, SearchCandidate c, WorkDiscoveryResultVO result) {
        WorkSourceBinding existingBinding = bindingMapper.selectBySourceBook(sourceId, c.sourceBookUrl());

        if (existingBinding != null) {
            applyBindingFields(existingBinding, c);
            bindingMapper.updateById(existingBinding);
            result.setBindingsUpdated(result.getBindingsUpdated() + 1);
            return workMapper.selectById(existingBinding.getWorkId());
        }

        // find-or-create Work
        String matchKey = MatchKeys.of(c.title(), c.author());
        Work work = workMapper.selectByMatchKey(matchKey);
        boolean firstBindingOfWork;
        if (work == null) {
            work = newWork(c, matchKey);
            workMapper.insert(work);
            result.setWorksCreated(result.getWorksCreated() + 1);
            firstBindingOfWork = true;
        } else {
            result.setWorksMatched(result.getWorksMatched() + 1);
            firstBindingOfWork = false;
        }

        WorkSourceBinding binding = new WorkSourceBinding();
        binding.setWorkId(work.getId());
        binding.setSourceId(sourceId);
        binding.setIsPrimarySource(firstBindingOfWork ? 1 : 0);
        binding.setBindingStatus("active");
        binding.setMatchConfidence(firstBindingOfWork ? 100 : 80);
        applyBindingFields(binding, c);
        bindingMapper.insert(binding);
        result.setBindingsCreated(result.getBindingsCreated() + 1);

        // 维护来源计数与归并状态
        int sourceCount = work.getSourceCount() == null ? 0 : work.getSourceCount();
        sourceCount++;
        work.setSourceCount(sourceCount);
        if (!"suspect".equals(work.getAggregationStatus())) {
            work.setAggregationStatus(sourceCount >= 2 ? "merged" : "single_source");
        }
        workMapper.updateById(work);
        return work;
    }

    private Work newWork(SearchCandidate c, String matchKey) {
        Work work = new Work();
        work.setTitle(c.title());
        work.setAuthorName(c.author());
        work.setCategoryName(c.category());
        work.setCoverUrl(c.coverUrl());
        work.setDescription(c.description());
        work.setLatestChapterTitle(c.latestChapter());
        work.setCompletionStatus("unknown");
        work.setAggregationStatus("single_source");
        work.setMatchKey(matchKey);
        work.setSourceCount(0);
        return work;
    }

    private void applyBindingFields(WorkSourceBinding binding, SearchCandidate c) {
        binding.setSourceBookUrl(c.sourceBookUrl());
        binding.setSourceBookName(c.title());
        binding.setSourceAuthorName(c.author());
    }

    private WorkBriefVO brief(Work work) {
        WorkBriefVO vo = new WorkBriefVO();
        vo.setWorkId(work.getId());
        vo.setTitle(work.getTitle());
        vo.setAuthor(work.getAuthorName());
        vo.setAggregationStatus(work.getAggregationStatus());
        vo.setSourceCount(work.getSourceCount());
        return vo;
    }
}
