package com.iflytek.admin.modules.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.dto.AssignMenusDTO;
import com.iflytek.admin.modules.system.dto.RoleFormDTO;
import com.iflytek.admin.modules.system.entity.SysRole;
import com.iflytek.admin.modules.system.service.RoleService;
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

@WebMvcTest(RoleController.class)
@Import({JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("RoleController 角色管理接口测试")
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RoleService roleService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("GET /api/roles")
    class ListTests {

        @Test
        @WithMockUser(authorities = "sys:role:list")
        @DisplayName("有权限 - 返回角色列表")
        void list_success() throws Exception {
            SysRole role = new SysRole();
            role.setId(1L);
            role.setRoleName("管理员");
            role.setRoleKey("admin");
            when(roleService.list()).thenReturn(List.of(role));

            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].roleName").value("管理员"));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void list_unauthorized() throws Exception {
            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "sys:other:list")
        @DisplayName("无权限 - 返回403")
        void list_forbidden() throws Exception {
            mockMvc.perform(get("/api/roles"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/roles/{id}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "sys:role:list")
        @DisplayName("角色存在 - 返回详情")
        void getById_success() throws Exception {
            SysRole role = new SysRole();
            role.setId(1L);
            role.setRoleName("管理员");
            when(roleService.getById(1L)).thenReturn(role);

            mockMvc.perform(get("/api/roles/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roleName").value("管理员"));
        }
    }

    @Nested
    @DisplayName("POST /api/roles")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "sys:role:add")
        @DisplayName("有效数据 - 创建成功")
        void create_success() throws Exception {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("编辑员");
            dto.setRoleKey("editor");

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(roleService).create(any(RoleFormDTO.class));
        }

        @Test
        @WithMockUser(authorities = "sys:role:add")
        @DisplayName("角色名为空 - 返回400")
        void create_emptyName() throws Exception {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("");
            dto.setRoleKey("key");

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/roles/{id}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "sys:role:edit")
        @DisplayName("有效更新 - 返回200")
        void update_success() throws Exception {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("更新角色");
            dto.setRoleKey("updated");

            mockMvc.perform(put("/api/roles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(roleService).update(eq(1L), any(RoleFormDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/roles/{id}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "sys:role:delete")
        @DisplayName("有权限 - 删除成功")
        void delete_success() throws Exception {
            mockMvc.perform(delete("/api/roles/1"))
                    .andExpect(status().isOk());

            verify(roleService).delete(1L);
        }

        @Test
        @WithMockUser(authorities = "sys:role:list")
        @DisplayName("无删除权限 - 返回403")
        void delete_forbidden() throws Exception {
            mockMvc.perform(delete("/api/roles/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/roles/{id}/menus")
    class AssignMenusTests {

        @Test
        @WithMockUser(authorities = "sys:role:edit")
        @DisplayName("分配菜单权限 - 成功")
        void assignMenus_success() throws Exception {
            AssignMenusDTO dto = new AssignMenusDTO();
            dto.setMenuIds(List.of(1L, 2L, 3L));

            mockMvc.perform(post("/api/roles/1/menus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(roleService).assignMenus(eq(1L), eq(List.of(1L, 2L, 3L)));
        }
    }
}
