package com.yuan.daydayup.common.mybatis.service;

import com.yuan.daydayup.common.core.dto.BasePageQueryDTO;
import com.yuan.daydayup.common.core.dto.BaseStatusDTO;
import com.yuan.daydayup.common.core.page.PageResult;

import java.io.Serializable;

public interface BaseCrudService<ID extends Serializable, CreateDTO, UpdateDTO, VO, Q extends BasePageQueryDTO, S extends BaseStatusDTO> {
    PageResult<VO> page(Q query);
    VO detail(ID id);
    VO create(CreateDTO createDTO);
    VO update(ID id, UpdateDTO updateDTO);
    void changeStatus(ID id, S statusDTO);
}
