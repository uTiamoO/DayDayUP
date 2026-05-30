package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.DictItemCreateDTO;
import com.yuan.daydayup.admin.dto.DictItemPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictItemStatusDTO;
import com.yuan.daydayup.admin.dto.DictItemUpdateDTO;
import com.yuan.daydayup.admin.vo.DictItemVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;
import com.yuan.daydayup.common.mybatis.service.DeletableCrudService;

public interface DictItemService extends BaseCrudService<Long, DictItemCreateDTO, DictItemUpdateDTO, DictItemVO, DictItemPageQueryDTO, DictItemStatusDTO>,
        DeletableCrudService<Long> {
}
