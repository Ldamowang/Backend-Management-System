package com.iflytek.admin.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultCode 响应码枚举测试")
class ResultCodeTest {

    @ParameterizedTest
    @EnumSource(ResultCode.class)
    @DisplayName("所有枚举值 - code和message不为空")
    void allResultCodes_haveCodeAndMessage(ResultCode code) {
        assertThat(code.getCode()).isPositive();
        assertThat(code.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("SUCCESS - code=200")
    void success() {
        assertThat(ResultCode.SUCCESS.getCode()).isEqualTo(200);
        assertThat(ResultCode.SUCCESS.getMessage()).isEqualTo("操作成功");
    }

    @Test
    @DisplayName("BAD_REQUEST - code=400")
    void badRequest() {
        assertThat(ResultCode.BAD_REQUEST.getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("UNAUTHORIZED - code=401")
    void unauthorized() {
        assertThat(ResultCode.UNAUTHORIZED.getCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("FORBIDDEN - code=403")
    void forbidden() {
        assertThat(ResultCode.FORBIDDEN.getCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("NOT_FOUND - code=404")
    void notFound() {
        assertThat(ResultCode.NOT_FOUND.getCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("INTERNAL_ERROR - code=500")
    void internalError() {
        assertThat(ResultCode.INTERNAL_ERROR.getCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("业务错误码 - 40001~40005")
    void businessCodes() {
        assertThat(ResultCode.USER_NOT_FOUND.getCode()).isEqualTo(40001);
        assertThat(ResultCode.USERNAME_EXISTS.getCode()).isEqualTo(40002);
        assertThat(ResultCode.WRONG_PASSWORD.getCode()).isEqualTo(40003);
        assertThat(ResultCode.TOKEN_EXPIRED.getCode()).isEqualTo(40004);
        assertThat(ResultCode.TOKEN_INVALID.getCode()).isEqualTo(40005);
    }

    @Test
    @DisplayName("RATE_LIMIT_EXCEEDED - code=429")
    void rateLimitExceeded() {
        assertThat(ResultCode.RATE_LIMIT_EXCEEDED.getCode()).isEqualTo(429);
    }

    @Test
    @DisplayName("枚举数量正确")
    void enumCount() {
        assertThat(ResultCode.values()).hasSize(13);
    }
}
