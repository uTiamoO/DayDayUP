package com.yuan.daydayup.common.security.filter;

import com.yuan.daydayup.common.core.constant.CommonConstants;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.context.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 网关头部透传认证过滤器
 *
 * <p>核心约定：</p>
 * <ul>
 *   <li>JWT 由网关统一校验，业务服务<b>不重复校验签名</b></li>
 *   <li>网关在校验通过后写入 {@code X-User-Id} 等请求头</li>
 *   <li>本过滤器读取这些头部 → 构造 {@link UserContext} 与 Spring Security 的 {@code Authentication}</li>
 * </ul>
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            String username = decode(request.getHeader(CommonConstants.HEADER_USER_NAME));
            String authoritiesHeader = request.getHeader(SecurityConstants.CLAIM_AUTHORITIES);

            Set<String> authorities = authoritiesHeader == null || authoritiesHeader.isBlank()
                    ? Set.of()
                    : Arrays.stream(authoritiesHeader.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());

            UserContext context = UserContext.builder()
                    .userId(Long.valueOf(userIdHeader))
                    .username(username)
                    .authorities(authorities)
                    .build();
            UserContextHolder.set(context);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    context, null,
                    authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String decode(String value) {
        if (value == null) {
            return null;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
