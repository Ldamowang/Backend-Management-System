package com.iflytek.admin.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Exportable {
    /** 列标题 */
    String value();

    /** 列序号（越小越靠前） */
    int order() default 0;

    /** 是否参与导入 */
    boolean importable() default true;

    /** 导入时是否必填 */
    boolean required() default false;
}
