package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.RateLimiter;
import com.iflytek.admin.common.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面。
 * 基于 Redis INCR + EXPIRE 实现固定窗口限流，限流 key 为 IP + 接口路径。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) {
        int count = rateLimiter.count();
        int time = rateLimiter.time();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = getClientIp(request);
        String uri = request.getRequestURI();

        String key = RATE_LIMIT_KEY_PREFIX + ip + ":" + uri;

        Long currentCount = redisTemplate.opsForValue().increment(key, 1);
        if (currentCount == null) {
            return;
        }

        // 第一次请求时设置过期时间
        if (currentCount == 1) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
        }

        if (currentCount > count) {
            log.warn("接口限流：IP={}, URI={}, 当前请求次数={}, 限流阈值={}/{}s",
                    ip, uri, currentCount, count, time);
            throw new RateLimitException(rateLimiter.message());
        }
    }

    /**
     * 获取客户端真实 IP。
     * 优先从代理头中获取，支持 Nginx 等反向代理场景。
     */
    /**
     * 获取客户端真实 IP。
     * 优先信任 Nginx 设置的 X-Real-IP（不可被客户端伪造），
     * 回退到 remoteAddr，不信任客户端可控的 X-Forwarded-For。
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
