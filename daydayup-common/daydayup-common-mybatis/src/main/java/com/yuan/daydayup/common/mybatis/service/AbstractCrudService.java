package com.yuan.daydayup.common.mybatis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;

public abstract class AbstractCrudService<M extends BaseMapper<E>, E extends BaseEntity, ID extends Serializable, CreateDTO, UpdateDTO, VO, Q extends BasePageQueryDTO, S extends BaseStatusDTO>
        implements BaseCrudService<ID, CreateDTO, UpdateDTO, VO, Q, S> {

    protected final M mapper;

    protected AbstractCrudService(M mapper) {
        this.mapper = mapper;
    }

    // ── BaseCrudService interface methods ───────────────────────────────

    @Override
    public PageResult<VO> page(Q query) {
        Page<E> page = mapper.selectPage(
                Page.of(query.normalizedPageNum(), query.normalizedPageSize()),
                buildPageQueryWrapper(query));
        List<VO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public VO detail(ID id) {
        E entity = requireById(id);
        return toVO(entity);
    }

    @Override
    public VO create(CreateDTO createDTO) {
        validateBeforeCreate(createDTO);
        E entity = toEntity(createDTO);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public VO update(ID id, UpdateDTO updateDTO) {
        validateBeforeUpdate(id, updateDTO);
        E entity = requireById(id);
        updateEntity(entity, updateDTO);
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    public void changeStatus(ID id, S statusDTO) {
        validateBeforeStatusChange(id, statusDTO);
        E entity = requireById(id);
        updateStatus(entity, statusDTO);
        mapper.updateById(entity);
    }

    // ── Hook methods (override in subclass for custom validation) ───────

    protected void validateBeforeCreate(CreateDTO createDTO) {
    }

    protected void validateBeforeUpdate(ID id, UpdateDTO updateDTO) {
    }

    protected void validateBeforeStatusChange(ID id, S statusDTO) {
    }

    // ── Template methods (must be implemented by subclass) ──────────────

    protected abstract LambdaQueryWrapper<E> buildPageQueryWrapper(Q query);

    protected abstract E toEntity(CreateDTO createDTO);

    protected abstract void updateEntity(E entity, UpdateDTO updateDTO);

    protected abstract VO toVO(E entity);

    protected abstract void updateStatus(E entity, S statusDTO);

    // ── Internal helpers ────────────────────────────────────────────────

    protected E requireById(ID id) {
        E entity = mapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND, "记录不存在");
        }
        return entity;
    }
}
