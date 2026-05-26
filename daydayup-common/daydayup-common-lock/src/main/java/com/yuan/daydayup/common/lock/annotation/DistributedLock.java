package com.yuan.daydayup.common.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁
 *
 * <p>用法：</p>
 * <pre>{@code
 * @DistributedLock(key = "'order:' + #orderId", waitTime = 3, leaseTime = 10)
 * public void payOrder(Long orderId) { ... }
 * }</pre>
 *
 * <p>语义：</p>
 * <ul>
 *   <li>等待 {@code waitTime} 内拿不到锁 → 抛 {@code LOCK_ACQUIRE_FAIL}</li>
 *   <li>拿到锁后，方法执行完或 {@code leaseTime} 到期自动释放</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /** SpEL 表达式，解析锁 Key */
    String key();

    /** 等待时间，默认 3 秒 */
    long waitTime() default 3;

    /** 持有时间，默认 30 秒；-1 表示 Redisson 看门狗自动续期 */
    long leaseTime() default 30;

    TimeUnit unit() default TimeUnit.SECONDS;
}
