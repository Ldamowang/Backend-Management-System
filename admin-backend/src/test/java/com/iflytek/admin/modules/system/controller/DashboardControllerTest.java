package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.mapper.SysLoginLogMapper;
import com.iflytek.admin.modules.system.mapper.SysMenuMapper;
import com.iflytek.admin.modules.system.mapper.SysRoleMapper;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import com.iflytek.admin.security.CustomAccessDeniedHandler;
import com.iflytek.admin.security.CustomUserDetailsService;
import com.iflytek.admin.security.JwtAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("DashboardController 仪表盘接口测试")
class DashboardControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SysUserMapper userMapper;
    @MockBean private SysRoleMapper roleMapper;
    @MockBean private SysMenuMapper menuMapper;
    @MockBean private SysLoginLogMapper loginLogMapper;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Test
    @WithMockUser
    @DisplayName("已认证用户 - 返回统计数据")
    void getStats_success() throws Exception {
        when(userMapper.selectCount(any())).thenReturn(10L);
        when(roleMapper.selectCount(any())).thenReturn(5L);
        when(menuMapper.selectCount(any())).thenReturn(20L);
        when(loginLogMapper.selectCount(any())).thenReturn(100L);

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userCount").value(10))
                .andExpect(jsonPath("$.data.roleCount").value(5))
                .andExpect(jsonPath("$.data.menuCount").value(20));
    }

    @Test
    @DisplayName("未认证 - 返回401")
    void getStats_unauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
