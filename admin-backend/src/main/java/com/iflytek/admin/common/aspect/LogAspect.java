package com.iflytek.admin.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.system.entity.SysOperationLog;
import com.iflytek.admin.modules.system.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
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
    private final SqlSessionFactory sqlSessionFactory;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint point, Log logAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog opLog = new SysOperationLog();
        opLog.setModule(logAnnotation.module());
        opLog.setOperation(logAnnotation.operation());
        opLog.setUsername(SecurityUtil.getCurrentUsername());

        MethodSignature signature = (MethodSignature) point.getSignature();
        opLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        String targetType = logAnnotation.targetType();
        Long targetId = null;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setRequestMethod(request.getMethod());
            opLog.setIp(request.getRemoteAddr());

            targetId = extractTargetId(request.getRequestURI());
        }

        // Set target info on the log entry
        if (!targetType.isEmpty()) {
            opLog.setTargetType(targetType);
        }
        if (targetId != null) {
            opLog.setTargetId(targetId);
        }

        // Capture before data for PUT/DELETE with valid target
        String requestMethod = opLog.getRequestMethod();
        if (!targetType.isEmpty() && targetId != null
                && ("PUT".equalsIgnoreCase(requestMethod) || "DELETE".equalsIgnoreCase(requestMethod))) {
            try {
                Object beforeObj = queryTargetById(targetType, targetId);
                if (beforeObj != null) {
                    opLog.setBeforeData(objectMapper.writeValueAsString(beforeObj));
                }
            } catch (Exception e) {
                log.warn("Failed to capture before data for audit log", e);
            }
        }

        try {
            Object result = point.proceed();
            opLog.setStatus(1);
            opLog.setResponseCode(200);

            // Capture after data for PUT with valid target
            if (!targetType.isEmpty() && targetId != null && "PUT".equalsIgnoreCase(requestMethod)) {
                try {
                    Object afterObj = queryTargetById(targetType, targetId);
                    if (afterObj != null) {
                        opLog.setAfterData(objectMapper.writeValueAsString(afterObj));
                    }
                } catch (Exception e) {
                    log.warn("Failed to capture after data for audit log", e);
                }
            }

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

    private Object queryTargetById(String targetType, Long id) {
        String statement = "com.iflytek.admin.modules.system.mapper.Sys"
                + targetType.substring(0, 1).toUpperCase() + targetType.substring(1)
                + "Mapper.selectById";
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectOne(statement, id);
        } catch (Exception e) {
            log.debug("Failed to query target {}:{} - {}", targetType, id, e.getMessage());
            return null;
        }
    }

    private Long extractTargetId(String uri) {
        if (uri == null) return null;
        String[] parts = uri.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
