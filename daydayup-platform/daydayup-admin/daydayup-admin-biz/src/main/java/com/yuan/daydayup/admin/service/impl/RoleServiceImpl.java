package com.yuan.daydayup.admin.service.impl;

import com.yuan.daydayup.admin.service.RoleService;
import com.yuan.daydayup.auth.api.client.RoleManageClient;
import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.RolePermissionAssignDTO;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleManageClient roleManageClient;

    @Override
    public PageResult<RoleVO> page(RolePageQuery query) {
        return roleManageClient.page(query).getData();
    }

    @Override
    public RoleVO detail(Long id) {
        return roleManageClient.detail(id).getData();
    }

    @Override
    public RoleVO create(RoleCreateDTO dto) {
        return roleManageClient.create(dto).getData();
    }

    @Override
    public RoleVO update(Long id, RoleUpdateDTO dto) {
        return roleManageClient.update(id, dto).getData();
    }

    @Override
    public void changeStatus(Long id, RoleStatusDTO dto) {
        roleManageClient.changeStatus(id, dto);
    }

    @Override
    public List<Long> getPermissionIds(Long id) {
        return roleManageClient.getPermissionIds(id).getData();
    }

    @Override
    public void assignPermissions(Long id, RolePermissionAssignDTO dto) {
        roleManageClient.assignPermissions(id, dto);
    }
}
