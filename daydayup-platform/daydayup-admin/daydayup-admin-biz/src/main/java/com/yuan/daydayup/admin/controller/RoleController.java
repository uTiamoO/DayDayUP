package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.RoleCreateDTO;
import com.yuan.daydayup.admin.dto.RolePageQueryDTO;
import com.yuan.daydayup.admin.dto.RoleStatusDTO;
import com.yuan.daydayup.admin.dto.RoleUpdateDTO;
import com.yuan.daydayup.admin.service.RoleService;
import com.yuan.daydayup.admin.vo.RoleVO;
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

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:role:list')")
    public R<PageResult<RoleVO>> page(RolePageQueryDTO query) {
        return R.ok(roleService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:role:detail')")
    public R<RoleVO> detail(@PathVariable Long id) {
        return R.ok(roleService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:role:create')")
    public R<RoleVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:role:update')")
    public R<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        return R.ok(roleService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:role:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody RoleStatusDTO dto) {
        roleService.changeStatus(id, dto);
        return R.ok();
    }
}
