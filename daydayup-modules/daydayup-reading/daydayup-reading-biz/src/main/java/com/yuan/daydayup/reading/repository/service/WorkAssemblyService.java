package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;

import java.util.List;

/**
 * 作品装配服务（content-repository，spec §5.2）：把某来源的搜索候选入库为统一作品 + 来源绑定，
 * 按保守策略归并。
 */
public interface WorkAssemblyService {

    /**
     * 把一批来源候选物化为 Work + WorkSourceBinding。
     *
     * @param sourceId   来源书源 id
     * @param candidates 归一化候选（来源无关视图）
     */
    WorkDiscoveryResultVO materialize(Long sourceId, List<SearchCandidate> candidates);
}
