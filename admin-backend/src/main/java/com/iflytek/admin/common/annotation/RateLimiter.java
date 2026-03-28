package com.iflytek.admin.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解。
 * 基于 Redis 实现滑动窗口限流，限流 key 策略为 IP + 接口路径。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * 时间窗口内允许的最大请求次数。
     */
    int count() default 10;

    /**
     * 时间窗口大小（秒）。
     */
    int time() default 60;

    /**
     * 限流提示消息。
     */
    String message() default "请求过于频繁，请稍后再试";
}
