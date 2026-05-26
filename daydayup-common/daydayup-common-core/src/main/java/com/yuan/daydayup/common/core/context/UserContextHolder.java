package com.yuan.daydayup.common.core.context;

/**
 * 用户上下文 ThreadLocal 持有器
 *
 * <p>由 Web 层 Filter（{@code HeaderAuthenticationFilter}）在请求开始时写入，
 * 请求结束时由同一 Filter 调用 {@link #clear()} 释放，避免线程复用导致脏读。</p>
 */
public final class UserContextHolder {

    private UserContextHolder() {
    }

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.getUserId();
    }

    public static String getUsername() {
        UserContext context = CONTEXT.get();
        return context == null ? null : context.getUsername();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
