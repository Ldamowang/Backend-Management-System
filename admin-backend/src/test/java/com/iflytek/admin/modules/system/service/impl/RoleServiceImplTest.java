package com.iflytek.admin.modules.system.service.impl;

import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.modules.system.dto.RoleFormDTO;
import com.iflytek.admin.modules.system.entity.SysRole;
import com.iflytek.admin.modules.system.entity.SysRoleMenu;
import com.iflytek.admin.modules.system.mapper.SysRoleMapper;
import com.iflytek.admin.modules.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @InjectMocks
    private RoleServiceImpl roleService;

    @Mock private SysRoleMapper roleMapper;
    @Mock private SysRoleMenuMapper roleMenuMapper;

    private SysRole testRole;

    @BeforeEach
    void setUp() {
        testRole = new SysRole();
        testRole.setId(1L);
        testRole.setRoleName("管理员");
        testRole.setRoleKey("admin");
        testRole.setSortOrder(1);
        testRole.setStatus(1);
    }

    @Nested
    @DisplayName("角色列表测试")
    class ListTests {

        @Test
        @DisplayName("返回按排序的角色列表")
        void list_success() {
            when(roleMapper.selectList(any())).thenReturn(List.of(testRole));

            List<SysRole> result = roleService.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleName()).isEqualTo("管理员");
        }
    }

    @Nested
    @DisplayName("获取角色详情测试")
    class GetByIdTests {

        @Test
        @DisplayName("角色存在 - 返回角色信息")
        void getById_success() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);

            SysRole result = roleService.getById(1L);

            assertThat(result.getRoleName()).isEqualTo("管理员");
            assertThat(result.getRoleKey()).isEqualTo("admin");
        }

        @Test
        @DisplayName("角色不存在 - 抛出异常")
        void getById_notFound() {
            when(roleMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> roleService.getById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("角色不存在");
        }
    }

    @Nested
    @DisplayName("创建角色测试")
    class CreateTests {

        @Test
        @DisplayName("正常创建 - 成功")
        void create_success() {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("编辑者");
            dto.setRoleKey("editor");
            dto.setDescription("编辑权限");

            roleService.create(dto);

            verify(roleMapper).insert(argThat(role ->
                    ((SysRole) role).getRoleName().equals("编辑者")
                            && ((SysRole) role).getRoleKey().equals("editor")));
        }
    }

    @Nested
    @DisplayName("更新角色测试")
    class UpdateTests {

        @Test
        @DisplayName("正常更新 - 成功")
        void update_success() {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("超级管理员");
            dto.setDescription("更新描述");

            when(roleMapper.selectById(1L)).thenReturn(testRole);

            roleService.update(1L, dto);

            verify(roleMapper).updateById(argThat(role ->
                    ((SysRole) role).getRoleName().equals("超级管理员")));
        }

        @Test
        @DisplayName("角色不存在 - 抛出异常")
        void update_notFound() {
            when(roleMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> roleService.update(999L, new RoleFormDTO()))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("分配菜单权限测试")
    class AssignMenusTests {

        @Test
        @DisplayName("分配菜单 - 先删后增")
        void assignMenus_success() {
            roleService.assignMenus(1L, List.of(1L, 2L, 3L));

            verify(roleMenuMapper).delete(any());
            verify(roleMenuMapper, times(3)).insert(any(SysRoleMenu.class));
        }

        @Test
        @DisplayName("空列表 - 仅清除")
        void assignMenus_emptyList() {
            roleService.assignMenus(1L, null);

            verify(roleMenuMapper).delete(any());
            verify(roleMenuMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("删除角色测试")
    class DeleteTests {

        @Test
        @DisplayName("删除角色及关联菜单")
        void delete_success() {
            roleService.delete(1L);

            verify(roleMapper).deleteById(1L);
            verify(roleMenuMapper).delete(any());
        }
    }
}
