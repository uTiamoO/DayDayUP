package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.common.core.page.PageResult;

import java.util.List;

public interface PermissionService {
    PageResult<PermissionVO> page(PermissionPageQuery query);

    PermissionVO detail(Long id);

    PermissionVO create(PermissionCreateDTO dto);

    PermissionVO update(Long id, PermissionUpdateDTO dto);

    void changeStatus(Long id, PermissionStatusDTO dto);

    List<PermissionVO> listAll();
}
