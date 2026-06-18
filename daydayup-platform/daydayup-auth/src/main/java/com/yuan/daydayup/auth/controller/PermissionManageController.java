package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.auth.service.PermissionManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限管理 API（auth 模块，供 admin-biz 通过 Feign 调用）
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionManageController {

    private final PermissionManageService permissionManageService;

    @GetMapping("/page")
    public R<PageResult<PermissionVO>> page(PermissionPageQuery query) {
        return R.ok(permissionManageService.page(query));
    }

    @GetMapping("/{id}")
    public R<PermissionVO> detail(@PathVariable Long id) {
        return R.ok(permissionManageService.detail(id));
    }

    @PostMapping
    public R<PermissionVO> create(@Valid @RequestBody PermissionCreateDTO dto) {
        return R.ok(permissionManageService.create(dto));
    }

    @PutMapping("/{id}")
    public R<PermissionVO> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateDTO dto) {
        return R.ok(permissionManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody PermissionStatusDTO dto) {
        permissionManageService.changeStatus(id, dto);
        return R.ok();
    }
}
