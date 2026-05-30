package com.yuan.daydayup.auth.user;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 简化用户模型（P1 阶段使用内存数据，P2 后改为查询 admin 服务）
 */
@Data
@Builder
public class SimpleUser {

    private Long userId;

    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    private Set<String> authorities;

    /** 状态：0=停用，1=启用 */
    private Integer status;
}
