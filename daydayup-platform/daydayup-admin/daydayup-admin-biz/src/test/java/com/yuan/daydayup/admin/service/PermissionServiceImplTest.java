package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.PermissionCreateDTO;
import com.yuan.daydayup.admin.dto.PermissionUpdateDTO;
import com.yuan.daydayup.admin.entity.SysPermission;
import com.yuan.daydayup.admin.mapper.SysPermissionMapper;
import com.yuan.daydayup.admin.service.impl.PermissionServiceImpl;
import com.yuan.daydayup.admin.vo.PermissionVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceImplTest {

    private SysPermissionMapper mapper;
    private PermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysPermissionMapper.class);
        service = new PermissionServiceImpl(mapper);
    }

    @Test
    void shouldRejectDuplicateCodeOnCreate() {
        PermissionCreateDTO dto = new PermissionCreateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("button");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("权限编码已存在");
    }

    @Test
    void shouldRejectDuplicateCodeOnUpdate() {
        PermissionUpdateDTO dto = new PermissionUpdateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("button");

        SysPermission existing = new SysPermission();
        existing.setId(1L);
        existing.setCode("admin:user:list");
        existing.setName("旧权限");
        existing.setType("button");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("权限编码已存在");
    }

    @Test
    void shouldCreatePermissionSuccessfully() {
        PermissionCreateDTO dto = new PermissionCreateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("button");

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(SysPermission.class))).thenReturn(1);

        PermissionVO vo = service.create(dto);

        assertThat(vo.getCode()).isEqualTo("admin:user:list");
        assertThat(vo.getName()).isEqualTo("用户列表");
        assertThat(vo.getType()).isEqualTo("button");
        assertThat(vo.getStatus()).isEqualTo(1);
        verify(mapper).insert(any(SysPermission.class));
    }
}
