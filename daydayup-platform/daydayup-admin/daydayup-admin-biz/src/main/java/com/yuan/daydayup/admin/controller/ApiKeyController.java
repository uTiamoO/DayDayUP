package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.service.ApiKeyService;
import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.common.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API Key 管理接口
 *
 * <p>用于创建和管理服务间调用 / 定时任务使用的 API Key。
 * API Key 关联一个用户身份，拥有该用户的权限。</p>
 */
@RestController
@RequestMapping("/api-keys")
@Tag(name = "API Key 管理", description = "服务间调用认证用 API Key 的创建与吊销")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "创建 API Key", description = "为当前用户创建一个 API Key，关联其全部权限")
    @PreAuthorize("hasPermission(null, 'admin:apikey:create')")
    public R<Map<String, String>> create() {
        var user = SecurityUtils.requireUser();
        String apiKey = apiKeyService.createApiKey(
                user.getUserId(),
                user.getUsername(),
                user.getAuthorities() != null ? List.copyOf(user.getAuthorities()) : List.of()
        );
        // 仅本次返回明文 API Key
        return R.ok(Map.of("apiKey", apiKey));
    }

    @DeleteMapping("/{apiKey}")
    @Operation(summary = "吊销 API Key")
    @PreAuthorize("hasPermission(null, 'admin:apikey:revoke')")
    public R<Void> revoke(@PathVariable String apiKey) {
        boolean revoked = apiKeyService.revokeApiKey(apiKey);
        return revoked ? R.ok() : R.fail(400, "API Key 不存在或已吊销");
    }
}
