package com.iflytek.admin.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("XssFilter XSS过滤器测试")
class XssFilterTest {

    @Test
    @DisplayName("普通请求 - 使用XssHttpServletRequestWrapper包装")
    void doFilter_normalRequest() throws IOException, ServletException {
        XssFilter filter = new XssFilter(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    @DisplayName("白名单路径 - 跳过XSS过滤")
    void doFilter_excludePath() throws IOException, ServletException {
        XssFilter filter = new XssFilter(List.of("/api/editor/"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/editor/save");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // For excluded path, original request should be passed (not wrapped)
        verify(chain).doFilter(eq(request), eq(response));
    }

    @Test
    @DisplayName("多个白名单路径 - 匹配任一则跳过")
    void doFilter_multipleExcludePaths() throws IOException, ServletException {
        XssFilter filter = new XssFilter(List.of("/api/editor/", "/api/upload/"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/upload/image");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(eq(request), eq(response));
    }

    @Test
    @DisplayName("null白名单 - 初始化为空列表")
    void doFilter_nullExcludePaths() throws IOException, ServletException {
        XssFilter filter = new XssFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    @DisplayName("非白名单路径 - 正常过滤")
    void doFilter_nonExcludePath() throws IOException, ServletException {
        XssFilter filter = new XssFilter(List.of("/api/editor/"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(XssHttpServletRequestWrapper.class), eq(response));
    }
}
