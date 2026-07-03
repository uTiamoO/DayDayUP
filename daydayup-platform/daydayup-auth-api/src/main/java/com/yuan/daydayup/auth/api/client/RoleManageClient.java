package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.auth.api.dto.RoleCreateDTO;
import com.yuan.daydayup.auth.api.dto.RoleDTO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.RolePermissionAssignDTO;
import com.yuan.daydayup.auth.api.dto.RoleStatusDTO;
import com.yuan.daydayup.auth.api.dto.RoleUpdateDTO;
import com.yuan.daydayup.auth.api.vo.RoleVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "daydayup-auth", contextId = "roleManageClient",
             path = "/api/roles")
public interface RoleManageClient {
    @GetMapping("/page")
    R<PageResult<RoleVO>> page(@SpringQueryMap RolePageQuery query);

    @GetMapping("/{id}")
    R<RoleVO> detail(@PathVariable("id") Long id);

    @PostMapping
    R<RoleVO> create(@RequestBody RoleCreateDTO dto);

    @PutMapping("/{id}")
    R<RoleVO> update(@PathVariable("id") Long id, @RequestBody RoleUpdateDTO dto);

    @PatchMapping("/{id}/status")
    R<Void> changeStatus(@PathVariable("id") Long id, @RequestBody RoleStatusDTO dto);

    @GetMapping("/{id}/permissions")
    R<List<Long>> getPermissionIds(@PathVariable("id") Long id);

    @PutMapping("/{id}/permissions")
    R<Void> assignPermissions(@PathVariable("id") Long id, @RequestBody RolePermissionAssignDTO dto);

    @GetMapping("/by-ids")
    R<List<RoleDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
