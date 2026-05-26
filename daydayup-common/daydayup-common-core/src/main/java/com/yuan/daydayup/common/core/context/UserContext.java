package com.yuan.daydayup.common.core.context;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户上下文
 *
 * <p>由网关解析 JWT 后写入下游请求头，
 * 业务服务通过 {@link UserContextHolder} 还原并访问。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 权限码集合 */
    private Set<String> authorities;
}
