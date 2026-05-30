package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.RoleCreateDTO;
import com.yuan.daydayup.admin.dto.RoleUpdateDTO;
import com.yuan.daydayup.admin.entity.SysRole;
import com.yuan.daydayup.admin.mapper.SysRoleMapper;
import com.yuan.daydayup.admin.service.impl.RoleServiceImpl;
import com.yuan.daydayup.admin.vo.RoleVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    private SysRoleMapper mapper;
    private RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysRoleMapper.class);
        service = new RoleServiceImpl(mapper);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setCode("admin");
        dto.setName("管理员");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void shouldRejectDuplicateCodeOnUpdate() {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setCode("admin");
        dto.setName("管理员");

        SysRole existing = new SysRole();
        existing.setId(1L);
        existing.setCode("admin");
        existing.setName("旧角色");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void shouldCreateRoleSuccessfully() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setCode("test");
        dto.setName("测试角色");
        dto.setSort(1);
        dto.setRemark("remark");

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SysRole.class))).thenReturn(1);

        RoleVO vo = service.create(dto);

        assertThat(vo.getCode()).isEqualTo("test");
        assertThat(vo.getName()).isEqualTo("测试角色");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(mapper).insert(any(SysRole.class));
    }

    @Test
    void shouldUpdateRoleSuccessfully() {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setCode("updated");
        dto.setName("更新角色");

        SysRole existing = new SysRole();
        existing.setId(1L);
        existing.setCode("old");
        existing.setName("旧角色");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(SysRole.class))).thenReturn(1);

        RoleVO vo = service.update(1L, dto);

        assertThat(vo.getCode()).isEqualTo("updated");
        assertThat(vo.getName()).isEqualTo("更新角色");
        verify(mapper).updateById(any(SysRole.class));
    }
}
