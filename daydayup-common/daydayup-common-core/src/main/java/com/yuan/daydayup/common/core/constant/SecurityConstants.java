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

    /**
     * 用户权限降级时间戳 Redis key 前缀。
     *
     * <p>auth 在用户停用/降权时写入降级时间（epoch 秒），网关（{@code AuthGlobalFilter}）
     * 校验 JWT 的 iat 是否早于该时间，实现「降级即时生效」。两端必须共用此前缀。</p>
     */
    public static final String PERM_CHANGED_KEY_PREFIX = "daydayup:auth:perm-changed:";
}
