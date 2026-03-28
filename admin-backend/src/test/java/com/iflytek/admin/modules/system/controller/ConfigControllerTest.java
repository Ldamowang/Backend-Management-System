package com.iflytek.admin.modules.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.entity.SysConfig;
import com.iflytek.admin.modules.system.mapper.SysConfigMapper;
import com.iflytek.admin.security.CustomAccessDeniedHandler;
import com.iflytek.admin.security.CustomUserDetailsService;
import com.iflytek.admin.security.JwtAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfigController.class)
@Import({JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("ConfigController 系统配置接口测试")
class ConfigControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SysConfigMapper configMapper;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("GET /api/configs")
    class ListTests {

        @Test
        @WithMockUser(authorities = "sys:config:list")
        @DisplayName("有权限 - 返回配置列表")
        void list_success() throws Exception {
            SysConfig config = new SysConfig();
            config.setId(1L);
            config.setConfigKey("site.name");
            config.setConfigValue("Admin System");
            when(configMapper.selectList(any())).thenReturn(List.of(config));

            mockMvc.perform(get("/api/configs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].configKey").value("site.name"));
        }

        @Test
        @WithMockUser(authorities = "sys:other:list")
        @DisplayName("无权限 - 返回403")
        void list_forbidden() throws Exception {
            mockMvc.perform(get("/api/configs"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void list_unauthorized() throws Exception {
            mockMvc.perform(get("/api/configs"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/configs")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "sys:config:edit")
        @DisplayName("有效数据 - 更新成功")
        void update_success() throws Exception {
            SysConfig config = new SysConfig();
            config.setId(1L);
            config.setConfigKey("site.name");
            config.setConfigValue("New Name");

            mockMvc.perform(put("/api/configs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(config))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(configMapper).updateById(any(SysConfig.class));
        }

        @Test
        @WithMockUser(authorities = "sys:config:list")
        @DisplayName("无编辑权限 - 返回403")
        void update_forbidden() throws Exception {
            mockMvc.perform(put("/api/configs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isForbidden());
        }
    }
}
