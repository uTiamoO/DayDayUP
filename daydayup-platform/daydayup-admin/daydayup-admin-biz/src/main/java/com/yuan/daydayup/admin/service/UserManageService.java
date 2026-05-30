package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserPageQueryDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;

public interface UserManageService extends BaseCrudService<Long, UserCreateDTO, UserUpdateDTO, UserDetailVO, UserPageQueryDTO, UserStatusDTO> {
}
