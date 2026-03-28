package com.iflytek.admin.common.exception;

import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler 全局异常处理测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException - 返回业务错误码和消息")
    void handleBusinessException() {
        BusinessException ex = new BusinessException(40001, "用户不存在");

        Result<Void> result = handler.handleBusinessException(ex);

        assertThat(result.getCode()).isEqualTo(40001);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("BusinessException(ResultCode) - 使用枚举值")
    void handleBusinessException_withResultCode() {
        BusinessException ex = new BusinessException(ResultCode.USERNAME_EXISTS);

        Result<Void> result = handler.handleBusinessException(ex);

        assertThat(result.getCode()).isEqualTo(40002);
        assertThat(result.getMessage()).isEqualTo("用户名已存在");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException - 返回400和字段错误")
    void handleValidationException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "username", "用户名不能为空");
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Result<Void> result = handler.handleValidationException(ex);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("用户名不能为空");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 无FieldError - 返回默认消息")
    void handleValidationException_noFieldError() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldError()).thenReturn(null);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Result<Void> result = handler.handleValidationException(ex);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数校验失败");
    }

    @Test
    @DisplayName("ConstraintViolationException - 返回400")
    void handleConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数错误");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        Result<Void> result = handler.handleConstraintViolation(ex);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数错误");
    }

    @Test
    @DisplayName("BadCredentialsException - 返回401")
    void handleBadCredentials() {
        Result<Void> result = handler.handleBadCredentials(
                new BadCredentialsException("Bad credentials"));

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("用户名或密码错误");
    }

    @Test
    @DisplayName("AccessDeniedException - 返回403")
    void handleAccessDenied() {
        Result<Void> result = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden"));

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("无权限访问");
    }

    @Test
    @DisplayName("RateLimitException - 返回429")
    void handleRateLimitException() {
        Result<Void> result = handler.handleRateLimitException(
                new RateLimitException("请求过于频繁，请稍后再试"));

        assertThat(result.getCode()).isEqualTo(429);
        assertThat(result.getMessage()).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    @DisplayName("未知异常 - 返回500")
    void handleException() {
        Result<Void> result = handler.handleException(new RuntimeException("unexpected"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("服务器内部错误");
    }
}
