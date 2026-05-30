package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.DictItemCreateDTO;
import com.yuan.daydayup.admin.dto.DictItemPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictItemStatusDTO;
import com.yuan.daydayup.admin.dto.DictItemUpdateDTO;
import com.yuan.daydayup.admin.entity.SysDictItem;
import com.yuan.daydayup.admin.mapper.SysDictItemMapper;
import com.yuan.daydayup.admin.service.DictItemService;
import com.yuan.daydayup.admin.vo.DictItemVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictItemServiceImpl
        extends AbstractCrudService<SysDictItemMapper, SysDictItem, Long, DictItemCreateDTO, DictItemUpdateDTO, DictItemVO, DictItemPageQueryDTO, DictItemStatusDTO>
        implements DictItemService {

    public DictItemServiceImpl(SysDictItemMapper mapper) {
        super(mapper);
    }

    @Override
    protected LambdaQueryWrapper<SysDictItem> buildPageQueryWrapper(DictItemPageQueryDTO query) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getDictCode()), SysDictItem::getDictCode, query.getDictCode())
               .like(StringUtils.hasText(query.getLabel()), SysDictItem::getLabel, query.getLabel())
               .eq(query.getStatus() != null, SysDictItem::getStatus, query.getStatus())
               .orderByAsc(SysDictItem::getSort)
               .orderByDesc(SysDictItem::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysDictItem toEntity(DictItemCreateDTO dto) {
        SysDictItem entity = new SysDictItem();
        entity.setDictCode(dto.getDictCode());
        entity.setValue(dto.getValue());
        entity.setLabel(dto.getLabel());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysDictItem entity, DictItemUpdateDTO dto) {
        entity.setDictCode(dto.getDictCode());
        entity.setValue(dto.getValue());
        entity.setLabel(dto.getLabel());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
    }

    @Override
    protected DictItemVO toVO(SysDictItem entity) {
        return DictItemVO.builder()
                .id(entity.getId())
                .dictCode(entity.getDictCode())
                .value(entity.getValue())
                .label(entity.getLabel())
                .sort(entity.getSort())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .build();
    }

    @Override
    protected void updateStatus(SysDictItem entity, DictItemStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    @Override
    protected void validateBeforeCreate(DictItemCreateDTO dto) {
        checkDictCodeValueUnique(dto.getDictCode(), dto.getValue(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, DictItemUpdateDTO dto) {
        checkDictCodeValueUnique(dto.getDictCode(), dto.getValue(), id);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void checkDictCodeValueUnique(String dictCode, String value, Long excludeId) {
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictCode, dictCode)
                .eq(SysDictItem::getValue, value);
        if (excludeId != null) {
            wrapper.ne(SysDictItem::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "字典项编码值已存在");
        }
    }
}
