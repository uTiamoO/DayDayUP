package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.RoleCreateDTO;
import com.yuan.daydayup.admin.dto.RolePageQueryDTO;
import com.yuan.daydayup.admin.dto.RoleStatusDTO;
import com.yuan.daydayup.admin.dto.RoleUpdateDTO;
import com.yuan.daydayup.admin.entity.SysRole;
import com.yuan.daydayup.admin.mapper.SysRoleMapper;
import com.yuan.daydayup.admin.service.RoleService;
import com.yuan.daydayup.admin.vo.RoleVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl
        extends AbstractCrudService<SysRoleMapper, SysRole, Long, RoleCreateDTO, RoleUpdateDTO, RoleVO, RolePageQueryDTO, RoleStatusDTO>
        implements RoleService {

    public RoleServiceImpl(SysRoleMapper mapper) {
        super(mapper);
    }

    @Override
    protected LambdaQueryWrapper<SysRole> buildPageQueryWrapper(RolePageQueryDTO query) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysRole::getName, query.getName())
               .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
               .orderByAsc(SysRole::getSort)
               .orderByDesc(SysRole::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysRole toEntity(RoleCreateDTO dto) {
        SysRole entity = new SysRole();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysRole entity, RoleUpdateDTO dto) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
    }

    @Override
    protected RoleVO toVO(SysRole entity) {
        return RoleVO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .sort(entity.getSort())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    @Override
    protected void updateStatus(SysRole entity, RoleStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    @Override
    protected void validateBeforeCreate(RoleCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, RoleUpdateDTO dto) {
        checkCodeUnique(dto.getCode(), id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "角色编码已存在");
        }
    }
}
