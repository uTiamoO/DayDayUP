package com.yuan.daydayup.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.auth.entity.SysPermission;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import com.yuan.daydayup.auth.entity.SysUserRole;
import com.yuan.daydayup.auth.mapper.SysPermissionMapper;
import com.yuan.daydayup.auth.mapper.SysRolePermissionMapper;
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
public class PermissionManageService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PermissionVersionService permissionVersionService;

    public PageResult<PermissionVO> page(PermissionPageQuery query) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .like(query.getName() != null && !query.getName().isBlank(),
                        SysPermission::getName, query.getName())
                .eq(query.getType() != null && !query.getType().isBlank(),
                        SysPermission::getType, query.getType())
                .eq(query.getStatus() != null, SysPermission::getStatus, query.getStatus())
                .orderByAsc(SysPermission::getSort)
                .orderByDesc(SysPermission::getCreateTime);

        Page<SysPermission> page = permissionMapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()), wrapper);
        List<PermissionVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PermissionVO detail(Long id) {
        return toVO(requireById(id));
    }

    /** 查询全部权限（扁平列表，按 sort、id 升序），供角色授权选择器使用。 */
    public List<PermissionVO> listAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSort)
                        .orderByAsc(SysPermission::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionVO create(PermissionCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
        SysPermission permission = new SysPermission();
        permission.setCode(dto.getCode());
        permission.setName(dto.getName());
        permission.setType(dto.getType());
        permission.setParentId(dto.getParentId());
        permission.setPath(dto.getPath());
        permission.setSort(dto.getSort());
        permission.setRemark(dto.getRemark());
        permission.setStatus(1);
        permissionMapper.insert(permission);
        return toVO(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    public PermissionVO update(Long id, PermissionUpdateDTO dto) {
        SysPermission permission = requireById(id);
        checkCodeUnique(dto.getCode(), id);
        permission.setCode(dto.getCode());
        permission.setName(dto.getName());
        permission.setType(dto.getType());
        permission.setParentId(dto.getParentId());
        permission.setPath(dto.getPath());
        permission.setSort(dto.getSort());
        permission.setRemark(dto.getRemark());
        permissionMapper.updateById(permission);
        markUsersAssignedToPermissionRevoked(id);
        return toVO(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, PermissionStatusDTO dto) {
        SysPermission permission = requireById(id);
        permission.setStatus(dto.getStatus());
        permissionMapper.updateById(permission);
        if (dto.getStatus() != null && dto.getStatus() == 0) {
            markUsersAssignedToPermissionRevoked(id);
        }
    }

    private void markUsersAssignedToPermissionRevoked(Long permissionId) {
        List<Long> roleIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, permissionId))
                .stream()
                .map(SysRolePermission::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return;
        }
        userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .in(SysUserRole::getRoleId, roleIds))
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .forEach(permissionVersionService::markPermissionRevoked);
    }

    private SysPermission requireById(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "权限不存在");
        }
        return permission;
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPermission::getId, excludeId);
        }
        if (permissionMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "权限编码已存在");
        }
    }

    private PermissionVO toVO(SysPermission permission) {
        return PermissionVO.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .type(permission.getType())
                .parentId(permission.getParentId())
                .path(permission.getPath())
                .sort(permission.getSort())
                .status(permission.getStatus())
                .remark(permission.getRemark())
                .createTime(permission.getCreateTime())
                .updateTime(permission.getUpdateTime())
                .build();
    }
}
