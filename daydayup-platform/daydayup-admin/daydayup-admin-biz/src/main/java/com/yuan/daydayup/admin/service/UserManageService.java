package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.common.core.page.PageResult;

/**
 * 用户管理服务接口（admin-biz 层，委托给 auth 服务）
 */
public interface UserManageService {

    PageResult<UserDetailVO> page(UserPageQuery query);

    UserDetailVO detail(Long id);

    UserDetailVO create(UserCreateDTO dto);

    UserDetailVO update(Long id, UserUpdateDTO dto);

    void changeStatus(Long id, UserStatusDTO dto);

    void updateLoginInfo(Long userId, String lastLoginIp);
}
