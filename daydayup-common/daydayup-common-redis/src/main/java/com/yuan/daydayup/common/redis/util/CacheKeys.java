package com.yuan.daydayup.common.redis.util;

/**
 * 缓存 Key 统一前缀工具
 *
 * <p>规范：{@code daydayup:<模块>:<业务>:<标识>}，避免不同业务键名冲突。</p>
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static final String PREFIX = "daydayup:";

    public static final String SEP = ":";

    public static String of(String module, String biz, Object id) {
        return PREFIX + module + SEP + biz + SEP + id;
    }

    public static String of(String module, String biz) {
        return PREFIX + module + SEP + biz;
    }
}
