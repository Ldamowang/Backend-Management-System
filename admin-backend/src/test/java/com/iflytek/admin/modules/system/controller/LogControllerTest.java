package com.iflytek.admin.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.entity.SysLoginLog;
import com.iflytek.admin.modules.system.entity.SysOperationLog;
import com.iflytek.admin.modules.system.mapper.SysLoginLogMapper;
import com.iflytek.admin.modules.system.mapper.SysOperationLogMapper;
import com.iflytek.admin.security.CustomAccessDeniedHandler;
import com.iflytek.admin.security.CustomUserDetailsService;
import com.iflytek.admin.security.JwtAuthenticationEntryPoint;
import com.iflytek.admin.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = LogController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("LogController 日志管理接口测试")
class LogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SysOperationLogMapper operationLogMapper;
    @MockBean private SysLoginLogMapper loginLogMapper;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("GET /api/logs/operation")
    class OperationLogsTests {

        @Test
        @WithMockUser(authorities = "sys:log:list")
        @DisplayName("有权限 - 返回操作日志分页")
        void operationLogs_success() throws Exception {
            Page<SysOperationLog> page = new Page<>(1, 10, 0);
            page.setRecords(List.of());
            when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

            mockMvc.perform(get("/api/logs/operation")
                            .param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void operationLogs_unauthorized() throws Exception {
            mockMvc.perform(get("/api/logs/operation"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "sys:other:list")
        @DisplayName("无权限 - 返回403")
        void operationLogs_forbidden() throws Exception {
            mockMvc.perform(get("/api/logs/operation"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/logs/login")
    class LoginLogsTests {

        @Test
        @WithMockUser(authorities = "sys:log:list")
        @DisplayName("有权限 - 返回登录日志分页")
        void loginLogs_success() throws Exception {
            Page<SysLoginLog> page = new Page<>(1, 10, 0);
            page.setRecords(List.of());
            when(loginLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

            mockMvc.perform(get("/api/logs/login")
                            .param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void loginLogs_unauthorized() throws Exception {
            mockMvc.perform(get("/api/logs/login"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
