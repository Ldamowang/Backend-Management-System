package com.iflytek.admin.modules.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Auth DTO 测试")
class AuthDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("LoginRequest 登录请求")
    class LoginRequestTests {

        @Test
        @DisplayName("有效请求 - 无校验错误")
        void valid() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword("admin123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("用户名为空 - 校验失败")
        void usernameBlank() {
            LoginRequest req = new LoginRequest();
            req.setUsername("");
            req.setPassword("admin123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("密码为空 - 校验失败")
        void passwordBlank() {
            LoginRequest req = new LoginRequest();
            req.setUsername("admin");
            req.setPassword("");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("用户名为null - 校验失败")
        void usernameNull() {
            LoginRequest req = new LoginRequest();
            req.setPassword("admin123");

            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("LoginResponse 登录响应")
    class LoginResponseTests {

        @Test
        @DisplayName("Builder构造 - 正确设置字段")
        void builder() {
            LoginResponse resp = LoginResponse.builder()
                    .accessToken("access")
                    .refreshToken("refresh")
                    .tokenType("Bearer")
                    .expiresIn(1800)
                    .build();

            assertThat(resp.getAccessToken()).isEqualTo("access");
            assertThat(resp.getRefreshToken()).isEqualTo("refresh");
            assertThat(resp.getTokenType()).isEqualTo("Bearer");
            assertThat(resp.getExpiresIn()).isEqualTo(1800);
        }

        @Test
        @DisplayName("默认tokenType - Bearer")
        void defaultTokenType() {
            LoginResponse resp = LoginResponse.builder()
                    .accessToken("access")
                    .build();

            assertThat(resp.getTokenType()).isEqualTo("Bearer");
        }
    }

    @Nested
    @DisplayName("RefreshTokenDTO 刷新Token请求")
    class RefreshTokenDTOTests {

        @Test
        @DisplayName("有效请求 - 无校验错误")
        void valid() {
            RefreshTokenDTO dto = new RefreshTokenDTO();
            dto.setRefreshToken("valid-token");

            Set<ConstraintViolation<RefreshTokenDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("refreshToken为空 - 校验失败")
        void blank() {
            RefreshTokenDTO dto = new RefreshTokenDTO();
            dto.setRefreshToken("");

            Set<ConstraintViolation<RefreshTokenDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }
}
