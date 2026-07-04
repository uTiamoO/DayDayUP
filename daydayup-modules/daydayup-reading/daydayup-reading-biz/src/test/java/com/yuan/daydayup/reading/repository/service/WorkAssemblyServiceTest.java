package com.yuan.daydayup.reading.repository.service;

import com.yuan.daydayup.reading.api.vo.WorkDiscoveryResultVO;
import com.yuan.daydayup.reading.repository.entity.Work;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import com.yuan.daydayup.reading.repository.mapper.WorkMapper;
import com.yuan.daydayup.reading.repository.mapper.WorkSourceBindingMapper;
import com.yuan.daydayup.reading.repository.service.impl.WorkAssemblyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link WorkAssemblyService} 单测：用内存 map 假实现两张表，覆盖
 * find-or-create、保守归并（跨源→merged）、幂等重绑（同源同 URL 只更新）。
 */
class WorkAssemblyServiceTest {

    private final List<Work> works = new ArrayList<>();
    private final List<WorkSourceBinding> bindings = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(1000);

    private WorkAssemblyService service;

    @BeforeEach
    void setUp() {
        works.clear();
        bindings.clear();

        WorkMapper workMapper = mock(WorkMapper.class);
        WorkSourceBindingMapper bindingMapper = mock(WorkSourceBindingMapper.class);

        doAnswer(assignIdAndAdd(works)).when(workMapper).insert(any(Work.class));
        when(workMapper.selectById(anyLong())).thenAnswer(inv ->
                works.stream().filter(w -> w.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(workMapper.selectByMatchKey(anyString())).thenAnswer(inv ->
                works.stream().filter(w -> inv.getArgument(0).equals(w.getMatchKey()))
                        .min((a, b) -> Long.compare(a.getId(), b.getId())).orElse(null));
        when(workMapper.updateById(any(Work.class))).thenReturn(1);

        doAnswer(assignIdAndAdd(bindings)).when(bindingMapper).insert(any(WorkSourceBinding.class));
        when(bindingMapper.selectBySourceBook(anyLong(), anyString())).thenAnswer(inv ->
                bindings.stream()
                        .filter(b -> b.getSourceId().equals(inv.<Long>getArgument(0))
                                && b.getSourceBookUrl().equals(inv.getArgument(1)))
                        .findFirst().orElse(null));
        when(bindingMapper.updateById(any(WorkSourceBinding.class))).thenReturn(1);

        service = new WorkAssemblyServiceImpl(workMapper, bindingMapper);
    }

    private <T> org.mockito.stubbing.Answer<Integer> assignIdAndAdd(List<T> store) {
        return (InvocationOnMock inv) -> {
            Object entity = inv.getArgument(0);
            if (entity instanceof Work w) {
                w.setId(seq.incrementAndGet());
            } else if (entity instanceof WorkSourceBinding b) {
                b.setId(seq.incrementAndGet());
            }
            @SuppressWarnings("unchecked")
            T t = (T) entity;
            store.add(t);
            return 1;
        };
    }

    private SearchCandidate candidate(String title, String author, String url) {
        return new SearchCandidate(title, author, "玄幻", "cover", "简介", "第100章", url);
    }

    @Test
    void createsNewWorkAndPrimaryBinding() {
        WorkDiscoveryResultVO r = service.materialize(1L,
                List.of(candidate("凡人修仙传", "忘语", "/b/1")));

        assertEquals(1, r.getWorksCreated());
        assertEquals(1, r.getBindingsCreated());
        assertEquals(1, works.size());
        assertEquals("single_source", works.get(0).getAggregationStatus());
        assertEquals(1, works.get(0).getSourceCount());
        assertEquals(1, bindings.get(0).getIsPrimarySource());
    }

    @Test
    void twoSourcesSameBookMergeConservatively() {
        service.materialize(1L, List.of(candidate("凡人修仙传", "忘语", "/s1/1")));
        // 另一来源、标题仅空白差异 → 同 matchKey → 归并
        WorkDiscoveryResultVO r2 = service.materialize(2L, List.of(candidate(" 凡人修仙传 ", "忘语", "/s2/9")));

        assertEquals(0, r2.getWorksCreated());
        assertEquals(1, r2.getWorksMatched());
        assertEquals(1, works.size());
        assertEquals("merged", works.get(0).getAggregationStatus());
        assertEquals(2, works.get(0).getSourceCount());
        // 主来源仍是第一个绑定
        assertEquals(1, bindings.get(0).getIsPrimarySource());
        assertEquals(0, bindings.get(1).getIsPrimarySource());
    }

    @Test
    void differentAuthorNotMerged() {
        service.materialize(1L, List.of(candidate("凡人修仙传", "忘语", "/s1/1")));
        service.materialize(2L, List.of(candidate("凡人修仙传", "别人", "/s2/2")));

        assertEquals(2, works.size());
    }

    @Test
    void reimportSameSourceBookIsIdempotent() {
        service.materialize(1L, List.of(candidate("凡人修仙传", "忘语", "/b/1")));
        WorkDiscoveryResultVO r2 = service.materialize(1L,
                List.of(candidate("凡人修仙传（校对）", "忘语", "/b/1")));   // 同源同 URL

        assertEquals(0, r2.getWorksCreated());
        assertEquals(0, r2.getBindingsCreated());
        assertEquals(1, r2.getBindingsUpdated());
        assertEquals(1, works.size());
        assertEquals(1, bindings.size());
        assertEquals(1, works.get(0).getSourceCount());   // 不重复计数
    }

    @Test
    void skipsBlankTitleOrUrl() {
        WorkDiscoveryResultVO r = service.materialize(1L, List.of(
                candidate("", "作者", "/b/1"),
                candidate("有名书", "作者", "  "),
                candidate("正常书", "作者", "/b/2")));

        assertEquals(2, r.getSkipped());
        assertEquals(1, r.getWorksCreated());
    }

    @Test
    void resultCarriesWorkBrief() {
        WorkDiscoveryResultVO r = service.materialize(1L,
                List.of(candidate("仙逆", "耳根", "/b/1")));
        assertEquals(1, r.getWorks().size());
        assertNotNull(r.getWorks().get(0).getWorkId());
        assertEquals("仙逆", r.getWorks().get(0).getTitle());
    }
}
