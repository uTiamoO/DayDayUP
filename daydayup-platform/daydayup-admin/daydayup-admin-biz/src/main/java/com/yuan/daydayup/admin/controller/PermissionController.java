package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.service.PermissionService;
import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:permission:list')")
    public R<PageResult<PermissionVO>> page(PermissionPageQuery query) {
        return R.ok(permissionService.page(query));
    }

    @GetMapping("/all")
    @PreAuthorize("hasPermission(null, 'admin:permission:list')")
    public R<List<PermissionVO>> listAll() {
        return R.ok(permissionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:permission:detail')")
    public R<PermissionVO> detail(@PathVariable Long id) {
        return R.ok(permissionService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:permission:create')")
    public R<PermissionVO> create(@Valid @RequestBody PermissionCreateDTO dto) {
        return R.ok(permissionService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:permission:update')")
    public R<PermissionVO> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateDTO dto) {
        return R.ok(permissionService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:permission:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody PermissionStatusDTO dto) {
        permissionService.changeStatus(id, dto);
        return R.ok();
    }
}
