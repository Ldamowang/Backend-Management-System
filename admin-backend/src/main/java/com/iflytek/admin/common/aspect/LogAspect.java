package com.iflytek.admin.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.system.entity.SysOperationLog;
import com.iflytek.admin.modules.system.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog opLog = new SysOperationLog();
        opLog.setModule(logAnnotation.module());
        opLog.setOperation(logAnnotation.operation());
        opLog.setUsername(SecurityUtil.getCurrentUsername());

        MethodSignature signature = (MethodSignature) point.getSignature();
        opLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setRequestMethod(request.getMethod());
            opLog.setIp(request.getRemoteAddr());
        }

        try {
            Object result = point.proceed();
            opLog.setStatus(1);
            opLog.setResponseCode(200);
            return result;
        } catch (Exception e) {
            opLog.setStatus(0);
            opLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            opLog.setDuration(System.currentTimeMillis() - startTime);
            opLog.setCreatedTime(LocalDateTime.now());
            try {
                logMapper.insert(opLog);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }
    }
}
