package com.yuan.daydayup.auth.grant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Set;

/**
 * password grant 自定义认证 token。
 *
 * <p>携带 username / password，由 {@link PasswordAuthenticationConverter} 构造，
 * 交给 {@link PasswordAuthenticationProvider} 处理。</p>
 */
public class PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    public static final AuthorizationGrantType PASSWORD =
            new AuthorizationGrantType("password");

    private final String username;
    private final String password;
    private final Set<String> scopes;

    public PasswordAuthenticationToken(String username,
                                       String password,
                                       Authentication clientPrincipal,
                                       Set<String> scopes) {
        super(PASSWORD, clientPrincipal, null);
        this.username = username;
        this.password = password;
        this.scopes = scopes;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getScopes() {
        return scopes;
    }
}
