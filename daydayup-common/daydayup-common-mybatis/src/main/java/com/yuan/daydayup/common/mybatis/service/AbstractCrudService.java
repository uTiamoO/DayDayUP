package com.yuan.daydayup.common.mybatis.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;

public abstract class AbstractCrudService<M extends BaseMapper<E>, E extends BaseEntity, ID, CreateDTO, UpdateDTO, VO, Q extends BasePageQueryDTO, S extends BaseStatusDTO>
        implements BaseCrudService<ID, CreateDTO, UpdateDTO, VO, Q, S> {

    protected final M mapper;

    protected AbstractCrudService(M mapper) {
        this.mapper = mapper;
    }

    protected void validateBeforeCreate(CreateDTO createDTO) {
    }

    protected void validateBeforeUpdate(ID id, UpdateDTO updateDTO) {
    }

    protected void validateBeforeStatusChange(ID id, S statusDTO) {
    }

    protected abstract E toEntity(CreateDTO createDTO);

    protected abstract void updateEntity(E entity, UpdateDTO updateDTO);

    protected abstract VO toVO(E entity);
}
