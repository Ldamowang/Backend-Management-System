package com.iflytek.admin.modules.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.profile.dto.PasswordUpdateDTO;
import com.iflytek.admin.modules.profile.dto.ProfileUpdateDTO;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import({JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("ProfileController 个人中心接口测试")
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SysUserMapper userMapper;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    private UsernamePasswordAuthenticationToken createAuth(Long userId) {
        UserDetails userDetails = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        return new UsernamePasswordAuthenticationToken(userDetails, userId, userDetails.getAuthorities());
    }

    @Nested
    @DisplayName("GET /api/profile")
    class GetProfileTests {

        @Test
        @DisplayName("已认证 - 返回个人信息")
        void getProfile_success() throws Exception {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setUsername("admin");
            user.setNickname("管理员");
            user.setEmail("admin@test.com");
            user.setPhone("13800138000");
            user.setAvatar("avatar.png");
            user.setGender(1);
            when(userMapper.selectById(1L)).thenReturn(user);

            mockMvc.perform(get("/api/profile")
                            .with(authentication(createAuth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.nickname").value("管理员"))
                    .andExpect(jsonPath("$.data.email").value("admin@test.com"));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void getProfile_unauthorized() throws Exception {
            mockMvc.perform(get("/api/profile"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/profile")
    class UpdateProfileTests {

        @Test
        @DisplayName("有效更新 - 返回200")
        void updateProfile_success() throws Exception {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setUsername("admin");
            when(userMapper.selectById(1L)).thenReturn(user);

            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setNickname("新昵称");
            dto.setEmail("new@test.com");

            mockMvc.perform(put("/api/profile")
                            .with(authentication(createAuth(1L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("用户不存在 - 返回错误")
        void updateProfile_userNotFound() throws Exception {
            when(userMapper.selectById(1L)).thenReturn(null);

            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setNickname("新昵称");

            mockMvc.perform(put("/api/profile")
                            .with(authentication(createAuth(1L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    @Nested
    @DisplayName("PUT /api/profile/password")
    class UpdatePasswordTests {

        @Test
        @DisplayName("密码正确 - 修改成功")
        void updatePassword_success() throws Exception {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setPassword("$2a$encoded");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(passwordEncoder.matches("oldpass123", "$2a$encoded")).thenReturn(true);
            when(passwordEncoder.encode("newpass123")).thenReturn("$2a$new");

            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass123");
            dto.setNewPassword("newpass123");

            mockMvc.perform(put("/api/profile/password")
                            .with(authentication(createAuth(1L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));

            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("当前密码错误 - 返回400")
        void updatePassword_wrongOldPassword() throws Exception {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setPassword("$2a$encoded");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(passwordEncoder.matches("wrongpass", "$2a$encoded")).thenReturn(false);

            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("wrongpass");
            dto.setNewPassword("newpass123");

            mockMvc.perform(put("/api/profile/password")
                            .with(authentication(createAuth(1L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("新密码太短 - 返回400")
        void updatePassword_shortNewPassword() throws Exception {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass123");
            dto.setNewPassword("123");

            mockMvc.perform(put("/api/profile/password")
                            .with(authentication(createAuth(1L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
