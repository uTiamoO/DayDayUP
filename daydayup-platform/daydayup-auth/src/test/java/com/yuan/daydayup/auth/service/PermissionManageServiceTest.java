package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionManageServiceTest {

    private SysPermissionMapper permissionMapper;
    private SysRolePermissionMapper rolePermissionMapper;
    private SysUserRoleMapper userRoleMapper;
    private PermissionVersionService permissionVersionService;
    private PermissionManageService service;

    @BeforeEach
    void setUp() {
        permissionMapper = mock(SysPermissionMapper.class);
        rolePermissionMapper = mock(SysRolePermissionMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        permissionVersionService = mock(PermissionVersionService.class);
        service = new PermissionManageService(permissionMapper, rolePermissionMapper, userRoleMapper,
                permissionVersionService);
    }

    @Test
    void createRejectsDuplicatePermissionCode() {
        PermissionCreateDTO dto = new PermissionCreateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("api");

        when(permissionMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("权限编码已存在");
    }

    @Test
    void createDefaultsPermissionToEnabled() {
        PermissionCreateDTO dto = new PermissionCreateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("api");
        dto.setParentId(0L);
        dto.setSort(10);

        when(permissionMapper.selectCount(any())).thenReturn(0L);
        when(permissionMapper.insert(any(SysPermission.class))).thenAnswer(invocation -> {
            SysPermission permission = invocation.getArgument(0);
            permission.setId(20L);
            return 1;
        });

        PermissionVO vo = service.create(dto);

        assertThat(vo.getId()).isEqualTo(20L);
        assertThat(vo.getCode()).isEqualTo("admin:user:list");
        assertThat(vo.getName()).isEqualTo("用户列表");
        assertThat(vo.getType()).isEqualTo("api");
        assertThat(vo.getParentId()).isEqualTo(0L);
        assertThat(vo.getStatus()).isEqualTo(1);
    }

    @Test
    void changeStatusMarksUsersAssignedThroughRolePermissionAsRevoked() {
        SysPermission permission = new SysPermission();
        permission.setId(3L);
        permission.setCode("admin:user:list");
        permission.setName("用户列表");
        permission.setStatus(1);
        when(permissionMapper.selectById(3L)).thenReturn(permission);
        when(permissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(8L);
        rolePermission.setPermissionId(3L);
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rolePermission));

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(99L);
        userRole.setRoleId(8L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));

        PermissionStatusDTO dto = new PermissionStatusDTO();
        dto.setStatus(0);

        service.changeStatus(3L, dto);

        verify(permissionVersionService).markPermissionRevoked(99L);
    }

    @Test
    void updateRejectsDuplicatePermissionCodeExcludingCurrentPermission() {
        SysPermission permission = new SysPermission();
        permission.setId(3L);
        permission.setCode("old");
        permission.setName("旧权限");
        when(permissionMapper.selectById(3L)).thenReturn(permission);
        when(permissionMapper.selectCount(any())).thenReturn(1L);

        PermissionUpdateDTO dto = new PermissionUpdateDTO();
        dto.setCode("admin:user:list");
        dto.setName("用户列表");
        dto.setType("api");

        assertThatThrownBy(() -> service.update(3L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("权限编码已存在");
    }
}
