package com.yuan.daydayup.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 角色授权：全量设置角色拥有的权限。 */
@Data
public class RolePermissionAssignDTO {

    /** 权限 ID 列表；传空列表表示清空该角色的全部权限。 */
    @NotNull(message = "权限 ID 列表不能为空")
    private List<Long> permissionIds;
}
