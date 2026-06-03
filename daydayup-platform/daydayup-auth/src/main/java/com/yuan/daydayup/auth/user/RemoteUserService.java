package com.yuan.daydayup.auth.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务：从本地数据库查询用户认证信息
 *
 * <p>auth 模块已拥有用户数据（sys_user / sys_user_role / sys_role_permission / sys_permission），
 * 不再需要通过 Feign 调用 admin-biz。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;

    /**
     * 按用户名查询认证用户信息（含密码哈希、权限、状态）。
     *
     * @param username 用户名
     * @return 用户信息；不存在时返回 {@link Optional#empty()}
     */
    public Optional<SimpleUser> findByUsername(String username) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            return Optional.empty();
        }
        Set<String> authorities = getUserAuthorities(user.getId());
        return Optional.of(SimpleUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .status(user.getStatus())
                .build());
    }

    /**
     * 回写用户最后登录信息（IP / 时间）。
     *
     * <p>失败不应阻断登录主流程，仅记录告警。</p>
     *
     * @param userId   用户 ID
     * @param clientIp 客户端 IP
     */
    public void updateLoginInfo(Long userId, String clientIp) {
        try {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                user.setLastLoginAt(java.time.LocalDateTime.now());
                user.setLastLoginIp(clientIp);
                userMapper.updateById(user);
            }
        } catch (Exception e) {
            log.warn("更新用户[{}]最后登录信息失败：{}", userId, e.getMessage());
        }
    }

    private Set<String> getUserAuthorities(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();

        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        if (rolePermissions.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId).toList();

        List<SysPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
                        .eq(SysPermission::getStatus, 1));
        return permissions.stream().map(SysPermission::getCode).collect(Collectors.toSet());
    }
}
