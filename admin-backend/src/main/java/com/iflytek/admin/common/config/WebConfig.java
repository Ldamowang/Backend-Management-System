package com.iflytek.admin.common.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 全局 WebDataBinder 配置。
 * 将请求参数中的空字符串自动转为 null，
 * 避免 @Email、@Pattern 等校验注解在可选字段上误报。
 */
@ControllerAdvice
public class WebConfig {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
