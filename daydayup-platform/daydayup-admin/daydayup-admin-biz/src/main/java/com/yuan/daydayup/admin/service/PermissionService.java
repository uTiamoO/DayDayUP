package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.PermissionCreateDTO;
import com.yuan.daydayup.admin.dto.PermissionPageQueryDTO;
import com.yuan.daydayup.admin.dto.PermissionStatusDTO;
import com.yuan.daydayup.admin.dto.PermissionUpdateDTO;
import com.yuan.daydayup.admin.vo.PermissionVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;

public interface PermissionService extends BaseCrudService<Long, PermissionCreateDTO, PermissionUpdateDTO, PermissionVO, PermissionPageQueryDTO, PermissionStatusDTO> {
}
