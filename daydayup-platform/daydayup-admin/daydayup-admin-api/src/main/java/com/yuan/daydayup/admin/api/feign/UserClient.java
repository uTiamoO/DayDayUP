package com.yuan.daydayup.admin.api.feign;

import com.yuan.daydayup.admin.api.vo.AuthUserVO;
import com.yuan.daydayup.admin.api.vo.UserVO;
import com.yuan.daydayup.common.core.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 后台管理服务 Feign 客户端
 */
@FeignClient(name = "daydayup-admin-biz", contextId = "adminClient", path = "/users")
public interface UserClient {

    /**
     * 按 ID 查询用户信息
     */
    @GetMapping("/{userId}")
    R<UserVO> getUserById(@PathVariable("userId") Long userId);

    /**
     * 按用户名查询认证信息（含密码哈希 + 权限，仅认证中心调用）
     */
    @GetMapping("/auth/{username}")
    R<AuthUserVO> getAuthUserByUsername(@PathVariable("username") String username);

    /**
     * 更新用户最后登录信息
     */
    @PatchMapping("/manage/{userId}/login-info")
    R<Void> updateLoginInfo(@PathVariable("userId") Long userId,
                            @RequestParam("lastLoginIp") String lastLoginIp);
}
