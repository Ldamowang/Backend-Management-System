package com.iflytek.admin.modules.profile.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Profile DTO 校验测试")
class ProfileDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("ProfileUpdateDTO 个人信息更新")
    class ProfileUpdateTests {

        @Test
        @DisplayName("有效数据 - 无校验错误")
        void valid() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setNickname("新昵称");
            dto.setEmail("test@example.com");
            dto.setPhone("13800138000");
            dto.setGender(1);

            Set<ConstraintViolation<ProfileUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("所有字段为null - 允许（可选更新）")
        void allNull() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();

            Set<ConstraintViolation<ProfileUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("邮箱格式错误 - 校验失败")
        void invalidEmail() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setEmail("not-an-email");

            Set<ConstraintViolation<ProfileUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("手机号格式错误 - 校验失败")
        void invalidPhone() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setPhone("123456");

            Set<ConstraintViolation<ProfileUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("昵称超长 - 校验失败")
        void nicknameTooLong() {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setNickname("a".repeat(51));

            Set<ConstraintViolation<ProfileUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("PasswordUpdateDTO 修改密码")
    class PasswordUpdateTests {

        @Test
        @DisplayName("有效数据 - 无校验错误")
        void valid() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass123");
            dto.setNewPassword("newpass123");

            Set<ConstraintViolation<PasswordUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("旧密码为空 - 校验失败")
        void oldPasswordBlank() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("");
            dto.setNewPassword("newpass123");

            Set<ConstraintViolation<PasswordUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("新密码为空 - 校验失败")
        void newPasswordBlank() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass");
            dto.setNewPassword("");

            Set<ConstraintViolation<PasswordUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("新密码太短 - 校验失败")
        void newPasswordTooShort() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass");
            dto.setNewPassword("12345");

            Set<ConstraintViolation<PasswordUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("新密码最小长度 - 有效")
        void newPasswordMinLength() {
            PasswordUpdateDTO dto = new PasswordUpdateDTO();
            dto.setOldPassword("oldpass");
            dto.setNewPassword("123456");

            Set<ConstraintViolation<PasswordUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }
}
