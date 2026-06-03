package com.yuan.daydayup.auth.grant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 从 {@code POST /oauth2/token} 请求中识别 {@code grant_type=password}，
 * 提取 username / password / scope 并构造 {@link PasswordAuthenticationToken}。
 *
 * <p>SAS 的 token 端点支持链式 {@link AuthenticationConverter}，
 * 本 converter 只在 {@code grant_type=password} 时返回 token，其余情况返回 null
 * 交给下一个 converter（如 SAS 内置的 authorization_code / client_credentials）。</p>
 */
public class PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"password".equals(grantType)) {
            return null;
        }

        // 客户端必须已通过 client authentication（Basic Auth 或 client_secret_post）
        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        if (clientPrincipal == null) {
            return null;
        }

        MultiValueMap<String, String> parameters = getParameters(request);

        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username)) {
            return null;
        }

        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        if (!StringUtils.hasText(password)) {
            return null;
        }

        Set<String> scopes = new HashSet<>();
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope)) {
            for (String s : scope.split(" ")) {
                if (StringUtils.hasText(s)) {
                    scopes.add(s);
                }
            }
        }

        return new PasswordAuthenticationToken(username, password, clientPrincipal, scopes);
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            for (String value : values) {
                parameters.add(key, value);
            }
        });
        return parameters;
    }
}
