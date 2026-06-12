package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RoleDTO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.auth.service.RoleManageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色查询 API（auth 模块，供 admin-biz 通过 Feign 调用）
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleManageController {

    private final RoleManageService roleManageService;
    private final SysRoleMapper roleMapper;

    @GetMapping("/page")
    public R<PageResult<RoleVO>> page(RolePageQuery query) {
        return R.ok(roleManageService.page(query));
    }

    @GetMapping("/{id}")
    public R<RoleVO> detail(@PathVariable Long id) {
        return R.ok(roleManageService.detail(id));
    }

    @PostMapping
    public R<RoleVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(roleManageService.create(dto));
    }

    @PutMapping("/{id}")
    public R<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        return R.ok(roleManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody RoleStatusDTO dto) {
        roleManageService.changeStatus(id, dto);
        return R.ok();
    }

    @GetMapping("/by-ids")
    public R<List<RoleDTO>> listByIds(@RequestParam List<Long> ids) {
        List<SysRole> roles = roleMapper.selectBatchIds(ids);
        List<RoleDTO> dtos = roles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return R.ok(dtos);
    }

    private RoleDTO toDTO(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setStatus(role.getStatus());
        return dto;
    }
}
