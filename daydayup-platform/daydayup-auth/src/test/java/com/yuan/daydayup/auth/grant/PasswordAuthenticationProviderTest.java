package com.yuan.daydayup.auth.grant;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PasswordAuthenticationProviderTest {

    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final OAuth2AuthorizationService authorizationService = mock(OAuth2AuthorizationService.class);
    private final OAuth2TokenGenerator<?> tokenGenerator = mock(OAuth2TokenGenerator.class);

    private final PasswordAuthenticationProvider provider = new PasswordAuthenticationProvider(
            userDetailsService, passwordEncoder, authorizationService, tokenGenerator);

    @Test
    void authenticateRejectsScopesOutsideRegisteredClient() {
        RegisteredClient registeredClient = RegisteredClient.withId("client-1")
                .clientId("legacy-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(PasswordAuthenticationToken.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope("openid")
                .scope("profile")
                .build();
        OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                registeredClient, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
        PasswordAuthenticationToken authentication = new PasswordAuthenticationToken(
                "admin", "password", clientPrincipal, Set.of("openid", "admin:*"));

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(User.withUsername("admin")
                .password("encoded-password")
                .authorities("admin:*")
                .build());
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                () -> provider.authenticate(authentication));

        assertEquals(OAuth2ErrorCodes.INVALID_SCOPE, exception.getError().getErrorCode());
        verifyNoInteractions(tokenGenerator, authorizationService);
    }
}
