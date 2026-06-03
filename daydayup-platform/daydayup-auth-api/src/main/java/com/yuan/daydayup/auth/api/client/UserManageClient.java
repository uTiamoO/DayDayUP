package com.yuan.daydayup.auth.api.client;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "daydayup-auth", contextId = "userManageClient",
             path = "/api/users")
public interface UserManageClient {
    @GetMapping("/page")
    R<PageResult<UserDetailVO>> page(UserPageQuery query);

    @GetMapping("/{id}")
    R<UserDetailVO> detail(@PathVariable("id") Long id);

    @PostMapping
    R<UserDetailVO> create(@RequestBody UserCreateDTO dto);

    @PutMapping("/{id}")
    R<UserDetailVO> update(@PathVariable("id") Long id, @RequestBody UserUpdateDTO dto);

    @PatchMapping("/{id}/status")
    R<Void> changeStatus(@PathVariable("id") Long id, @RequestBody UserStatusDTO dto);

    @PatchMapping("/{id}/login-info")
    R<Void> updateLoginInfo(@PathVariable("id") Long id, @RequestParam("lastLoginIp") String lastLoginIp);
}
