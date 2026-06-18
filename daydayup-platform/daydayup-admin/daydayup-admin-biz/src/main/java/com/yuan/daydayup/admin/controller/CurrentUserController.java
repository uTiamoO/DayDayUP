package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.service.MenuService;
import com.yuan.daydayup.admin.vo.MenuTreeVO;
import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户信息接口。
 *
 * <p>任意登录用户均可访问：返回自身的身份与权限码（{@code /me}），
 * 以及按权限过滤后的动态菜单树（{@code /me/menus}）。</p>
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final MenuService menuService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<UserContext> me() {
        return R.ok(SecurityUtils.requireUser());
    }

    @GetMapping("/menus")
    @PreAuthorize("isAuthenticated()")
    public R<List<MenuTreeVO>> myMenus() {
        return R.ok(menuService.currentUserMenus(SecurityUtils.requireUser().getAuthorities()));
    }
}
