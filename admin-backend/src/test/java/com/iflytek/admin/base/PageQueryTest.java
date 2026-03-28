package com.iflytek.admin.base;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageQuery 分页查询基类测试")
class PageQueryTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("默认值 - page=1, size=10")
    void defaultValues() {
        PageQuery query = new PageQuery();

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("有效值 - 无校验错误")
    void validValues() {
        PageQuery query = new PageQuery();
        query.setPage(5);
        query.setSize(50);

        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("page=0 - 校验失败")
    void page_lessThanMin() {
        PageQuery query = new PageQuery();
        query.setPage(0);

        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("页码最小为1");
    }

    @Test
    @DisplayName("size=0 - 校验失败")
    void size_lessThanMin() {
        PageQuery query = new PageQuery();
        query.setSize(0);

        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("size=101 - 超过最大值校验失败")
    void size_greaterThanMax() {
        PageQuery query = new PageQuery();
        query.setSize(101);

        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("每页条数最大为100");
    }

    @Test
    @DisplayName("size=100 - 边界值有效")
    void size_maxBoundary() {
        PageQuery query = new PageQuery();
        query.setSize(100);

        Set<ConstraintViolation<PageQuery>> violations = validator.validate(query);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Setter和Getter - 正确工作")
    void setterGetter() {
        PageQuery query = new PageQuery();
        query.setPage(3);
        query.setSize(25);

        assertThat(query.getPage()).isEqualTo(3);
        assertThat(query.getSize()).isEqualTo(25);
    }
}
