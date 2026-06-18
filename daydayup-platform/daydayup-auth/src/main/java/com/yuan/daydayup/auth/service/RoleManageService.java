package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.mapper.SysUserRoleMapper;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleManageService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PermissionVersionService permissionVersionService;

    public PageResult<RoleVO> page(RolePageQuery query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(query.getName() != null && !query.getName().isBlank(), SysRole::getName, query.getName())
                .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                .orderByAsc(SysRole::getSort)
                .orderByDesc(SysRole::getCreateTime);

        Page<SysRole> page = roleMapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()), wrapper);
        List<RoleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public RoleVO detail(Long id) {
        return toVO(requireById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleVO create(RoleCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setSort(dto.getSort());
        role.setRemark(dto.getRemark());
        role.setStatus(1);
        roleMapper.insert(role);
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleVO update(Long id, RoleUpdateDTO dto) {
        SysRole role = requireById(id);
        checkCodeUnique(dto.getCode(), id);
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setSort(dto.getSort());
        role.setRemark(dto.getRemark());
        roleMapper.updateById(role);
        return toVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, RoleStatusDTO dto) {
        SysRole role = requireById(id);
        role.setStatus(dto.getStatus());
        roleMapper.updateById(role);
        if (dto.getStatus() != null && dto.getStatus() == 0) {
            markUsersAssignedToRoleRevoked(id);
        }
    }

    private void markUsersAssignedToRoleRevoked(Long roleId) {
        userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, roleId))
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .forEach(permissionVersionService::markPermissionRevoked);
    }

    private SysRole requireById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "角色编码已存在");
        }
    }

    private RoleVO toVO(SysRole role) {
        return RoleVO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .sort(role.getSort())
                .status(role.getStatus())
                .remark(role.getRemark())
                .createTime(role.getCreateTime())
                .updateTime(role.getUpdateTime())
                .build();
    }
}
