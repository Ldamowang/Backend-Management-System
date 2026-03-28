package com.iflytek.admin.security;

import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 测试")
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Mock
    private SysUserMapper userMapper;

    @Test
    @DisplayName("加载存在的活跃用户 - 返回UserDetails")
    void loadUserByUsername_success() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$encoded");
        user.setStatus(1);

        when(userMapper.selectUserByUsername("admin")).thenReturn(user);
        when(userMapper.selectUserPermissions(1L)).thenReturn(List.of("sys:user:list", "sys:user:add"));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(userDetails.getAuthorities()).hasSize(2);
    }

    @Test
    @DisplayName("用户不存在 - 抛出UsernameNotFoundException")
    void loadUserByUsername_notFound() {
        when(userMapper.selectUserByUsername("unknown")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("用户已禁用 - 抛出UsernameNotFoundException")
    void loadUserByUsername_disabled() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("disabled");
        user.setPassword("$2a$10$encoded");
        user.setStatus(0);

        when(userMapper.selectUserByUsername("disabled")).thenReturn(user);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("disabled"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户已被禁用");
    }
}
