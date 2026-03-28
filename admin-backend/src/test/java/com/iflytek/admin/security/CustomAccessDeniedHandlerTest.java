package com.iflytek.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomAccessDeniedHandler 测试")
class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();

    @Test
    @DisplayName("无权限请求 - 返回403和JSON错误消息")
    void handle_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");

        ObjectMapper mapper = new ObjectMapper();
        Result<?> result = mapper.readValue(response.getContentAsByteArray(), Result.class);
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("无权限访问");
    }
}
