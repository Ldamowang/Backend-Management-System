package com.iflytek.admin.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.auth.dto.LoginRequest;
import com.iflytek.admin.modules.auth.dto.LoginResponse;
import com.iflytek.admin.modules.auth.dto.RefreshTokenDTO;
import com.iflytek.admin.modules.auth.service.AuthService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("AuthController 认证接口测试")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("有效凭证 - 返回200和Token")
        void login_success() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("admin123");

            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(1800)
                    .build();
            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("用户名为空 - 返回400")
        void login_emptyUsername() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("");
            request.setPassword("admin123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码为空 - 返回400")
        void login_emptyPassword() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("有效RefreshToken - 返回新Token")
        void refresh_success() throws Exception {
            RefreshTokenDTO dto = new RefreshTokenDTO();
            dto.setRefreshToken("valid-refresh-token");

            LoginResponse response = LoginResponse.builder()
                    .accessToken("new-access")
                    .refreshToken("new-refresh")
                    .tokenType("Bearer")
                    .expiresIn(1800)
                    .build();
            when(authService.refreshToken("valid-refresh-token")).thenReturn(response);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
        }

        @Test
        @DisplayName("RefreshToken为空 - 返回400")
        void refresh_emptyToken() throws Exception {
            RefreshTokenDTO dto = new RefreshTokenDTO();
            dto.setRefreshToken("");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @WithMockUser
        @DisplayName("已认证用户登出 - 返回200")
        void logout_success() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer some-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(authService).logout("Bearer some-token");
        }

        @Test
        @DisplayName("未认证用户登出 - 返回401")
        void logout_unauthorized() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/userinfo")
    class UserInfoTests {

        @Test
        @DisplayName("已认证用户 - 返回用户信息")
        void userInfo_success() throws Exception {
            UserDetails userDetails = User.withUsername("admin")
                    .password("pass")
                    .authorities("ROLE_USER")
                    .build();
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, 1L, userDetails.getAuthorities());

            Map<String, Object> userInfo = Map.of(
                    "user", Map.of("username", "admin"),
                    "roles", List.of("admin"),
                    "permissions", List.of("sys:user:list")
            );
            when(authService.getUserInfo(1L)).thenReturn(userInfo);

            mockMvc.perform(get("/api/auth/userinfo")
                            .with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.roles[0]").value("admin"));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void userInfo_unauthorized() throws Exception {
            mockMvc.perform(get("/api/auth/userinfo"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
