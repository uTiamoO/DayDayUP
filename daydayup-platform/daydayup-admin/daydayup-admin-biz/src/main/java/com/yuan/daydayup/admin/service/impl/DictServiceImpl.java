package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.DictCreateDTO;
import com.yuan.daydayup.admin.dto.DictPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictStatusDTO;
import com.yuan.daydayup.admin.dto.DictUpdateDTO;
import com.yuan.daydayup.admin.entity.SysDict;
import com.yuan.daydayup.admin.entity.SysDictItem;
import com.yuan.daydayup.admin.mapper.SysDictItemMapper;
import com.yuan.daydayup.admin.mapper.SysDictMapper;
import com.yuan.daydayup.admin.service.DictService;
import com.yuan.daydayup.admin.vo.DictVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictServiceImpl
        extends AbstractCrudService<SysDictMapper, SysDict, Long, DictCreateDTO, DictUpdateDTO, DictVO, DictPageQueryDTO, DictStatusDTO>
        implements DictService {

    private final SysDictItemMapper dictItemMapper;

    public DictServiceImpl(SysDictMapper mapper, SysDictItemMapper dictItemMapper) {
        super(mapper);
        this.dictItemMapper = dictItemMapper;
    }

    @Override
    protected LambdaQueryWrapper<SysDict> buildPageQueryWrapper(DictPageQueryDTO query) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysDict::getName, query.getName())
               .eq(query.getStatus() != null, SysDict::getStatus, query.getStatus())
               .orderByDesc(SysDict::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysDict toEntity(DictCreateDTO dto) {
        SysDict entity = new SysDict();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysDict entity, DictUpdateDTO dto) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
    }

    @Override
    protected DictVO toVO(SysDict entity) {
        return DictVO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    @Override
    protected void updateStatus(SysDict entity, DictStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    @Override
    protected void validateBeforeCreate(DictCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, DictUpdateDTO dto) {
        checkCodeUnique(dto.getCode(), id);
    }

    @Override
    public void delete(Long id) {
        SysDict dict = mapper.selectById(id);
        if (dict == null) {
            return;
        }
        Long itemCount = dictItemMapper.selectCount(
                new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictCode, dict.getCode()));
        if (itemCount > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "字典下存在字典项，不允许删除");
        }
        mapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysDict::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "字典编码已存在");
        }
    }
}
