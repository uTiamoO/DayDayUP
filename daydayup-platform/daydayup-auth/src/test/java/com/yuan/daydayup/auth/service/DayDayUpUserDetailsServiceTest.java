package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.SysUser;
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
    }
}
