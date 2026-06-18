package com.yuan.daydayup.admin.service.impl;

import com.yuan.daydayup.admin.service.PermissionService;
import com.yuan.daydayup.auth.api.client.PermissionManageClient;
import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionManageClient permissionManageClient;

    @Override
    public PageResult<PermissionVO> page(PermissionPageQuery query) {
        return permissionManageClient.page(query).getData();
    }

    @Override
    public PermissionVO detail(Long id) {
        return permissionManageClient.detail(id).getData();
    }

    @Override
    public PermissionVO create(PermissionCreateDTO dto) {
        return permissionManageClient.create(dto).getData();
    }

    @Override
    public PermissionVO update(Long id, PermissionUpdateDTO dto) {
        return permissionManageClient.update(id, dto).getData();
    }

    @Override
    public void changeStatus(Long id, PermissionStatusDTO dto) {
        permissionManageClient.changeStatus(id, dto);
    }

    @Override
    public List<PermissionVO> listAll() {
        return permissionManageClient.listAll().getData();
    }
}
