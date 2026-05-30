package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.DictCreateDTO;
import com.yuan.daydayup.admin.dto.DictUpdateDTO;
import com.yuan.daydayup.admin.entity.SysDict;
import com.yuan.daydayup.admin.mapper.SysDictMapper;
import com.yuan.daydayup.admin.service.impl.DictServiceImpl;
import com.yuan.daydayup.admin.vo.DictVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictServiceImplTest {

    private SysDictMapper mapper;
    private DictServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysDictMapper.class);
        service = new DictServiceImpl(mapper);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        DictCreateDTO dto = new DictCreateDTO();
        dto.setCode("gender");
        dto.setName("性别");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字典编码已存在");
    }

    @Test
    void shouldRejectDuplicateCodeOnUpdate() {
        DictUpdateDTO dto = new DictUpdateDTO();
        dto.setCode("gender");
        dto.setName("性别");

        SysDict existing = new SysDict();
        existing.setId(1L);
        existing.setCode("gender");
        existing.setName("旧字典");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("字典编码已存在");
    }

    @Test
    void shouldCreateDictSuccessfully() {
        DictCreateDTO dto = new DictCreateDTO();
        dto.setCode("gender");
        dto.setName("性别");

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SysDict.class))).thenReturn(1);

        DictVO vo = service.create(dto);

        assertThat(vo.getCode()).isEqualTo("gender");
        assertThat(vo.getName()).isEqualTo("性别");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(mapper).insert(any(SysDict.class));
    }

    @Test
    void shouldDeleteDict() {
        when(mapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(mapper).deleteById(1L);
    }
}
