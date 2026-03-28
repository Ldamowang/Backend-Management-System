package com.iflytek.admin.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Result 响应封装测试")
class ResultTest {

    @Test
    @DisplayName("ok(data) - 返回200和数据")
    void okWithData() {
        Result<String> result = Result.ok("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("ok() - 返回200无数据")
    void okNoData() {
        Result<Void> result = Result.ok();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("fail(code, message) - 返回错误码和消息")
    void failWithCodeAndMessage() {
        Result<Void> result = Result.fail(400, "参数错误");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数错误");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("fail(ResultCode) - 使用枚举值")
    void failWithResultCode() {
        Result<Void> result = Result.fail(ResultCode.USER_NOT_FOUND);

        assertThat(result.getCode()).isEqualTo(40001);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
    }
}
