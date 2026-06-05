package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysUser;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysUserMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.redis.util.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户管理服务（auth 模块，数据拥有方）
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private static final String CACHE_PREFIX = "daydayup:auth:user:";
    private static final UserDetailVO NULL_PLACEHOLDER = new UserDetailVO();

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheService cacheService;
    private final PermissionVersionService permissionVersionService;

    /**
     * 分页查询用户
     */
    public PageResult<UserDetailVO> page(UserPageQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getUsername() != null && !query.getUsername().isBlank(),
                        SysUser::getUsername, query.getUsername())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = userMapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()), wrapper);

        List<UserDetailVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询用户详情（含角色信息），带缓存
     */
    public UserDetailVO detail(Long id) {
        String key = CACHE_PREFIX + id;
        UserDetailVO cached = cacheService.getJson(key, UserDetailVO.class);
        if (cached != null) {
            return cached.getId() == null ? null : cached;
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            cacheService.setJson(key, NULL_PLACEHOLDER, 1, TimeUnit.MINUTES);
            return null;
        }
        UserDetailVO vo = toVO(user);
        cacheService.setJson(key, vo, 30, TimeUnit.MINUTES);
        return vo;
    }

    /**
     * 新增用户（BCrypt 编码密码 + 分配角色）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDetailVO create(UserCreateDTO dto) {
        checkUsernameUnique(dto.getUsername(), null);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        user.setStatus(1);
        userMapper.insert(user);

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), dto.getRoleIds());
        }

        return toVO(user);
    }

    /**
     * 更新用户信息和角色
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDetailVO update(Long id, UserUpdateDTO dto) {
        SysUser user = requireById(id);
        // 双删-1：先清旧缓存（与方法末尾的删除构成双删，缩小并发回填窗口）
        cacheService.delete(CACHE_PREFIX + id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setMobile(dto.getMobile());
        userMapper.updateById(user);

        // 重新分配角色：先删旧关联再插入新关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            assignRoles(id, dto.getRoleIds());
        }

        cacheService.delete(CACHE_PREFIX + id);
        // 角色被重新分配，可能收回权限，标记权限版本以强制旧 token 失效
        permissionVersionService.markPermissionRevoked(id);
        return toVO(user);
    }

    /**
     * 启用/停用用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, UserStatusDTO dto) {
        SysUser user = requireById(id);
        // 双删-1：先清旧缓存，降低并发读在更新窗口内回填旧值的概率
        cacheService.delete(CACHE_PREFIX + id);
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        // 双删-2：更新后再清一次（如需完全消除并发回填，应改为事务提交后删除）
        cacheService.delete(CACHE_PREFIX + id);
        // 停用即降权：标记权限版本，强制其已签发的 JWT 立即失效
        if (dto.getStatus() != null && dto.getStatus() == 0) {
            permissionVersionService.markPermissionRevoked(id);
        }
    }

    /**
     * 更新最后登录信息
     */
    public void updateLoginInfo(Long userId, String lastLoginIp) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(lastLoginIp);
            userMapper.updateById(user);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────

    private UserDetailVO toVO(SysUser user) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        List<String> roleCodes = Collections.emptyList();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            roleCodes = roles.stream()
                    .map(SysRole::getCode)
                    .collect(Collectors.toList());
        }
        return UserDetailVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .roleIds(roleIds)
                .roleCodes(roleCodes)
                .build();
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private SysUser requireById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
    }
}
