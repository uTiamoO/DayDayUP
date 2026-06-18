package com.yuan.daydayup.auth.controller;

import com.yuan.daydayup.auth.api.dto.UserCreateDTO;
import com.yuan.daydayup.auth.api.dto.PasswordResetDTO;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.auth.api.dto.UserStatusDTO;
import com.yuan.daydayup.auth.api.dto.UserUpdateDTO;
import com.yuan.daydayup.auth.api.vo.UserDetailVO;
import com.yuan.daydayup.auth.service.UserManageService;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 API（auth 模块，供 admin-biz 通过 Feign 调用）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping("/page")
    public R<PageResult<UserDetailVO>> page(UserPageQuery query) {
        return R.ok(userManageService.page(query));
    }

    @GetMapping("/{id}")
    public R<UserDetailVO> detail(@PathVariable Long id) {
        return R.ok(userManageService.detail(id));
    }

    @PostMapping
    public R<UserDetailVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userManageService.create(dto));
    }

    @PutMapping("/{id}")
    public R<UserDetailVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return R.ok(userManageService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userManageService.changeStatus(id, dto);
        return R.ok();
    }

    @PatchMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetDTO dto) {
        userManageService.resetPassword(id, dto.getNewPassword());
        return R.ok();
    }

    @PatchMapping("/{id}/login-info")
    public R<Void> updateLoginInfo(@PathVariable Long id,
                                   @RequestParam("lastLoginIp") String lastLoginIp) {
        userManageService.updateLoginInfo(id, lastLoginIp);
        return R.ok();
    }
}
