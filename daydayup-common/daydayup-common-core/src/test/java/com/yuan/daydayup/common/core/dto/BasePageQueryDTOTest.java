package com.yuan.daydayup.common.core.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BasePageQueryDTOTest {
    @Test
    void shouldUseDefaultPageValues() {
        BasePageQueryDTO query = new BasePageQueryDTO();
        assertThat(query.getPageNum()).isEqualTo(1L);
        assertThat(query.getPageSize()).isEqualTo(10L);
    }
    @Test
    void shouldNormalizeInvalidPageValues() {
        BasePageQueryDTO query = new BasePageQueryDTO();
        query.setPageNum(0L);
        query.setPageSize(200L);
        assertThat(query.normalizedPageNum()).isEqualTo(1L);
        assertThat(query.normalizedPageSize()).isEqualTo(100L);
    }
}
