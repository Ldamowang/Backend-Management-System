package com.iflytek.admin.common.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * XSS 过滤器。
 * 对所有 HTTP 请求参数进行 HTML 转义，防止 XSS 攻击。
 * 支持白名单路径排除（如富文本编辑器接口）。
 */
@Slf4j
public class XssFilter implements Filter {

    /**
     * 白名单路径列表，这些路径不进行 XSS 过滤。
     * 当前项目暂无富文本编辑器接口，预留扩展点。
     */
    private final List<String> excludePaths;

    public XssFilter(List<String> excludePaths) {
        this.excludePaths = excludePaths != null ? excludePaths : List.of();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 白名单路径跳过 XSS 过滤
        if (isExcludePath(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        chain.doFilter(new XssHttpServletRequestWrapper(httpRequest), response);
    }

    /**
     * 判断请求路径是否在白名单中。
     */
    private boolean isExcludePath(String requestUri) {
        return excludePaths.stream().anyMatch(requestUri::startsWith);
    }
}
