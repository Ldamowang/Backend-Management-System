package com.iflytek.admin.modules.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.dto.MenuFormDTO;
import com.iflytek.admin.modules.system.entity.SysMenu;
import com.iflytek.admin.modules.system.service.MenuService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
@Import({JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
@DisplayName("MenuController 菜单管理接口测试")
class MenuControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MenuService menuService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private RedisTemplate<String, Object> redisTemplate;

    @Nested
    @DisplayName("GET /api/menus/tree")
    class TreeTests {

        @Test
        @WithMockUser(authorities = "sys:menu:list")
        @DisplayName("有权限 - 返回菜单树")
        void getMenuTree_success() throws Exception {
            List<Map<String, Object>> tree = List.of(
                    Map.of("id", 1L, "menuName", "系统管理", "children", List.of()));
            when(menuService.getMenuTree()).thenReturn(tree);

            mockMvc.perform(get("/api/menus/tree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].menuName").value("系统管理"));
        }

        @Test
        @DisplayName("未认证 - 返回401")
        void getMenuTree_unauthorized() throws Exception {
            mockMvc.perform(get("/api/menus/tree"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/menus/{id}")
    class GetByIdTests {

        @Test
        @WithMockUser(authorities = "sys:menu:list")
        @DisplayName("菜单存在 - 返回详情")
        void getById_success() throws Exception {
            SysMenu menu = new SysMenu();
            menu.setId(1L);
            menu.setMenuName("用户管理");
            menu.setMenuType(2);
            when(menuService.getById(1L)).thenReturn(menu);

            mockMvc.perform(get("/api/menus/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.menuName").value("用户管理"));
        }
    }

    @Nested
    @DisplayName("POST /api/menus")
    class CreateTests {

        @Test
        @WithMockUser(authorities = "sys:menu:add")
        @DisplayName("有效数据 - 创建成功")
        void create_success() throws Exception {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("新菜单");
            dto.setMenuType(2);
            dto.setPath("/new");

            mockMvc.perform(post("/api/menus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(menuService).create(any(MenuFormDTO.class));
        }

        @Test
        @WithMockUser(authorities = "sys:menu:add")
        @DisplayName("菜单名为空 - 返回400")
        void create_emptyName() throws Exception {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("");
            dto.setMenuType(2);

            mockMvc.perform(post("/api/menus")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/menus/{id}")
    class UpdateTests {

        @Test
        @WithMockUser(authorities = "sys:menu:edit")
        @DisplayName("有效更新 - 返回200")
        void update_success() throws Exception {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("更新菜单");
            dto.setMenuType(2);

            mockMvc.perform(put("/api/menus/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            verify(menuService).update(eq(1L), any(MenuFormDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/menus/{id}")
    class DeleteTests {

        @Test
        @WithMockUser(authorities = "sys:menu:delete")
        @DisplayName("有权限 - 删除成功")
        void delete_success() throws Exception {
            mockMvc.perform(delete("/api/menus/1"))
                    .andExpect(status().isOk());

            verify(menuService).delete(1L);
        }

        @Test
        @WithMockUser(authorities = "sys:menu:list")
        @DisplayName("无删除权限 - 返回403")
        void delete_forbidden() throws Exception {
            mockMvc.perform(delete("/api/menus/1"))
                    .andExpect(status().isForbidden());
        }
    }
}
