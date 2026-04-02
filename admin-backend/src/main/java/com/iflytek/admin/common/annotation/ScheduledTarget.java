package com.iflytek.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可被定时任务调度器反射调用的方法。
 * 未标注此注解的方法不允许被 JobRunner 调用，防止 RCE 攻击。
 *
 * <p><b>约束：</b>被标注的方法必须是无参方法（no-args）。
 * JobRunner 通过 {@code getMethod(methodName)} 进行反射查找，
 * 仅支持无参签名；带参方法将导致 {@link NoSuchMethodException}。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledTarget {
}
