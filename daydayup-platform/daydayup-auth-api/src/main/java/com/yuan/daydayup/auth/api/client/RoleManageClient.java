package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.auth.api.dto.RoleDTO;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "daydayup-auth", contextId = "roleManageClient",
             path = "/api/roles")
public interface RoleManageClient {
    @GetMapping("/by-ids")
    R<List<RoleDTO>> listByIds(@RequestParam("ids") List<Long> ids);
}
