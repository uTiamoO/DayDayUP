package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.MenuCreateDTO;
import com.yuan.daydayup.admin.dto.MenuUpdateDTO;
import com.yuan.daydayup.admin.entity.SysMenu;
import com.yuan.daydayup.admin.mapper.SysMenuMapper;
import com.yuan.daydayup.admin.service.impl.MenuServiceImpl;
import com.yuan.daydayup.admin.vo.MenuVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuServiceImplTest {

    private SysMenuMapper mapper;
    private MenuServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysMenuMapper.class);
        service = new MenuServiceImpl(mapper);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setCode("system");
        dto.setName("系统管理");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("菜单编码已存在");
    }

    @Test
    void shouldRejectDuplicateCodeOnUpdate() {
        MenuUpdateDTO dto = new MenuUpdateDTO();
        dto.setCode("system");
        dto.setName("系统管理");

        SysMenu existing = new SysMenu();
        existing.setId(1L);
        existing.setCode("system");
        existing.setName("旧菜单");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("菜单编码已存在");
    }

    @Test
    void shouldRejectDeleteWhenHasChildren() {
        when(mapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("存在子菜单");
    }

    @Test
    void shouldDeleteMenuSuccessfully() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        verify(mapper).deleteById(1L);
    }

    @Test
    void shouldCreateMenuSuccessfully() {
        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setCode("user");
        dto.setName("用户管理");
        dto.setParentId(0L);
        dto.setType("menu");

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SysMenu.class))).thenReturn(1);

        MenuVO vo = service.create(dto);

        assertThat(vo.getCode()).isEqualTo("user");
        assertThat(vo.getName()).isEqualTo("用户管理");
        assertThat(vo.getParentId()).isEqualTo(0L);
        assertThat(vo.getType()).isEqualTo("menu");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(mapper).insert(any(SysMenu.class));
    }
}
