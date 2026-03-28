package com.iflytek.admin.modules.system.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO 校验注解测试")
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("UserCreateDTO 校验")
    class UserCreateDTOTests {

        @Test
        @DisplayName("有效数据 - 无违规")
        void valid() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("password123");
            dto.setNickname("测试");
            dto.setEmail("test@example.com");
            dto.setPhone("13800138000");

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("用户名为空 - 校验失败")
        void usernameBlank() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("");
            dto.setPassword("password123");

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("密码太短 - 校验失败")
        void passwordTooShort() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("12345");

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("邮箱格式错误 - 校验失败")
        void invalidEmail() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("password123");
            dto.setEmail("not-an-email");

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("手机号格式错误 - 校验失败")
        void invalidPhone() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("password123");
            dto.setPhone("12345");

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
        }

        @Test
        @DisplayName("可选字段为null - 校验通过")
        void optionalFieldsNull() {
            UserCreateDTO dto = new UserCreateDTO();
            dto.setUsername("testuser");
            dto.setPassword("password123");
            // email, phone, nickname 都是 null

            Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("RoleFormDTO 校验")
    class RoleFormDTOTests {

        @Test
        @DisplayName("有效数据 - 无违规")
        void valid() {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("管理员");
            dto.setRoleKey("admin");

            Set<ConstraintViolation<RoleFormDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("角色名为空 - 校验失败")
        void roleNameBlank() {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("");
            dto.setRoleKey("admin");

            Set<ConstraintViolation<RoleFormDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("roleName"));
        }

        @Test
        @DisplayName("角色标识为空 - 校验失败")
        void roleKeyBlank() {
            RoleFormDTO dto = new RoleFormDTO();
            dto.setRoleName("管理员");
            dto.setRoleKey("");

            Set<ConstraintViolation<RoleFormDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("roleKey"));
        }
    }

    @Nested
    @DisplayName("MenuFormDTO 校验")
    class MenuFormDTOTests {

        @Test
        @DisplayName("有效数据 - 无违规")
        void valid() {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("用户管理");
            dto.setMenuType(2);
            dto.setPath("/system/user");

            Set<ConstraintViolation<MenuFormDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("菜单名为空 - 校验失败")
        void menuNameBlank() {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("");
            dto.setMenuType(2);

            Set<ConstraintViolation<MenuFormDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("menuName"));
        }

        @Test
        @DisplayName("菜单类型为null - 校验失败")
        void menuTypeNull() {
            MenuFormDTO dto = new MenuFormDTO();
            dto.setMenuName("测试菜单");
            dto.setMenuType(null);

            Set<ConstraintViolation<MenuFormDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("menuType"));
        }
    }

    @Nested
    @DisplayName("UserQueryDTO 校验")
    class UserQueryDTOTests {

        @Test
        @DisplayName("有效查询 - 无违规")
        void valid() {
            UserQueryDTO dto = new UserQueryDTO();
            dto.setUsername("admin");
            dto.setEmail("admin@test.com");
            dto.setStatus(1);
            dto.setPage(1);
            dto.setSize(10);

            Set<ConstraintViolation<UserQueryDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("所有字段为null - 校验通过（可选条件）")
        void allNull() {
            UserQueryDTO dto = new UserQueryDTO();

            Set<ConstraintViolation<UserQueryDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("邮箱格式错误 - 校验失败")
        void invalidEmail() {
            UserQueryDTO dto = new UserQueryDTO();
            dto.setEmail("not-email");

            Set<ConstraintViolation<UserQueryDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("状态值超出范围 - 校验失败")
        void statusOutOfRange() {
            UserQueryDTO dto = new UserQueryDTO();
            dto.setStatus(3);

            Set<ConstraintViolation<UserQueryDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("用户名超长 - 校验失败")
        void usernameTooLong() {
            UserQueryDTO dto = new UserQueryDTO();
            dto.setUsername("a".repeat(51));

            Set<ConstraintViolation<UserQueryDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("UserUpdateDTO 校验")
    class UserUpdateDTOTests {

        @Test
        @DisplayName("有效更新 - 无违规")
        void valid() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("新昵称");
            dto.setEmail("new@test.com");
            dto.setPhone("13800138000");

            Set<ConstraintViolation<UserUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("所有字段为null - 校验通过（部分更新）")
        void allNull() {
            UserUpdateDTO dto = new UserUpdateDTO();

            Set<ConstraintViolation<UserUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("邮箱格式错误 - 校验失败")
        void invalidEmail() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setEmail("bad");

            Set<ConstraintViolation<UserUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("AssignMenusDTO 校验")
    class AssignMenusDTOTests {

        @Test
        @DisplayName("有效数据 - 无违规")
        void valid() {
            AssignMenusDTO dto = new AssignMenusDTO();
            dto.setMenuIds(java.util.List.of(1L, 2L));

            Set<ConstraintViolation<AssignMenusDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("menuIds为null - 校验失败")
        void menuIdsNull() {
            AssignMenusDTO dto = new AssignMenusDTO();

            Set<ConstraintViolation<AssignMenusDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("menuIds"));
        }
    }

    @Nested
    @DisplayName("StatusDTO 校验")
    class StatusDTOTests {

        @Test
        @DisplayName("状态为null - 校验失败")
        void statusNull() {
            StatusDTO dto = new StatusDTO();

            Set<ConstraintViolation<StatusDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
        }

        @Test
        @DisplayName("状态超出范围 - 校验失败")
        void statusOutOfRange() {
            StatusDTO dto = new StatusDTO();
            dto.setStatus(5);

            Set<ConstraintViolation<StatusDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
        }

        @Test
        @DisplayName("状态值0 - 校验通过")
        void statusZero() {
            StatusDTO dto = new StatusDTO();
            dto.setStatus(0);

            Set<ConstraintViolation<StatusDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }
}
