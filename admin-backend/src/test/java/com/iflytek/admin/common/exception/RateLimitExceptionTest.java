package com.iflytek.admin.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitException 限流异常测试")
class RateLimitExceptionTest {

    @Test
    @DisplayName("构造函数 - 正确设置message")
    void constructor() {
        RateLimitException ex = new RateLimitException("请求过于频繁，请稍后再试");

        assertThat(ex.getMessage()).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    @DisplayName("继承RuntimeException")
    void isRuntimeException() {
        RateLimitException ex = new RateLimitException("too many requests");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
