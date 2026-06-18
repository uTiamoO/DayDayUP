package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.common.core.page.PageResult;

public interface RoleService {
    PageResult<RoleVO> page(RolePageQuery query);

    RoleVO detail(Long id);

    RoleVO create(RoleCreateDTO dto);

    RoleVO update(Long id, RoleUpdateDTO dto);

    void changeStatus(Long id, RoleStatusDTO dto);
}
