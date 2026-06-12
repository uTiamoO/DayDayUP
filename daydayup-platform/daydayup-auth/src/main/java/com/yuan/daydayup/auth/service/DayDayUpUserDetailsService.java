package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SAS 认证用的 UserDetailsService —— 直接查本地 auth 库。
 */
@Service
@RequiredArgsConstructor
public class DayDayUpUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        Collection<GrantedAuthority> authorities = loadAuthorities(user.getId());
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;

        return new DayDayUpUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                enabled,
                authorities);
    }

    private Collection<GrantedAuthority> loadAuthorities(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<Long> enabledRoleIds = roleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getId)
                .toList();
        if (enabledRoleIds.isEmpty()) {
            return List.of();
        }

        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, enabledRoleIds));
        if (rolePermissions.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();

        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        return permissions.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .map(p -> new SimpleGrantedAuthority(p.getCode()))
                .collect(Collectors.toList());
    }
}
