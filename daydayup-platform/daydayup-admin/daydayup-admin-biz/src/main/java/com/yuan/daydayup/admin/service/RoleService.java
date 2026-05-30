package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.RoleCreateDTO;
import com.yuan.daydayup.admin.dto.RolePageQueryDTO;
import com.yuan.daydayup.admin.dto.RoleStatusDTO;
import com.yuan.daydayup.admin.dto.RoleUpdateDTO;
import com.yuan.daydayup.admin.vo.RoleVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;

public interface RoleService extends BaseCrudService<Long, RoleCreateDTO, RoleUpdateDTO, RoleVO, RolePageQueryDTO, RoleStatusDTO> {
}
