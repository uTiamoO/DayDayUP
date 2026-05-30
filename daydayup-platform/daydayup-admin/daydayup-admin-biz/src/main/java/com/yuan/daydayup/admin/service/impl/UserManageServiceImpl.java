package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserPageQueryDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.entity.SysUser;
import com.yuan.daydayup.admin.mapper.SysUserMapper;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserManageServiceImpl
        extends AbstractCrudService<SysUserMapper, SysUser, Long, UserCreateDTO, UserUpdateDTO, UserDetailVO, UserPageQueryDTO, UserStatusDTO>
        implements UserManageService {

    private final PasswordEncoder passwordEncoder;

    public UserManageServiceImpl(SysUserMapper mapper, PasswordEncoder passwordEncoder) {
        super(mapper);
        this.passwordEncoder = passwordEncoder;
    }

    // ── Template methods ───────────────────────────────────────────────

    @Override
    protected LambdaQueryWrapper<SysUser> buildPageQueryWrapper(UserPageQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
               .like(StringUtils.hasText(query.getNickname()), SysUser::getNickname, query.getNickname())
               .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
               .orderByDesc(SysUser::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysUser toEntity(UserCreateDTO dto) {
        SysUser entity = new SysUser();
        entity.setUsername(dto.getUsername());
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setMobile(dto.getMobile());
        entity.setAvatar(dto.getAvatar());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysUser entity, UserUpdateDTO dto) {
        entity.setUsername(dto.getUsername());
        entity.setNickname(dto.getNickname());
        entity.setEmail(dto.getEmail());
        entity.setMobile(dto.getMobile());
        entity.setAvatar(dto.getAvatar());
        entity.setRemark(dto.getRemark());
    }

    @Override
    protected UserDetailVO toVO(SysUser entity) {
        return UserDetailVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .mobile(entity.getMobile())
                .avatar(entity.getAvatar())
                .status(entity.getStatus())
                .lastLoginAt(entity.getLastLoginAt())
                .remark(entity.getRemark())
                .build();
    }

    @Override
    protected void updateStatus(SysUser entity, UserStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    // ── Validation hooks ───────────────────────────────────────────────

    @Override
    protected void validateBeforeCreate(UserCreateDTO dto) {
        checkUsernameUnique(dto.getUsername(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, UserUpdateDTO dto) {
        checkUsernameUnique(dto.getUsername(), id);
    }

    // ── Public business methods ─────────────────────────────────────────

    @Override
    public void updateLoginInfo(Long userId, String lastLoginIp) {
        SysUser user = mapper.selectById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(lastLoginIp);
            mapper.updateById(user);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUser::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
    }
}
