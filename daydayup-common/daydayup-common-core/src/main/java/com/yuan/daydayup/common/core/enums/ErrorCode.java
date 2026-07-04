package com.yuan.daydayup.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码
 *
 * <p>编码规范：5 位整数</p>
 * <ul>
 *   <li>2xx：成功</li>
 *   <li>4xx：客户端错误</li>
 *   <li>5xx：服务端错误</li>
 *   <li>1xxxx：通用业务错误</li>
 *   <li>2xxxx：认证授权相关</li>
 *   <li>3xxxx：用户相关</li>
 *   <li>4xxxx：游戏业务</li>
 *   <li>5xxxx：社交业务</li>
 *   <li>6xxxx：阅读业务</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    BIZ_ERROR(10000, "业务异常"),
    PARAM_VALIDATE_FAIL(10001, "参数校验失败"),
    DATA_NOT_FOUND(10002, "数据不存在"),
    DATA_ALREADY_EXISTS(10003, "数据已存在"),
    REPEAT_SUBMIT(10004, "重复提交，请稍后再试"),
    LOCK_ACQUIRE_FAIL(10005, "获取分布式锁失败"),

    TOKEN_INVALID(20001, "令牌无效"),
    TOKEN_EXPIRED(20002, "令牌已过期"),
    LOGIN_FAILED(20003, "用户名或密码错误"),
    ACCOUNT_DISABLED(20004, "账号已被禁用"),
    LOGIN_LOCKED(20005, "登录已被锁定，请稍后再试"),
    TOKEN_BLACKLISTED(20006, "令牌已失效"),

    READING_WORK_NOT_FOUND(60001, "作品不存在"),
    READING_CHAPTER_NOT_FOUND(60002, "章节不存在"),
    READING_SOURCE_NOT_AVAILABLE(60003, "无可用书源"),
    READING_UPSTREAM_FETCH_FAILED(60101, "源站抓取失败"),
    READING_UPSTREAM_TIMEOUT(60102, "源站抓取超时"),
    READING_UPSTREAM_BLOCKED(60103, "目标被安全策略拦截"),
    READING_RULE_COMPILE_FAILED(60201, "书源编译失败"),
    READING_RULE_RUNTIME_FAILED(60202, "书源规则运行时执行失败"),
    READING_CONTENT_EMPTY(60301, "正文为空"),
    READING_CONTENT_SANITIZATION_FAILED(60302, "正文净化失败"),
    READING_CONTENT_QUALITY_LOW(60303, "正文质量过低"),
    READING_INVALID_ARGUMENT(60401, "阅读请求参数非法"),
    READING_UNSUPPORTED_MODE(60402, "不支持的调用模式");

    private final Integer code;
    private final String message;
}
