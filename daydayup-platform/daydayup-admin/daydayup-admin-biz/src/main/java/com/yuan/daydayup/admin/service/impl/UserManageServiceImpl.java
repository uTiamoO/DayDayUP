package com.yuan.daydayup.admin.service.impl;

import com.yuan.daydayup.auth.api.client.UserManageClient;
import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户管理服务实现（Feign 代理，委托给 auth 服务）
 */
@Service
@RequiredArgsConstructor
public class UserManageServiceImpl implements UserManageService {

    private final UserManageClient userManageClient;

    @Override
    public PageResult<UserDetailVO> page(UserPageQuery query) {
        return userManageClient.page(query).getData();
    }

    @Override
    public UserDetailVO detail(Long id) {
        return userManageClient.detail(id).getData();
    }

    @Override
    public UserDetailVO create(UserCreateDTO dto) {
        return userManageClient.create(dto).getData();
    }

    @Override
    public UserDetailVO update(Long id, UserUpdateDTO dto) {
        return userManageClient.update(id, dto).getData();
    }

    @Override
    public void changeStatus(Long id, UserStatusDTO dto) {
        userManageClient.changeStatus(id, dto);
    }

    @Override
    public void updateLoginInfo(Long userId, String lastLoginIp) {
        userManageClient.updateLoginInfo(userId, lastLoginIp);
    }
}
