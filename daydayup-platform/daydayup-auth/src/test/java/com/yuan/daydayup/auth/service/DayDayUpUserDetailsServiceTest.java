package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DayDayUpUserDetailsServiceTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysRolePermissionMapper rolePermissionMapper = mock(SysRolePermissionMapper.class);
    private final SysPermissionMapper permissionMapper = mock(SysPermissionMapper.class);

    private final DayDayUpUserDetailsService service = new DayDayUpUserDetailsService(
            userMapper, userRoleMapper, roleMapper, rolePermissionMapper, permissionMapper);

    @Test
    void loadUserByUsername_notFound() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_found_noRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("$2a$10$hash");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(userRoleMapper.selectList(any())).thenReturn(java.util.List.of());

        UserDetails result = service.loadUserByUsername("test");
        assertEquals("test", result.getUsername());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().isEmpty());
        // 必须返回携带 userId 的 DayDayUpUser，jwtCustomizer 据此注入 uid claim
        assertInstanceOf(DayDayUpUser.class, result);
        assertEquals(1L, ((DayDayUpUser) result).getUserId());
    }

    @Test
    void loadUserByUsername_ignoresDisabledRoles() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("test");
        user.setPassword("$2a$10$hash");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(1L);
        userRole.setRoleId(2L);
        when(userRoleMapper.selectList(any())).thenReturn(java.util.List.of(userRole));

        SysRole disabledRole = new SysRole();
        disabledRole.setId(2L);
        disabledRole.setStatus(0);
        when(roleMapper.selectBatchIds(java.util.List.of(2L))).thenReturn(java.util.List.of(disabledRole));

        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setRoleId(2L);
        rolePermission.setPermissionId(3L);
        when(rolePermissionMapper.selectList(any())).thenReturn(java.util.List.of(rolePermission));

        SysPermission permission = new SysPermission();
        permission.setId(3L);
        permission.setCode("admin:user:list");
        permission.setStatus(1);
        when(permissionMapper.selectBatchIds(java.util.List.of(3L))).thenReturn(java.util.List.of(permission));

        UserDetails result = service.loadUserByUsername("test");

        assertTrue(result.getAuthorities().isEmpty());
    }
}
