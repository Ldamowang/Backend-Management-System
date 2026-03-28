package com.iflytek.admin.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageResult 分页响应测试")
class PageResultTest {

    @Test
    @DisplayName("全参构造函数 - 正确设置字段")
    void allArgsConstructor() {
        List<String> data = List.of("item1", "item2");
        PageResult<String> result = new PageResult<>(data, 100L, 1, 10);

        assertThat(result.getList()).isEqualTo(data);
        assertThat(result.getTotal()).isEqualTo(100L);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("无参构造函数 - 字段为默认值")
    void noArgsConstructor() {
        PageResult<String> result = new PageResult<>();

        assertThat(result.getList()).isNull();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isZero();
    }

    @Test
    @DisplayName("Setter - 正确设置字段")
    void setters() {
        PageResult<String> result = new PageResult<>();
        result.setList(List.of("a"));
        result.setTotal(50);
        result.setPage(2);
        result.setSize(20);

        assertThat(result.getList()).containsExactly("a");
        assertThat(result.getTotal()).isEqualTo(50);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("空列表 - 总数为0")
    void emptyList() {
        PageResult<Object> result = new PageResult<>(List.of(), 0L, 1, 10);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }
}
