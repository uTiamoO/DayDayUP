package com.yuan.daydayup.common.core.constant;

/**
 * 安全相关常量：JWT 自定义 claim 名称、权限授权前缀等
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** Bearer Token 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** JWT 自定义 claim：用户 ID */
    public static final String CLAIM_USER_ID = "uid";

    /** JWT 自定义 claim：用户名 */
    public static final String CLAIM_USERNAME = "username";

    /** JWT 自定义 claim：权限列表 */
    public static final String CLAIM_AUTHORITIES = "authorities";

    /**
     * Token 黑名单 Redis key 前缀。
     *
     * <p>网关（{@code AuthGlobalFilter}）与认证中心（{@code TokenBlacklistService}）
     * 共用此前缀，必须保持一致，否则黑名单校验会静默失效。</p>
     */
    public static final String TOKEN_BLACKLIST_KEY_PREFIX = "daydayup:auth:token:blacklist:";

    /** OAuth2 客户端 ID（认证中心默认） */
    public static final String DEFAULT_CLIENT_ID = "daydayup-client";

    /** OAuth2 客户端密钥（认证中心默认，生产环境必须替换） */
    public static final String DEFAULT_CLIENT_SECRET = "daydayup-secret";
}
