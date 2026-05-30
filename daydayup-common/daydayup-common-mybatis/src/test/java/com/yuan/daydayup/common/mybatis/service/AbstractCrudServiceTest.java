package com.yuan.daydayup.common.mybatis.service;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.page.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCrudServiceTest {

    @Test
    void shouldExposeCrudContract() {
        TestService service = new TestService();
        BasePageQueryDTO query = new BasePageQueryDTO();
        BaseStatusDTO status = new BaseStatusDTO();
        status.setStatus(0);

        assertThat(service.page(query).getRecords()).containsExactly("page");
        assertThat(service.detail(1L)).isEqualTo("detail-1");
        assertThat(service.create("create")).isEqualTo("created-create");
        assertThat(service.update(1L, "update")).isEqualTo("updated-1-update");
        service.changeStatus(1L, status);
        assertThat(service.statusChanged).isTrue();
    }

    private static class TestService implements BaseCrudService<Long, String, String, String, BasePageQueryDTO, BaseStatusDTO> {
        private boolean statusChanged;

        @Override
        public PageResult<String> page(BasePageQueryDTO query) {
            return PageResult.of(List.of("page"), 1L, 1L, 10L);
        }

        @Override
        public String detail(Long id) {
            return "detail-" + id;
        }

        @Override
        public String create(String createDTO) {
            return "created-" + createDTO;
        }

        @Override
        public String update(Long id, String updateDTO) {
            return "updated-" + id + "-" + updateDTO;
        }

        @Override
        public void changeStatus(Long id, BaseStatusDTO statusDTO) {
            statusChanged = true;
        }
    }
}
