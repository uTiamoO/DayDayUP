package com.yuan.daydayup.common.log.annotation;

import com.yuan.daydayup.common.log.enums.BusinessType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 *
 * <p>标在 Controller 方法或类上，由 {@code OperLogAspect} 自动采集。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 操作标题，如 "新增用户" */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 是否记录请求参数（默认记录） */
    boolean recordParams() default true;

    /** 是否记录响应结果（默认不记录，避免大对象） */
    boolean recordResult() default false;
}
