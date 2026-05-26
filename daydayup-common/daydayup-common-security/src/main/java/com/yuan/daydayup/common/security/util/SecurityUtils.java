package com.yuan.daydayup.common.security.util;

import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.context.UserContextHolder;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;

/**
 * 安全工具类：业务代码获取当前登录用户的统一入口
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserContext getUser() {
        return UserContextHolder.get();
    }

    public static UserContext requireUser() {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }

    public static Long getUserId() {
        return UserContextHolder.getUserId();
    }

    public static String getUsername() {
        return UserContextHolder.getUsername();
    }
}
