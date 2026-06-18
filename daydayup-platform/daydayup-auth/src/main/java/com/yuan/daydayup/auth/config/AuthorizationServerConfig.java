package com.yuan.daydayup.auth.config;

import com.yuan.daydayup.auth.grant.PasswordAuthenticationConverter;
import com.yuan.daydayup.auth.grant.PasswordAuthenticationProvider;
import com.yuan.daydayup.auth.service.DayDayUpUser;
import com.yuan.daydayup.common.core.constant.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Authorization Server 核心配置。
 *
 * <p>注册 SAS 协议端点 SecurityFilterChain（Order=HIGHEST_PRECEDENCE）、
 * JdbcRegisteredClientRepository、JdbcOAuth2AuthorizationService 等基础设施 Bean，
 * 以及自定义 password grant 的 Converter / Provider。</p>
 *
 * <p>JWK 相关 Bean（RSAKey、JWKSource、JwtEncoder、JwtDecoder）由 {@link JwkConfig} 提供。</p>
 */
@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

    // ==================== SAS 协议端点 SecurityFilterChain ====================

    /**
     * SAS 协议端点 SecurityFilterChain（Order=HIGHEST_PRECEDENCE）。
     *
     * <p>拦截 {@code /oauth2/authorize}、{@code /oauth2/token}、{@code /.well-known/*}、
     * {@code /oauth2/jwks} 等 SAS 标准端点。未认证时跳转到可配置的 OIDC 登录入口；
     * auth 服务自身不再渲染登录页面。</p>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
            @Value("${daydayup.auth.oidc-login-url:/api/session/login-required}") String oidcLoginUrl) throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                        .accessTokenRequestConverter(new PasswordAuthenticationConverter())
                        .authenticationProvider(new PasswordAuthenticationProvider(
                                userDetailsService, passwordEncoder, authorizationService, tokenGenerator)))
                .oidc(Customizer.withDefaults());

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(oidcLoginUrl)));

        return http.build();
    }

    // ==================== JDBC 持久化 Bean ====================

    @Bean
    public JdbcRegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public JdbcOAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            JdbcRegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JdbcOAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            JdbcRegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    // ==================== Token 生成 ====================

    /**
     * 向 access_token 的 JWT payload 注入 {@code authorities} claim。
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();
                if (principal != null
                        && principal.getPrincipal() instanceof User userDetails) {
                    Collection<String> authorities = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());
                    context.getClaims().claim(SecurityConstants.CLAIM_AUTHORITIES, authorities);
                    // 注入 uid claim：网关据此透传 X-User-Id，并做权限版本校验
                    if (userDetails instanceof DayDayUpUser ddu && ddu.getUserId() != null) {
                        context.getClaims().claim(SecurityConstants.CLAIM_USER_ID, ddu.getUserId());
                    }
                }
            }
        };
    }

    /**
     * OAuth2TokenGenerator：JWT（access_token）+ RefreshToken。
     *
     * <p>SAS 自动配置可能不会创建此 Bean，因此手动注册。</p>
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(
            JwtEncoder jwtEncoder,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(jwtCustomizer);
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
    }

    // ==================== Authorization Server Settings ====================

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${daydayup.auth.issuer-url:http://127.0.0.1:9200}") String issuerUrl) {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUrl)
                .build();
    }
}
