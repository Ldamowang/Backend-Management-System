package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.RateLimiter;
import com.iflytek.admin.common.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterAspect 限流切面测试")
class RateLimiterAspectTest {

    @InjectMocks
    private RateLimiterAspect rateLimiterAspect;

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private JoinPoint joinPoint;
    @Mock private RateLimiter rateLimiter;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("首次请求 - 放行并设置过期时间")
    void doBefore_firstRequest() {
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(1L);

        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        verify(redisTemplate).expire(anyString(), eq(60L), any());
    }

    @Test
    @DisplayName("未超限 - 正常放行")
    void doBefore_withinLimit() {
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(3L);

        assertThatCode(() -> rateLimiterAspect.doBefore(joinPoint, rateLimiter))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("超过限制 - 抛出 RateLimitException")
    void doBefore_exceedsLimit() {
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(rateLimiter.message()).thenReturn("请求过于频繁");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(6L);

        assertThatThrownBy(() -> rateLimiterAspect.doBefore(joinPoint, rateLimiter))
                .isInstanceOf(RateLimitException.class)
                .hasMessage("请求过于频繁");
    }

    @Test
    @DisplayName("无请求上下文 - 直接返回不限流")
    void doBefore_noRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        assertThatCode(() -> rateLimiterAspect.doBefore(joinPoint, rateLimiter))
                .doesNotThrowAnyException();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("X-Forwarded-For 头 - 使用代理IP")
    void doBefore_xForwardedFor() {
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("192.168.1.1"), eq(1L))).thenReturn(1L);

        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        verify(valueOperations).increment(contains("192.168.1.1"), eq(1L));
    }

    @Test
    @DisplayName("X-Real-IP 头 - 使用真实IP")
    void doBefore_xRealIp() {
        request.addHeader("X-Real-IP", "10.0.0.5");
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("10.0.0.5"), eq(1L))).thenReturn(1L);

        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        verify(valueOperations).increment(contains("10.0.0.5"), eq(1L));
    }

    @Test
    @DisplayName("increment返回null - 直接返回不限流")
    void doBefore_nullIncrement() {
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(null);

        assertThatCode(() -> rateLimiterAspect.doBefore(joinPoint, rateLimiter))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("X-Forwarded-For 为 unknown - 回退到remoteAddr")
    void doBefore_unknownForwardedFor() {
        request.addHeader("X-Forwarded-For", "unknown");
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(contains("127.0.0.1"), eq(1L))).thenReturn(1L);

        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        verify(valueOperations).increment(contains("127.0.0.1"), eq(1L));
    }
}
