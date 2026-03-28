package com.iflytek.admin.security;

import com.iflytek.admin.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 测试")
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无Authorization头 - 直接放行，不设置认证")
    void noAuthHeader_passThrough() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("无效Token - 直接放行")
    void invalidToken_passThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("有效Token - 设置认证信息")
    void validToken_setsAuthentication() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(redisTemplate.hasKey("token:blacklist:valid-token")).thenReturn(false);
        when(jwtUtil.getUsername("valid-token")).thenReturn("admin");
        when(jwtUtil.getUserId("valid-token")).thenReturn(1L);

        UserDetails userDetails = User.withUsername("admin")
                .password("pass")
                .authorities("sys:user:list")
                .build();
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Token在黑名单中 - 直接放行，不设置认证")
    void blacklistedToken_passThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer blacklisted-token");
        when(jwtUtil.isTokenValid("blacklisted-token")).thenReturn(true);
        when(redisTemplate.hasKey("token:blacklist:blacklisted-token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("非Bearer格式头 - 直接放行")
    void nonBearerHeader_passThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("RefreshToken类型 - 直接放行不设置认证")
    void refreshTokenType_passThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer refresh-token");
        when(jwtUtil.isTokenValid("refresh-token")).thenReturn(true);
        when(jwtUtil.getTokenType("refresh-token")).thenReturn("refresh");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("有效Token但username为null - 不设置认证")
    void validToken_nullUsername() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.getTokenType("valid-token")).thenReturn("access");
        when(redisTemplate.hasKey("token:blacklist:valid-token")).thenReturn(false);
        when(jwtUtil.getUsername("valid-token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
