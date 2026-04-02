package com.iflytek.admin.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.auth.dto.LoginRequest;
import com.iflytek.admin.modules.auth.dto.LoginResponse;
import com.iflytek.admin.modules.system.entity.SysLoginLog;
import com.iflytek.admin.modules.system.entity.SysMenu;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.entity.SysUserRole;
import com.iflytek.admin.modules.system.mapper.SysLoginLogMapper;
import com.iflytek.admin.modules.system.mapper.SysMenuMapper;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private SysUserMapper userMapper;
    @Mock private SysUserRoleMapper userRoleMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private SysLoginLogMapper loginLogMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private CacheService cacheService;

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encoded");
        testUser.setNickname("管理员");
        testUser.setStatus(1);
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录 - 返回 Token")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("admin123");

            when(userMapper.selectUserByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("admin123", "$2a$10$encoded")).thenReturn(true);
            when(jwtUtil.generateAccessToken(1L, "admin")).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh-token");
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(1800000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            LoginResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(1800L);

            verify(userMapper).updateById(testUser);
            verify(loginLogMapper).insert(any(SysLoginLog.class));
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void login_userNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUsername("unknown");
            request.setPassword("pass");

            when(userMapper.selectUserByUsername("unknown")).thenReturn(null);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);

            verify(loginLogMapper).insert(any(SysLoginLog.class));
        }

        @Test
        @DisplayName("密码错误 - 抛出异常")
        void login_wrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("wrong");

            when(userMapper.selectUserByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("用户已禁用 - 抛出异常")
        void login_userDisabled() {
            testUser.setStatus(0);
            LoginRequest request = new LoginRequest();
            request.setUsername("admin");
            request.setPassword("admin123");

            when(userMapper.selectUserByUsername("admin")).thenReturn(testUser);
            when(passwordEncoder.matches("admin123", "$2a$10$encoded")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("登出测试")
    class LogoutTests {

        @Test
        @DisplayName("正常登出 - Token加入黑名单")
        void logout_success() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(1800000L);

            authService.logout("Bearer some-token");

            verify(valueOperations).set(
                    eq("token:blacklist:some-token"),
                    eq("1"),
                    eq(1800000L),
                    any()
            );
        }

        @Test
        @DisplayName("空Token登出 - 不操作Redis")
        void logout_nullToken() {
            authService.logout(null);
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("刷新Token测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("有效RefreshToken - 返回新Token")
        void refreshToken_success() {
            when(jwtUtil.isTokenValid("valid-refresh")).thenReturn(true);
            when(jwtUtil.getTokenType("valid-refresh")).thenReturn("refresh");
            when(jwtUtil.getUserId("valid-refresh")).thenReturn(1L);
            when(jwtUtil.getUsername("valid-refresh")).thenReturn("admin");
            when(jwtUtil.generateAccessToken(1L, "admin")).thenReturn("new-access");
            when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("new-refresh");
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(1800000L);

            LoginResponse response = authService.refreshToken("valid-refresh");

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(1800L);
        }

        @Test
        @DisplayName("过期RefreshToken - 抛出异常")
        void refreshToken_expired() {
            when(jwtUtil.isTokenValid("expired")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken("expired"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("使用AccessToken刷新 - 拒绝并抛出异常")
        void refreshToken_accessTokenType() {
            when(jwtUtil.isTokenValid("access-token")).thenReturn(true);
            when(jwtUtil.getTokenType("access-token")).thenReturn("access");

            assertThatThrownBy(() -> authService.refreshToken("access-token"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("获取用户信息测试")
    class GetUserInfoTests {

        @Test
        @DisplayName("正常获取 - 返回用户信息、角色、权限和菜单树")
        void getUserInfo_success() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectUserRoles(1L)).thenReturn(List.of("admin"));
            when(userMapper.selectUserPermissions(1L)).thenReturn(List.of("sys:user:list"));

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(1L);
            userRole.setRoleId(1L);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole));

            SysMenu parentMenu = new SysMenu();
            parentMenu.setId(1L);
            parentMenu.setParentId(0L);
            parentMenu.setMenuName("系统管理");
            parentMenu.setMenuType(1);

            SysMenu childMenu = new SysMenu();
            childMenu.setId(2L);
            childMenu.setParentId(1L);
            childMenu.setMenuName("用户管理");
            childMenu.setMenuType(2);

            when(menuMapper.selectMenusByRoleIds(List.of(1L))).thenReturn(List.of(parentMenu, childMenu));

            Map<String, Object> result = authService.getUserInfo(1L);

            assertThat(result).containsKeys("user", "roles", "permissions", "menus");

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
            assertThat(userInfo.get("username")).isEqualTo("admin");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) result.get("roles");
            assertThat(roles).containsExactly("admin");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> menus = (List<Map<String, Object>>) result.get("menus");
            assertThat(menus).hasSize(1);
            assertThat(menus.get(0).get("menuName")).isEqualTo("系统管理");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) menus.get(0).get("children");
            assertThat(children).hasSize(1);
            assertThat(children.get(0).get("menuName")).isEqualTo("用户管理");
        }

        @Test
        @DisplayName("用户不存在 - 抛出异常")
        void getUserInfo_notFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> authService.getUserInfo(999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("无角色 - 返回空菜单")
        void getUserInfo_noRoles() {
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectUserRoles(1L)).thenReturn(List.of());
            when(userMapper.selectUserPermissions(1L)).thenReturn(List.of());
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            Map<String, Object> result = authService.getUserInfo(1L);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> menus = (List<Map<String, Object>>) result.get("menus");
            assertThat(menus).isEmpty();
        }
    }

    @Nested
    @DisplayName("登出附加测试")
    class LogoutAdditionalTests {

        @Test
        @DisplayName("不带Bearer前缀的Token - 仍加入黑名单")
        void logout_withoutBearerPrefix() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(jwtUtil.getAccessTokenExpiration()).thenReturn(1800000L);

            authService.logout("raw-token");

            verify(valueOperations).set(
                    eq("token:blacklist:raw-token"),
                    eq("1"),
                    eq(1800000L),
                    any()
            );
        }
    }
}
