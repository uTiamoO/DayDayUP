package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.RoleDTO;
import com.yuan.daydayup.auth.entity.SysRole;
import com.yuan.daydayup.auth.mapper.SysRoleMapper;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final SysRoleMapper roleMapper;

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
