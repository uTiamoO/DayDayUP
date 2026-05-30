package com.yuan.daydayup.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.api.feign.UserClient;
import com.yuan.daydayup.admin.api.vo.AuthUserVO;
import com.yuan.daydayup.admin.api.vo.UserVO;
import com.yuan.daydayup.admin.entity.SysPermission;
import com.yuan.daydayup.admin.entity.SysRolePermission;
import com.yuan.daydayup.admin.entity.SysUser;
import com.yuan.daydayup.admin.entity.SysUserRole;
import com.yuan.daydayup.admin.mapper.SysPermissionMapper;
import com.yuan.daydayup.admin.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.admin.mapper.SysUserMapper;
import com.yuan.daydayup.admin.mapper.SysUserRoleMapper;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户查询接口（实现 UserClient Feign 契约）
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController implements UserClient {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final UserManageService userManageService;

    @Override
    public R<UserVO> getUserById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return R.ok(null);
        }
        return R.ok(toVO(user));
    }

    @Override
    @GetMapping("/auth/{username}")
    public R<AuthUserVO> getAuthUserByUsername(@PathVariable("username") String username) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return R.ok(null);
        }

        Set<String> authorities = getUserAuthorities(user.getId());

        return R.ok(AuthUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .status(user.getStatus())
                .authorities(authorities)
                .build());
    }

    @Override
    @PatchMapping("/manage/{userId}/login-info")
    public R<Void> updateLoginInfo(@PathVariable("userId") Long userId,
                                   @RequestParam("lastLoginIp") String lastLoginIp) {
        userManageService.updateLoginInfo(userId, lastLoginIp);
        return R.ok();
    }

    private Set<String> getUserAuthorities(Long userId) {
        // 1. 用户 → 角色 ID 列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();

        // 2. 角色 → 权限 ID 列表
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> permissionIds = rolePermissions.stream().map(SysRolePermission::getPermissionId).toList();

        // 3. 权限 → 权限码
        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
                        .eq(SysPermission::getStatus, 1));
        return permissions.stream().map(SysPermission::getCode).collect(Collectors.toSet());
    }

    private static UserVO toVO(SysUser user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
