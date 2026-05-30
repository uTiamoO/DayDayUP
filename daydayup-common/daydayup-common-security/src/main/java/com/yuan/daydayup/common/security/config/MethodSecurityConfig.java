package com.yuan.daydayup.common.security.config;

import com.yuan.daydayup.common.security.permission.WildcardPermissionEvaluator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * 方法安全表达式配置
 *
 * <p>注册 {@link WildcardPermissionEvaluator}，使 {@code hasPermission} 表达式支持通配符匹配：</p>
 * <ul>
 *   <li>{@code admin:*} 可匹配 {@code admin:user:list}、{@code admin:role:add} 等</li>
 *   <li>精确权限照常匹配</li>
 * </ul>
 *
 * <p>用法示例：</p>
 * <pre>
 *   &#64;PreAuthorize("hasPermission(null, 'admin:user:list')")
 *   public void someMethod() { ... }
 * </pre>
 *
 * <p>注：Spring Security 6.x 中 {@code MethodSecurityExpressionRoot} 已改为包级私有，
 * 无法通过 {@code createSecurityExpressionRoot} 覆盖 {@code hasAuthority}。
 * 因此使用 {@code PermissionEvaluator} + {@code hasPermission} 表达式方案。</p>
 */
@Configuration(proxyBeanMethods = false)
public class MethodSecurityConfig {

    @Bean
    @ConditionalOnMissingBean(WildcardPermissionEvaluator.class)
    public WildcardPermissionEvaluator wildcardPermissionEvaluator() {
        return new WildcardPermissionEvaluator();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            WildcardPermissionEvaluator wildcardPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(wildcardPermissionEvaluator);
        return handler;
    }
}
