package com.iflytek.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可被定时任务调度器反射调用的方法。
 * 未标注此注解的方法不允许被 JobRunner 调用，防止 RCE 攻击。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledTarget {
}
