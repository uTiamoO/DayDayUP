package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 物理删除某角色的全部权限关联。
     *
     * <p>关联表唯一键为 (role_id, permission_id) 且不含 deleted；若走 BaseEntity 的逻辑删除，
     * 软删行仍占用唯一键，重新授权插入同一 (role_id, permission_id) 会触发唯一键冲突，
     * 故此处直接物理删除。</p>
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") Long roleId);
}
