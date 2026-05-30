package com.yuan.daydayup.common.core.page;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {
    @Test
    void shouldCreatePageResult() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 12L, 2L, 5L);
        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getTotal()).isEqualTo(12L);
        assertThat(result.getPageNum()).isEqualTo(2L);
        assertThat(result.getPageSize()).isEqualTo(5L);
        assertThat(result.getPages()).isEqualTo(3L);
    }
}
