package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.PermissionCreateDTO;
import com.yuan.daydayup.admin.dto.PermissionPageQueryDTO;
import com.yuan.daydayup.admin.dto.PermissionStatusDTO;
import com.yuan.daydayup.admin.dto.PermissionUpdateDTO;
import com.yuan.daydayup.admin.entity.SysPermission;
import com.yuan.daydayup.admin.mapper.SysPermissionMapper;
import com.yuan.daydayup.admin.service.PermissionService;
import com.yuan.daydayup.admin.vo.PermissionVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PermissionServiceImpl
        extends AbstractCrudService<SysPermissionMapper, SysPermission, Long, PermissionCreateDTO, PermissionUpdateDTO, PermissionVO, PermissionPageQueryDTO, PermissionStatusDTO>
        implements PermissionService {

    public PermissionServiceImpl(SysPermissionMapper mapper) {
        super(mapper);
    }

    @Override
    protected LambdaQueryWrapper<SysPermission> buildPageQueryWrapper(PermissionPageQueryDTO query) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysPermission::getName, query.getName())
               .eq(StringUtils.hasText(query.getType()), SysPermission::getType, query.getType())
               .eq(query.getStatus() != null, SysPermission::getStatus, query.getStatus())
               .orderByAsc(SysPermission::getSort)
               .orderByDesc(SysPermission::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysPermission toEntity(PermissionCreateDTO dto) {
        SysPermission entity = new SysPermission();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setParentId(dto.getParentId());
        entity.setPath(dto.getPath());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysPermission entity, PermissionUpdateDTO dto) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setParentId(dto.getParentId());
        entity.setPath(dto.getPath());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
    }

    @Override
    protected PermissionVO toVO(SysPermission entity) {
        return PermissionVO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .type(entity.getType())
                .parentId(entity.getParentId())
                .path(entity.getPath())
                .sort(entity.getSort())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .build();
    }

    @Override
    protected void updateStatus(SysPermission entity, PermissionStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    @Override
    protected void validateBeforeCreate(PermissionCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, PermissionUpdateDTO dto) {
        checkCodeUnique(dto.getCode(), id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPermission::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "权限编码已存在");
        }
    }
}
