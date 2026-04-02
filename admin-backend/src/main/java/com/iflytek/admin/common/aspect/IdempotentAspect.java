package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口幂等性切面。
 * 基于 Redis Token 机制实现：客户端先获取 Token，请求时携带 Token，
 * 服务端原子删除 Token 确保请求只处理一次。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("X-Idempotent-Token");

        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        String key = CacheConstants.IDEMPOTENT_PREFIX + token;
        Boolean deleted = redisTemplate.delete(key);

        if (!Boolean.TRUE.equals(deleted)) {
            log.warn("幂等性校验失败：token={}, URI={}", token, request.getRequestURI());
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        return joinPoint.proceed();
    }
}
