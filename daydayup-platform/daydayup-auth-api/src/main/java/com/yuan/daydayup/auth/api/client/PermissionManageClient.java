package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.auth.api.dto.PermissionCreateDTO;
import com.yuan.daydayup.auth.api.dto.PermissionPageQuery;
import com.yuan.daydayup.auth.api.dto.PermissionStatusDTO;
import com.yuan.daydayup.auth.api.dto.PermissionUpdateDTO;
import com.yuan.daydayup.auth.api.vo.PermissionVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "daydayup-auth", contextId = "permissionManageClient",
             path = "/api/permissions")
public interface PermissionManageClient {
    @GetMapping("/page")
    R<PageResult<PermissionVO>> page(PermissionPageQuery query);

    @GetMapping("/{id}")
    R<PermissionVO> detail(@PathVariable("id") Long id);

    @PostMapping
    R<PermissionVO> create(@RequestBody PermissionCreateDTO dto);

    @PutMapping("/{id}")
    R<PermissionVO> update(@PathVariable("id") Long id, @RequestBody PermissionUpdateDTO dto);

    @PatchMapping("/{id}/status")
    R<Void> changeStatus(@PathVariable("id") Long id, @RequestBody PermissionStatusDTO dto);
}
