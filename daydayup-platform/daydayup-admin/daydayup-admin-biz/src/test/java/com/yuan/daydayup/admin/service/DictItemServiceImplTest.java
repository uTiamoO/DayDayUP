package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.DictItemCreateDTO;
import com.yuan.daydayup.admin.dto.DictItemUpdateDTO;
import com.yuan.daydayup.admin.entity.SysDictItem;
import com.yuan.daydayup.admin.mapper.SysDictItemMapper;
import com.yuan.daydayup.admin.service.impl.DictItemServiceImpl;
import com.yuan.daydayup.admin.vo.DictItemVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictItemServiceImplTest {

    private SysDictItemMapper mapper;
    private DictItemServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysDictItemMapper.class);
        service = new DictItemServiceImpl(mapper);
    }

    @Test
    void shouldRejectDuplicateDictCodeValueOnCreate() {
        DictItemCreateDTO dto = new DictItemCreateDTO();
        dto.setDictCode("gender");
        dto.setValue("male");
        dto.setLabel("男");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字典项编码值已存在");
    }

    @Test
    void shouldRejectDuplicateDictCodeValueOnUpdate() {
        DictItemUpdateDTO dto = new DictItemUpdateDTO();
        dto.setDictCode("gender");
        dto.setValue("male");
        dto.setLabel("男");

        SysDictItem existing = new SysDictItem();
        existing.setId(1L);
        existing.setDictCode("gender");
        existing.setValue("male");
        existing.setLabel("旧标签");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字典项编码值已存在");
    }

    @Test
    void shouldCreateDictItemSuccessfully() {
        DictItemCreateDTO dto = new DictItemCreateDTO();
        dto.setDictCode("gender");
        dto.setValue("male");
        dto.setLabel("男");
        dto.setSort(1);

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SysDictItem.class))).thenReturn(1);

        DictItemVO vo = service.create(dto);

        assertThat(vo.getDictCode()).isEqualTo("gender");
        assertThat(vo.getValue()).isEqualTo("male");
        assertThat(vo.getLabel()).isEqualTo("男");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(mapper).insert(any(SysDictItem.class));
    }

    @Test
    void shouldDeleteDictItem() {
        when(mapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(mapper).deleteById(1L);
    }
}
