package com.iflytek.admin.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.modules.system.entity.SysOperationLog;
import com.iflytek.admin.modules.system.mapper.SysOperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogAspect 操作日志切面测试")
class LogAspectTest {

    @InjectMocks
    private LogAspect logAspect;

    @Mock private SysOperationLogMapper logMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;
    @Mock private Log logAnnotation;

    @BeforeEach
    void setUp() {
        UserDetails user = User.withUsername("admin")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, 1L, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("正常执行 - 记录成功日志")
    void around_success() throws Throwable {
        when(logAnnotation.module()).thenReturn("用户管理");
        when(logAnnotation.operation()).thenReturn("新增");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("UserController");
        when(signature.getName()).thenReturn("create");
        when(joinPoint.proceed()).thenReturn("result");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Object result = logAspect.around(joinPoint, logAnnotation);

        assertThat(result).isEqualTo("result");

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(logMapper).insert(captor.capture());

        SysOperationLog log = captor.getValue();
        assertThat(log.getModule()).isEqualTo("用户管理");
        assertThat(log.getOperation()).isEqualTo("新增");
        assertThat(log.getStatus()).isEqualTo(1);
        assertThat(log.getResponseCode()).isEqualTo(200);
        assertThat(log.getMethod()).isEqualTo("UserController.create");
        assertThat(log.getRequestUrl()).isEqualTo("/api/users");
        assertThat(log.getRequestMethod()).isEqualTo("POST");
        assertThat(log.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("方法抛出异常 - 记录失败日志并重新抛出")
    void around_exception() throws Throwable {
        when(logAnnotation.module()).thenReturn("用户管理");
        when(logAnnotation.operation()).thenReturn("删除");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("UserController");
        when(signature.getName()).thenReturn("delete");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("删除失败"));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/users/1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> logAspect.around(joinPoint, logAnnotation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("删除失败");

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(logMapper).insert(captor.capture());

        SysOperationLog log = captor.getValue();
        assertThat(log.getStatus()).isEqualTo(0);
        assertThat(log.getErrorMsg()).isEqualTo("删除失败");
    }

    @Test
    @DisplayName("无请求上下文 - 不记录请求信息但仍记录日志")
    void around_noRequestContext() throws Throwable {
        when(logAnnotation.module()).thenReturn("系统");
        when(logAnnotation.operation()).thenReturn("测试");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("TestController");
        when(signature.getName()).thenReturn("test");
        when(joinPoint.proceed()).thenReturn(null);

        logAspect.around(joinPoint, logAnnotation);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(logMapper).insert(captor.capture());

        SysOperationLog log = captor.getValue();
        assertThat(log.getRequestUrl()).isNull();
        assertThat(log.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("日志保存失败 - 不影响业务方法执行")
    void around_logSaveFails() throws Throwable {
        when(logAnnotation.module()).thenReturn("系统");
        when(logAnnotation.operation()).thenReturn("测试");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("TestController");
        when(signature.getName()).thenReturn("test");
        when(joinPoint.proceed()).thenReturn("result");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // logMapper.insert throws exception
        doThrow(new RuntimeException("DB error")).when(logMapper).insert(any());

        Object result = logAspect.around(joinPoint, logAnnotation);

        // Business result should still be returned
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("正常执行 - 记录持续时间")
    void around_recordsDuration() throws Throwable {
        when(logAnnotation.module()).thenReturn("系统");
        when(logAnnotation.operation()).thenReturn("测试");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("TestController");
        when(signature.getName()).thenReturn("test");
        when(joinPoint.proceed()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        logAspect.around(joinPoint, logAnnotation);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(logMapper).insert(captor.capture());

        SysOperationLog log = captor.getValue();
        assertThat(log.getDuration()).isGreaterThanOrEqualTo(0);
        assertThat(log.getCreatedTime()).isNotNull();
        assertThat(log.getIp()).isNotNull();
    }
}
