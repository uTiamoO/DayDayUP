package com.yuan.daydayup.admin.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 认证专用用户 VO（含密码哈希 + 权限列表，仅内部 Feign 调用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证用户信息（内部）")
public class AuthUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "BCrypt 密码哈希")
    private String password;

    @Schema(description = "状态：0=停用，1=启用")
    private Integer status;

    @Schema(description = "权限码集合")
    private Set<String> authorities;
}
