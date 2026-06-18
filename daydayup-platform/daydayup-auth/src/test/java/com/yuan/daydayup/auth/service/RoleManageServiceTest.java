package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleManageServiceTest {

    private SysRoleMapper roleMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysRolePermissionMapper rolePermissionMapper;
    private PermissionVersionService permissionVersionService;
    private RoleManageService service;

    @BeforeEach
    void setUp() {
        roleMapper = mock(SysRoleMapper.class);
        userRoleMapper = mock(SysUserRoleMapper.class);
        rolePermissionMapper = mock(SysRolePermissionMapper.class);
        permissionVersionService = mock(PermissionVersionService.class);
        service = new RoleManageService(roleMapper, userRoleMapper, rolePermissionMapper, permissionVersionService);
    }

    @Test
    void createRejectsDuplicateRoleCode() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setCode("admin");
        dto.setName("管理员");

        when(roleMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void createDefaultsRoleToEnabled() {
        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setCode("operator");
        dto.setName("运营");
        dto.setSort(5);

        when(roleMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
            SysRole role = invocation.getArgument(0);
            role.setId(10L);
            return 1;
        });

        RoleVO vo = service.create(dto);

        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getCode()).isEqualTo("operator");
        assertThat(vo.getName()).isEqualTo("运营");
        assertThat(vo.getSort()).isEqualTo(5);
        assertThat(vo.getStatus()).isEqualTo(1);
    }

    @Test
    void changeStatusMarksUsersAssignedToRoleAsRevoked() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setCode("operator");
        role.setName("运营");
        role.setStatus(1);
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(99L);
        userRole.setRoleId(2L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));

        RoleStatusDTO dto = new RoleStatusDTO();
        dto.setStatus(0);

        service.changeStatus(2L, dto);

        verify(permissionVersionService).markPermissionRevoked(99L);
    }

    @Test
    void updateRejectsDuplicateRoleCodeExcludingCurrentRole() {
        SysRole role = new SysRole();
        role.setId(2L);
        role.setCode("old");
        role.setName("旧角色");
        when(roleMapper.selectById(2L)).thenReturn(role);
        when(roleMapper.selectCount(any())).thenReturn(1L);

        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setCode("admin");
        dto.setName("管理员");

        assertThatThrownBy(() -> service.update(2L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("角色编码已存在");
    }

    @Test
    void assignPermissionsReplacesBindingsAndRevokesAssignedUsers() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(roleMapper.selectById(2L)).thenReturn(role);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(99L);
        userRole.setRoleId(2L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));

        service.assignPermissions(2L, List.of(10L, 11L));

        verify(rolePermissionMapper).physicalDeleteByRoleId(2L);
        verify(rolePermissionMapper, times(2)).insert(any(SysRolePermission.class));
        verify(permissionVersionService).markPermissionRevoked(99L);
    }

    @Test
    void getPermissionIdsReturnsAssignedPermissionIds() {
        SysRole role = new SysRole();
        role.setId(2L);
        when(roleMapper.selectById(2L)).thenReturn(role);

        SysRolePermission rp1 = new SysRolePermission();
        rp1.setPermissionId(10L);
        SysRolePermission rp2 = new SysRolePermission();
        rp2.setPermissionId(11L);
        when(rolePermissionMapper.selectList(any())).thenReturn(List.of(rp1, rp2));

        assertThat(service.getPermissionIds(2L)).containsExactly(10L, 11L);
    }
}
