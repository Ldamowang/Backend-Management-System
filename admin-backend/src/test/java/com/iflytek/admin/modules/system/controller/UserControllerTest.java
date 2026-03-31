package com.iflytek.admin.modules.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.dto.StatusDTO;
import com.iflytek.admin.modules.system.dto.UserCreateDTO;
import com.iflytek.admin.modules.system.dto.UserUpdateDTO;
import com.iflytek.admin.modules.system.service.UserService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("UserController 用户管理接口测试")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("GET /api/users")
    class PageTests {

        @Test
        @WithMockUser(authorities = "sys:user:list")
        @DisplayName("有权限 - 返回用户分页列表")
        void page_success() throws Exception {
            PageResult<Map<String, Object>> pageResult = new PageResult<>(
                    List.of(Map.of("id", 1L, "username", "admin")), 1L, 1, 10);
            when(userService.page(any())).thenReturn(pageResult);

            mockMvc.perform(get("/api/users").param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1));
        }

        @Test
        @WithMockUser(authorities = "sys:other:list")
        @DisplayName("无权限 - 返回403")
        void page_forbidden() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void page_unauthorized() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "sys:user:list")
        @DisplayName("有权限且用户存在 - 返回用户详情")
        void getById_success() throws Exception {
            when(userService.getById(1L)).thenReturn(Map.of("id", 1L, "username", "admin"));

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("admin"));
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "sys:user:add")
        @DisplayName("有效数据 - 创建成功")
        void create_success() throws Exception {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setNickname("新用户");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userService).create(any(UserCreateDTO.class));
        }

        @Test
        @WithMockUser(authorities = "sys:user:add")
        @DisplayName("用户名为空 - 返回400")
        void create_emptyUsername() throws Exception {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("");
            dto.setPassword("password123");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(authorities = "sys:user:add")
        @DisplayName("密码太短 - 返回400")
        void create_shortPassword() throws Exception {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("newuser");
            dto.setPassword("12345");

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "sys:user:edit")
        @DisplayName("有效更新 - 返回200")
        void update_success() throws Exception {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("更新昵称");

            mockMvc.perform(put("/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(userService).update(eq(1L), any(UserUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "sys:user:delete")
        @DisplayName("有权限 - 删除成功")
        void delete_success() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isOk());

            verify(userService).delete(1L);
        }

        @Test
        @WithMockUser(authorities = "sys:user:list")
        @DisplayName("无删除权限 - 返回403")
        void delete_forbidden() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{id}/status")
    class StatusTests {

        @Test
        @WithMockUser(authorities = "sys:user:edit")
        @DisplayName("有效状态 - 更新成功")
        void updateStatus_success() throws Exception {
            StatusDTO dto = new StatusDTO();
            dto.setStatus(0);

            mockMvc.perform(patch("/api/users/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(userService).updateStatus(1L, 0);
        }
    }
}
