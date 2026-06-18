package com.yuan.daydayup.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.auth.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 物理删除某用户的全部角色关联。
     *
     * <p>关联表唯一键为 (user_id, role_id) 且不含 deleted；走 BaseEntity 的逻辑删除时，
     * 软删行仍占用唯一键，重新分配同一 (user_id, role_id) 会触发唯一键冲突，故物理删除。</p>
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}
