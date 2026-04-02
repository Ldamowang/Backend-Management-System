package com.iflytek.admin.modules.system.service.impl;

import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.dto.MenuFormDTO;
import com.iflytek.admin.modules.system.entity.SysMenu;
import com.iflytek.admin.modules.system.mapper.SysMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @InjectMocks
    private MenuServiceImpl menuService;

    @Mock private SysMenuMapper menuMapper;
    @Mock private CacheService cacheService;

    private SysMenu parentMenu;
    private SysMenu childMenu;

    @BeforeEach
    void setUp() {
        parentMenu = new SysMenu();
        parentMenu.setId(1L);
        parentMenu.setParentId(0L);
        parentMenu.setMenuName("系统管理");
        parentMenu.setMenuType(1);
        parentMenu.setSortOrder(1);

        childMenu = new SysMenu();
        childMenu.setId(2L);
        childMenu.setParentId(1L);
        childMenu.setMenuName("用户管理");
        childMenu.setMenuType(2);
        childMenu.setPath("/system/user");
        childMenu.setSortOrder(1);
    }

    @Nested
    @DisplayName("菜单树测试")
    class TreeTests {

        @Test
        @DisplayName("构建菜单树 - 正确的父子关系")
        void getMenuTree_success() {
            when(menuMapper.selectList(any())).thenReturn(List.of(parentMenu, childMenu));

            List<Map<String, Object>> tree = menuService.getMenuTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).get("menuName")).isEqualTo("系统管理");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get(0).get("children");
            assertThat(children).hasSize(1);
            assertThat(children.get(0).get("menuName")).isEqualTo("用户管理");
        }

        @Test
        @DisplayName("空菜单列表 - 返回空树")
        void getMenuTree_empty() {
            when(menuMapper.selectList(any())).thenReturn(List.of());

            List<Map<String, Object>> tree = menuService.getMenuTree();

            assertThat(tree).isEmpty();
        }
    }

    @Nested
    @DisplayName("获取菜单详情测试")
    class GetByIdTests {

        @Test
        @DisplayName("菜单存在 - 返回菜单")
        void getById_success() {
            when(menuMapper.selectById(1L)).thenReturn(parentMenu);

            SysMenu result = menuService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getMenuName()).isEqualTo("系统管理");
        }

        @Test
        @DisplayName("菜单不存在 - 返回null")
        void getById_notFound() {
            when(menuMapper.selectById(999L)).thenReturn(null);

            SysMenu result = menuService.getById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("创建菜单测试")
    class CreateTests {

        @Test
        @DisplayName("正常创建 - 成功")
        void create_success() {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("角色管理");
            dto.setMenuType(2);
            dto.setPath("/system/role");
            dto.setParentId(1L);

            menuService.create(dto);

            verify(menuMapper).insert(argThat(menu ->
                    ((SysMenu) menu).getMenuName().equals("角色管理")
                            && ((SysMenu) menu).getParentId().equals(1L)));
        }
    }

    @Nested
    @DisplayName("更新菜单测试")
    class UpdateTests {

        @Test
        @DisplayName("菜单不存在 - 抛出异常")
        void update_notFound() {
            when(menuMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> menuService.update(999L, new MenuFormDTO()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("正常更新 - 成功")
        void update_success() {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("更新菜单名");
            dto.setMenuType(2);

            when(menuMapper.selectById(1L)).thenReturn(parentMenu);

            menuService.update(1L, dto);

            verify(menuMapper).updateById(any(SysMenu.class));
        }
    }

    @Nested
    @DisplayName("删除菜单测试")
    class DeleteTests {

        @Test
        @DisplayName("有子菜单 - 拒绝删除")
        void delete_hasChildren() {
            when(menuMapper.selectCount(any())).thenReturn(2L);

            assertThatThrownBy(() -> menuService.delete(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("子菜单");
        }

        @Test
        @DisplayName("无子菜单 - 成功删除")
        void delete_success() {
            when(menuMapper.selectCount(any())).thenReturn(0L);

            menuService.delete(1L);

            verify(menuMapper).deleteById(1L);
        }
    }
}
