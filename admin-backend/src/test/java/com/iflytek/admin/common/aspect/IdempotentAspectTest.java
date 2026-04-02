package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.ResultCode;
import org.aspectj.lang.ProceedingJoinPoint;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotentAspect 幂等性切面测试")
class IdempotentAspectTest {

    @InjectMocks
    private IdempotentAspect idempotentAspect;

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ProceedingJoinPoint joinPoint;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("POST", "/api/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("缺少幂等Token请求头 - 抛出BusinessException(DUPLICATE_SUBMIT)")
    void around_missingTokenHeader_throwsBusinessException() {
        Idempotent idempotent = mock(Idempotent.class);

        assertThatThrownBy(() -> idempotentAspect.around(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResultCode.DUPLICATE_SUBMIT.getMessage())
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.DUPLICATE_SUBMIT.getCode());
    }

    @Test
    @DisplayName("有效Token(首次提交,Redis DELETE返回true) - 正常放行")
    void around_validToken_proceedsNormally() throws Throwable {
        Idempotent idempotent = mock(Idempotent.class);
        request.addHeader("X-Idempotent-Token", "test-token-123");
        when(redisTemplate.delete(contains("test-token-123"))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = idempotentAspect.around(joinPoint, idempotent);

        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
        verify(redisTemplate).delete(contains("test-token-123"));
    }

    @Test
    @DisplayName("无效Token(重复提交,Redis DELETE返回false) - 抛出BusinessException(DUPLICATE_SUBMIT)")
    void around_invalidToken_duplicateSubmit_throwsBusinessException() throws Throwable {
        Idempotent idempotent = mock(Idempotent.class);
        request.addHeader("X-Idempotent-Token", "expired-token");
        when(redisTemplate.delete(contains("expired-token"))).thenReturn(false);

        assertThatThrownBy(() -> idempotentAspect.around(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResultCode.DUPLICATE_SUBMIT.getMessage())
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.DUPLICATE_SUBMIT.getCode());

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("无效Token(Redis DELETE返回null) - 抛出BusinessException(DUPLICATE_SUBMIT)")
    void around_invalidToken_nullDelete_throwsBusinessException() throws Throwable {
        Idempotent idempotent = mock(Idempotent.class);
        request.addHeader("X-Idempotent-Token", "null-token");
        when(redisTemplate.delete(contains("null-token"))).thenReturn(null);

        assertThatThrownBy(() -> idempotentAspect.around(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResultCode.DUPLICATE_SUBMIT.getMessage());

        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("无请求上下文 - 抛出BusinessException(DUPLICATE_SUBMIT)")
    void around_noRequestContext_throwsBusinessException() {
        RequestContextHolder.resetRequestAttributes();
        Idempotent idempotent = mock(Idempotent.class);

        assertThatThrownBy(() -> idempotentAspect.around(joinPoint, idempotent))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ResultCode.DUPLICATE_SUBMIT.getMessage());
    }
}
