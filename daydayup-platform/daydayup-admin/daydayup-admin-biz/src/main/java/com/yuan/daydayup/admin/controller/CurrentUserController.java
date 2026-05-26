package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.common.security.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户信息接口
 *
 * <p>用于端到端验证：</p>
 * <ol>
 *   <li>客户端登录拿 token</li>
 *   <li>带 token 调 /admin/me（网关路由后变 /me）</li>
 *   <li>网关校验 JWT → 写入 X-User-* 头</li>
 *   <li>本服务从头部还原 UserContext，返回</li>
 * </ol>
 */
@RestController
@RequestMapping("/me")
public class CurrentUserController {

    @GetMapping
    @PreAuthorize("hasAuthority('admin:*')")
    public R<UserContext> me() {
        return R.ok(SecurityUtils.requireUser());
    }
}
