package com.yuan.daydayup.common.feign.interceptor;

import com.yuan.daydayup.common.core.constant.CommonConstants;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.context.UserContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Feign 请求拦截器
 *
 * <p>透传规则（优先级从高到低）：</p>
 * <ol>
 *   <li>{@link UserContextHolder} 中的上下文（业务自己已登录的请求）</li>
 *   <li>当前请求头中的 {@code X-*} 头（兜底）</li>
 *   <li>当前请求头中的 {@code Authorization}（少数需要服务直接持有 token 的场景）</li>
 * </ol>
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        UserContext context = UserContextHolder.get();
        if (context != null) {
            putIfAbsent(template, CommonConstants.HEADER_USER_ID, valueOf(context.getUserId()));
            if (context.getUsername() != null) {
                putIfAbsent(template, CommonConstants.HEADER_USER_NAME,
                        URLEncoder.encode(context.getUsername(), StandardCharsets.UTF_8));
            }
            if (context.getAuthorities() != null && !context.getAuthorities().isEmpty()) {
                putIfAbsent(template, SecurityConstants.CLAIM_AUTHORITIES,
                        String.join(",", context.getAuthorities()));
            }
        }

        HttpServletRequest current = currentRequest();
        if (current != null) {
            copyHeader(template, current, CommonConstants.HEADER_TRACE_ID);
            copyHeader(template, current, CommonConstants.HEADER_USER_ID);
            copyHeader(template, current, CommonConstants.HEADER_USER_NAME);
            copyHeader(template, current, SecurityConstants.CLAIM_AUTHORITIES);
            copyHeader(template, current, HttpHeaders.AUTHORIZATION);
        }
    }

    private void copyHeader(RequestTemplate template, HttpServletRequest request, String name) {
        if (!template.headers().containsKey(name)) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                template.header(name, value);
            }
        }
    }

    private void putIfAbsent(RequestTemplate template, String name, String value) {
        if (value != null && !template.headers().containsKey(name)) {
            template.header(name, value);
        }
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
