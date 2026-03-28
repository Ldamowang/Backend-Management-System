package com.iflytek.admin.common.exception;

import com.iflytek.admin.common.result.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException 业务异常测试")
class BusinessExceptionTest {

    @Test
    @DisplayName("code+message构造 - 正确设置")
    void constructor_codeAndMessage() {
        BusinessException ex = new BusinessException(40001, "用户不存在");

        assertThat(ex.getCode()).isEqualTo(40001);
        assertThat(ex.getMessage()).isEqualTo("用户不存在");
    }

    @Test
    @DisplayName("ResultCode构造 - 使用枚举值")
    void constructor_resultCode() {
        BusinessException ex = new BusinessException(ResultCode.USERNAME_EXISTS);

        assertThat(ex.getCode()).isEqualTo(40002);
        assertThat(ex.getMessage()).isEqualTo("用户名已存在");
    }

    @Test
    @DisplayName("继承RuntimeException - 可抛出")
    void isRuntimeException() {
        BusinessException ex = new BusinessException(500, "error");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("所有ResultCode枚举 - 正确构造")
    void allResultCodes() {
        for (ResultCode code : ResultCode.values()) {
            BusinessException ex = new BusinessException(code);
            assertThat(ex.getCode()).isEqualTo(code.getCode());
            assertThat(ex.getMessage()).isEqualTo(code.getMessage());
        }
    }
}
