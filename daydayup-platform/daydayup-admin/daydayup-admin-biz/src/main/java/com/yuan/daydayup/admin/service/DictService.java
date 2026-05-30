package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.DictCreateDTO;
import com.yuan.daydayup.admin.dto.DictPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictStatusDTO;
import com.yuan.daydayup.admin.dto.DictUpdateDTO;
import com.yuan.daydayup.admin.vo.DictVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;
import com.yuan.daydayup.common.mybatis.service.DeletableCrudService;

public interface DictService extends BaseCrudService<Long, DictCreateDTO, DictUpdateDTO, DictVO, DictPageQueryDTO, DictStatusDTO>,
        DeletableCrudService<Long> {
}
