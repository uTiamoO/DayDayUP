package com.yuan.daydayup.common.security.permission;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.Collection;

/**
 * 通配符权限匹配工具，同时实现 Spring Security 的 {@link PermissionEvaluator}
 *
 * <p>支持 {@code admin:*} 匹配 {@code admin:user:list} 等细粒度权限。</p>
 *
 * <p>使用方式：在 {@code @PreAuthorize} 注解中使用 {@code hasPermission} 表达式：</p>
 * <pre>
 *   &#64;PreAuthorize("hasPermission(null, 'admin:user:list')")
 * </pre>
 */
public class WildcardPermissionEvaluator implements PermissionEvaluator {

    // ========================= 静态工具方法 =========================

    /**
     * 检查给定的权限集合中是否包含满足 requiredAuthority 的权限
     *
     * @param authorities       用户拥有的权限集合
     * @param requiredAuthority 所需的权限（精确匹配或前缀匹配）
     * @return 是否满足
     */
    public static boolean hasAuthority(Collection<? extends GrantedAuthority> authorities,
                                       String requiredAuthority) {
        if (authorities == null || requiredAuthority == null) {
            return false;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> matches(authority, requiredAuthority));
    }

    private static boolean matches(String authority, String requiredAuthority) {
        if (requiredAuthority.equals(authority)) {
            return true;
        }
        if (!authority.endsWith(":*")) {
            return false;
        }
        String prefix = authority.substring(0, authority.length() - 1);
        return requiredAuthority.startsWith(prefix);
    }

    // ========================= PermissionEvaluator 实现 =========================

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return hasAuthority(authentication.getAuthorities(), permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }
        return hasAuthority(authentication.getAuthorities(), permission.toString());
    }
}
