package com.yuan.daydayup.common.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractCrudServiceTest {

    private TestMapper mapper;
    private TestService service;

    @BeforeEach
    void setUp() {
        mapper = mock(TestMapper.class);
        service = new TestService(mapper);
    }

    // ── page ────────────────────────────────────────────────────────────

    @Nested
    class PageTest {

        @Test
        void shouldReturnPagedRecords() {
            TestEntity entity = new TestEntity();
            entity.setName("alice");

            Page<TestEntity> page = new Page<>(1, 10);
            page.setRecords(List.of(entity));
            page.setTotal(1);
            when(mapper.selectPage(any(Page.class), any())).thenReturn(page);

            BasePageQueryDTO query = new BasePageQueryDTO();
            PageResult<String> result = service.page(query);

            assertThat(result.getRecords()).containsExactly("VO:alice");
            assertThat(result.getTotal()).isEqualTo(1L);
        }

        @Test
        void shouldReturnEmptyPage() {
            Page<TestEntity> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0);
            when(mapper.selectPage(any(Page.class), any())).thenReturn(page);

            BasePageQueryDTO query = new BasePageQueryDTO();
            PageResult<String> result = service.page(query);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
        }
    }

    // ── detail ──────────────────────────────────────────────────────────

    @Nested
    class DetailTest {

        @Test
        void shouldReturnVOWhenFound() {
            TestEntity entity = new TestEntity();
            entity.setName("bob");
            when(mapper.selectById(42L)).thenReturn(entity);

            String vo = service.detail(42L);

            assertThat(vo).isEqualTo("VO:bob");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(mapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.detail(99L))
                    .isInstanceOf(BizException.class)
                    .hasMessage("记录不存在");
        }
    }

    // ── create ──────────────────────────────────────────────────────────

    @Nested
    class CreateTest {

        @Test
        void shouldInsertAndReturnVO() {
            when(mapper.insert(any(TestEntity.class))).thenReturn(1);

            String vo = service.create("req");

            assertThat(vo).isEqualTo("VO:req");
            verify(mapper).insert(any(TestEntity.class));
        }

        @Test
        void shouldCallValidateBeforeCreate() {
            when(mapper.insert(any(TestEntity.class))).thenReturn(1);
            ValidatingService validatingService = new ValidatingService(mapper);

            validatingService.create("test");

            assertThat(validatingService.createValidated).isTrue();
        }
    }

    // ── update ──────────────────────────────────────────────────────────

    @Nested
    class UpdateTest {

        @Test
        void shouldUpdateExistingEntityAndReturnVO() {
            TestEntity entity = new TestEntity();
            entity.setName("old");
            when(mapper.selectById(1L)).thenReturn(entity);
            when(mapper.updateById(any(TestEntity.class))).thenReturn(1);

            String vo = service.update(1L, "new");

            assertThat(vo).isEqualTo("VO:new");
            verify(mapper).updateById(any(TestEntity.class));
        }

        @Test
        void shouldThrowWhenUpdatingNonExistent() {
            when(mapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.update(99L, "new"))
                    .isInstanceOf(BizException.class)
                    .hasMessage("记录不存在");
        }
    }

    // ── changeStatus ────────────────────────────────────────────────────

    @Nested
    class ChangeStatusTest {

        @Test
        void shouldUpdateStatus() {
            TestEntity entity = new TestEntity();
            entity.setName("carol");
            entity.setStatus(0);
            when(mapper.selectById(5L)).thenReturn(entity);
            when(mapper.updateById(any(TestEntity.class))).thenReturn(1);

            BaseStatusDTO statusDTO = new BaseStatusDTO();
            statusDTO.setStatus(1);
            service.changeStatus(5L, statusDTO);

            assertThat(entity.getStatus()).isEqualTo(1);
            verify(mapper).updateById(entity);
        }

        @Test
        void shouldThrowWhenChangingStatusOfNonExistent() {
            when(mapper.selectById(99L)).thenReturn(null);

            BaseStatusDTO statusDTO = new BaseStatusDTO();
            statusDTO.setStatus(1);

            assertThatThrownBy(() -> service.changeStatus(99L, statusDTO))
                    .isInstanceOf(BizException.class)
                    .hasMessage("记录不存在");
        }
    }

    // ── test doubles ────────────────────────────────────────────────────

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class TestEntity extends BaseEntity {
        private String name;
        private Integer status;
    }

    interface TestMapper extends BaseMapper<TestEntity> {
    }

    static class TestService extends AbstractCrudService<TestMapper, TestEntity, Long, String, String, String, BasePageQueryDTO, BaseStatusDTO> {

        TestService(TestMapper mapper) {
            super(mapper);
        }

        @Override
        protected LambdaQueryWrapper<TestEntity> buildPageQueryWrapper(BasePageQueryDTO query) {
            return new LambdaQueryWrapper<>();
        }

        @Override
        protected TestEntity toEntity(String createDTO) {
            TestEntity entity = new TestEntity();
            entity.setName(createDTO);
            return entity;
        }

        @Override
        protected void updateEntity(TestEntity entity, String updateDTO) {
            entity.setName(updateDTO);
        }

        @Override
        protected String toVO(TestEntity entity) {
            return "VO:" + entity.getName();
        }

        @Override
        protected void updateStatus(TestEntity entity, BaseStatusDTO statusDTO) {
            entity.setStatus(statusDTO.getStatus());
        }
    }

    static class ValidatingService extends TestService {
        boolean createValidated;

        ValidatingService(TestMapper mapper) {
            super(mapper);
        }

        @Override
        protected void validateBeforeCreate(String createDTO) {
            createValidated = true;
        }
    }
}
