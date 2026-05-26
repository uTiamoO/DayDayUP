package com.yuan.daydayup.common.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等
 *
 * <p>用法：</p>
 * <pre>{@code
 * @Idempotent(key = "'order:' + #dto.orderNo", expire = 5)
 * public R<Void> create(@RequestBody OrderDTO dto) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** SpEL 表达式，从方法入参中解析幂等键；为空时使用 "类#方法+参数指纹" */
    String key() default "";

    /** 过期时间，超过此时间允许再次提交 */
    long expire() default 5;

    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /** 提交重复时的错误提示 */
    String message() default "请勿重复提交";
}
