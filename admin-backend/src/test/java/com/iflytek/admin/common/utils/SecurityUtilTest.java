package com.iflytek.admin.common.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUtil 工具类测试")
class SecurityUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUsername - 有认证信息时返回用户名")
    void getCurrentUsername_authenticated() {
        UserDetails user = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        assertThat(SecurityUtil.getCurrentUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("getCurrentUsername - 无认证信息时返回null")
    void getCurrentUsername_notAuthenticated() {
        assertThat(SecurityUtil.getCurrentUsername()).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId - 有认证信息时返回用户ID")
    void getCurrentUserId_authenticated() {
        UserDetails user = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, 1L, user.getAuthorities()));

        assertThat(SecurityUtil.getCurrentUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCurrentUserId - 无认证信息时返回null")
    void getCurrentUserId_notAuthenticated() {
        assertThat(SecurityUtil.getCurrentUserId()).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId - credentials非Long类型时返回null")
    void getCurrentUserId_nonLongCredentials() {
        UserDetails user = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "string-creds", user.getAuthorities()));

        assertThat(SecurityUtil.getCurrentUserId()).isNull();
    }
}
