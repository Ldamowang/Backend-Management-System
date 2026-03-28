package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.modules.system.dto.UserCreateDTO;
import com.iflytek.admin.modules.system.dto.UserQueryDTO;
import com.iflytek.admin.modules.system.dto.UserUpdateDTO;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import com.iflytek.admin.modules.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock private SysUserMapper userMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setGender(1);
        testUser.setStatus(1);
    }

    @Nested
    @DisplayName("分页查询测试")
    class PageTests {

        @Test
        @DisplayName("无过滤条件 - 返回全部用户")
        void page_noFilter() {
            UserQueryDTO query = new UserQueryDTO();
            query.setPage(1);
            query.setSize(10);

            Page<SysUser> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(testUser));

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
            when(userMapper.selectUserRoles(1L)).thenReturn(List.of("admin"));

            var result = userService.page(query);

            assertThat(result.getList()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("创建用户测试")
    class CreateTests {

        @Test
        @DisplayName("正常创建 - 成功")
        void create_success() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setNickname("新用户");
            dto.setEmail("new@example.com");
            dto.setRoleIds(List.of(1L, 2L));

            when(userMapper.selectUserByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");

            userService.create(dto);

            verify(userMapper).insert(any(SysUser.class));
            verify(userRoleMapper, times(2)).insert(any());
        }

        @Test
        @DisplayName("用户名已存在 - 抛出异常")
        void create_usernameExists() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("pass");

            when(userMapper.selectUserByUsername("testuser")).thenReturn(testUser);

            assertThatThrownBy(() -> userService.create(dto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("更新用户测试")
    class UpdateTests {

        @Test
        @DisplayName("正常更新 - 成功")
        void update_success() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("新昵称");
            dto.setEmail("new@example.com");

            when(userMapper.selectById(1L)).thenReturn(testUser);

            userService.update(1L, dto);

            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void update_notFound() {
            UserUpdateDTO dto = new UserUpdateDTO();
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> userService.update(999L, dto))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("删除用户测试")
    class DeleteTests {

        @Test
        @DisplayName("正常删除 - 成功")
        void delete_success() {
            userService.delete(1L);
            verify(userMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("状态切换测试")
    class StatusTests {

        @Test
        @DisplayName("切换状态 - 成功")
        void updateStatus_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);

            userService.updateStatus(1L, 0);

            verify(userMapper).updateById(argThat(user ->
                    ((SysUser) user).getStatus() == 0));
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void updateStatus_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> userService.updateStatus(999L, 1))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("获取用户详情测试")
    class GetByIdTests {

        @Test
        @DisplayName("用户存在 - 返回用户信息包含角色")
        void getById_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectUserRoles(1L)).thenReturn(List.of("admin"));

            Map<String, Object> result = userService.getById(1L);

            assertThat(result.get("username")).isEqualTo("testuser");
            assertThat(result.get("nickname")).isEqualTo("测试用户");
            assertThat(result.get("email")).isEqualTo("test@example.com");
            assertThat(result.get("phone")).isEqualTo("13800138000");
            assertThat(result.get("gender")).isEqualTo(1);
            assertThat(result.get("status")).isEqualTo(1);
            assertThat(result.get("roles")).isEqualTo(List.of("admin"));
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void getById_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> userService.getById(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("分页查询 - 带过滤条件")
    class PageWithFilterTests {

        @Test
        @DisplayName("按用户名过滤 - 构建like条件")
        void page_filterByUsername() {
            UserQueryDTO query = new UserQueryDTO();
            query.setPage(1);
            query.setSize(10);
            query.setUsername("admin");

            Page<SysUser> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(testUser));

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
            when(userMapper.selectUserRoles(anyLong())).thenReturn(List.of("admin"));

            var result = userService.page(query);

            assertThat(result.getList()).hasSize(1);
            verify(userMapper).selectPage(any(Page.class), any());
        }

        @Test
        @DisplayName("按邮箱过滤")
        void page_filterByEmail() {
            UserQueryDTO query = new UserQueryDTO();
            query.setPage(1);
            query.setSize(10);
            query.setEmail("test@");

            Page<SysUser> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(List.of());

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

            var result = userService.page(query);

            assertThat(result.getList()).isEmpty();
        }

        @Test
        @DisplayName("按状态过滤")
        void page_filterByStatus() {
            UserQueryDTO query = new UserQueryDTO();
            query.setPage(1);
            query.setSize(10);
            query.setStatus(1);

            Page<SysUser> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(testUser));

            when(userMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);
            when(userMapper.selectUserRoles(1L)).thenReturn(List.of("admin"));

            var result = userService.page(query);

            assertThat(result.getList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("更新用户 - 角色变更")
    class UpdateWithRolesTests {

        @Test
        @DisplayName("更新用户含角色 - 先删后增")
        void update_withRoleIds() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("新昵称");
            dto.setRoleIds(List.of(1L, 2L));

            when(userMapper.selectById(1L)).thenReturn(testUser);

            userService.update(1L, dto);

            verify(userMapper).updateById(any(SysUser.class));
            verify(userRoleMapper).delete(any());
            verify(userRoleMapper, times(2)).insert(any());
        }

        @Test
        @DisplayName("更新用户不含角色 - 不操作角色表")
        void update_withoutRoleIds() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("新昵称");
            // roleIds is null

            when(userMapper.selectById(1L)).thenReturn(testUser);

            userService.update(1L, dto);

            verify(userMapper).updateById(any(SysUser.class));
            verify(userRoleMapper, never()).delete(any());
        }

        @Test
        @DisplayName("更新所有字段")
        void update_allFields() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("新昵称");
            dto.setEmail("new@test.com");
            dto.setPhone("13900139000");
            dto.setGender(0);
            dto.setStatus(0);

            when(userMapper.selectById(1L)).thenReturn(testUser);

            userService.update(1L, dto);

            verify(userMapper).updateById(argThat(user -> {
                SysUser u = (SysUser) user;
                return "新昵称".equals(u.getNickname())
                        && "new@test.com".equals(u.getEmail())
                        && "13900139000".equals(u.getPhone())
                        && u.getGender() == 0
                        && u.getStatus() == 0;
            }));
        }
    }

    @Nested
    @DisplayName("创建用户 - 无角色")
    class CreateWithoutRolesTests {

        @Test
        @DisplayName("创建用户不含角色 - 不插入角色关联")
        void create_withoutRoleIds() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setNickname("新用户");

            when(userMapper.selectUserByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");

            userService.create(dto);

            verify(userMapper).insert(any(SysUser.class));
            verify(userRoleMapper, never()).insert(any());
        }
    }
}
