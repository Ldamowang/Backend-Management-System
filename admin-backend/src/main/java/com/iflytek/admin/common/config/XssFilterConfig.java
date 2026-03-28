package com.iflytek.admin.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * XSS 过滤器配置。
 * 注册 XssFilter 并设置优先级，确保在 Spring Security Filter 之后执行。
 * Spring Security 的 FilterChainProxy 默认 order 为 -100，
 * 这里设置为 1 以保证在安全过滤之后执行。
 */
@Configuration
public class XssFilterConfig {

    /**
     * XSS 过滤器白名单路径。
     * 当前项目暂无富文本编辑器等需要排除的路径，预留扩展。
     */
    private static final List<String> EXCLUDE_PATHS = List.of();

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter(EXCLUDE_PATHS));
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        // Spring Security FilterChainProxy 的 order 为 -100
        // 设置为 1 确保在 Spring Security 之后执行
        registration.setOrder(1);
        return registration;
    }
}
