package com.yuan.daydayup.auth.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 携带 userId 的 UserDetails 实现。
 *
 * <p>Spring 标准 {@link User} 不含用户主键 ID，导致签发的 JWT 无法携带 {@code uid} claim，
 * 进而网关无法透传 {@code X-User-Id}、下游无法构建用户上下文、权限版本校验也无从进行。
 * 本类补齐 userId，供 {@code AuthorizationServerConfig#jwtCustomizer} 注入 {@code uid} claim。</p>
 */
public class DayDayUpUser extends User {

    private final Long userId;

    public DayDayUpUser(Long userId,
                        String username,
                        String password,
                        boolean enabled,
                        Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
