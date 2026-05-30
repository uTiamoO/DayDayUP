package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.MenuCreateDTO;
import com.yuan.daydayup.admin.dto.MenuPageQueryDTO;
import com.yuan.daydayup.admin.dto.MenuStatusDTO;
import com.yuan.daydayup.admin.dto.MenuUpdateDTO;
import com.yuan.daydayup.admin.vo.MenuVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;
import com.yuan.daydayup.common.mybatis.service.DeletableCrudService;

public interface MenuService extends BaseCrudService<Long, MenuCreateDTO, MenuUpdateDTO, MenuVO, MenuPageQueryDTO, MenuStatusDTO>,
        DeletableCrudService<Long> {
}
