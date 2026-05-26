package com.yuan.daydayup.common.core.constant;

/**
 * 系统通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 成功标识 */
    public static final Integer SUCCESS = 200;

    /** 失败标识 */
    public static final Integer FAIL = 500;

    /** UTF-8 字符集 */
    public static final String UTF8 = "UTF-8";

    /** 默认页码 */
    public static final Integer DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数 */
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /** 请求头：traceId 透传 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 请求头：用户 ID 透传（网关解析 JWT 后写入） */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 请求头：用户名透传 */
    public static final String HEADER_USER_NAME = "X-User-Name";

}
