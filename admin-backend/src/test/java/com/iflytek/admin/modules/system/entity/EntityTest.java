package com.iflytek.admin.modules.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entity 实体类测试")
class EntityTest {

    @Nested
    @DisplayName("SysUser 用户实体")
    class SysUserTests {

        @Test
        @DisplayName("所有字段 - Getter/Setter正确")
        void allFields() {
            SysUser user = new SysUser();
            LocalDateTime now = LocalDateTime.now();

            user.setUsername("admin");
            user.setPassword("encoded");
            user.setNickname("管理员");
            user.setEmail("admin@test.com");
            user.setPhone("13800138000");
            user.setAvatar("avatar.png");
            user.setGender(1);
            user.setStatus(1);
            user.setLastLoginTime(now);
            user.setLastLoginIp("127.0.0.1");

            assertThat(user.getUsername()).isEqualTo("admin");
            assertThat(user.getPassword()).isEqualTo("encoded");
            assertThat(user.getNickname()).isEqualTo("管理员");
            assertThat(user.getEmail()).isEqualTo("admin@test.com");
            assertThat(user.getPhone()).isEqualTo("13800138000");
            assertThat(user.getAvatar()).isEqualTo("avatar.png");
            assertThat(user.getGender()).isEqualTo(1);
            assertThat(user.getStatus()).isEqualTo(1);
            assertThat(user.getLastLoginTime()).isEqualTo(now);
            assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
        }
    }

    @Nested
    @DisplayName("SysRole 角色实体")
    class SysRoleTests {

        @Test
        @DisplayName("所有字段 - Getter/Setter正确")
        void allFields() {
            SysRole role = new SysRole();
            role.setRoleName("管理员");
            role.setRoleKey("admin");
            role.setSortOrder(1);
            role.setStatus(1);
            role.setDescription("系统管理员");

            assertThat(role.getRoleName()).isEqualTo("管理员");
            assertThat(role.getRoleKey()).isEqualTo("admin");
            assertThat(role.getSortOrder()).isEqualTo(1);
            assertThat(role.getStatus()).isEqualTo(1);
            assertThat(role.getDescription()).isEqualTo("系统管理员");
        }
    }

    @Nested
    @DisplayName("SysMenu 菜单实体")
    class SysMenuTests {

        @Test
        @DisplayName("所有字段 - Getter/Setter正确")
        void allFields() {
            SysMenu menu = new SysMenu();
            LocalDateTime now = LocalDateTime.now();

            menu.setId(1L);
            menu.setParentId(0L);
            menu.setMenuName("系统管理");
            menu.setMenuType(1);
            menu.setPath("/system");
            menu.setComponent("Layout");
            menu.setIcon("setting");
            menu.setSortOrder(1);
            menu.setPermission("sys:manage");
            menu.setVisible(1);
            menu.setStatus(1);
            menu.setCreatedTime(now);
            menu.setUpdatedTime(now);

            assertThat(menu.getId()).isEqualTo(1L);
            assertThat(menu.getParentId()).isZero();
            assertThat(menu.getMenuName()).isEqualTo("系统管理");
            assertThat(menu.getMenuType()).isEqualTo(1);
            assertThat(menu.getPath()).isEqualTo("/system");
            assertThat(menu.getComponent()).isEqualTo("Layout");
            assertThat(menu.getIcon()).isEqualTo("setting");
            assertThat(menu.getSortOrder()).isEqualTo(1);
            assertThat(menu.getPermission()).isEqualTo("sys:manage");
            assertThat(menu.getVisible()).isEqualTo(1);
            assertThat(menu.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("SysUserRole 用户角色关联")
    class SysUserRoleTests {

        @Test
        @DisplayName("所有字段")
        void allFields() {
            SysUserRole ur = new SysUserRole();
            ur.setId(1L);
            ur.setUserId(1L);
            ur.setRoleId(1L);

            assertThat(ur.getId()).isEqualTo(1L);
            assertThat(ur.getUserId()).isEqualTo(1L);
            assertThat(ur.getRoleId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("SysRoleMenu 角色菜单关联")
    class SysRoleMenuTests {

        @Test
        @DisplayName("所有字段")
        void allFields() {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setId(1L);
            rm.setRoleId(1L);
            rm.setMenuId(1L);

            assertThat(rm.getId()).isEqualTo(1L);
            assertThat(rm.getRoleId()).isEqualTo(1L);
            assertThat(rm.getMenuId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("SysOperationLog 操作日志")
    class SysOperationLogTests {

        @Test
        @DisplayName("所有字段")
        void allFields() {
            SysOperationLog log = new SysOperationLog();
            LocalDateTime now = LocalDateTime.now();

            log.setId(1L);
            log.setUserId(1L);
            log.setUsername("admin");
            log.setModule("用户管理");
            log.setOperation("新增");
            log.setMethod("UserController.create");
            log.setRequestUrl("/api/users");
            log.setRequestMethod("POST");
            log.setRequestParams("{\"username\":\"test\"}");
            log.setResponseCode(200);
            log.setResponseResult("ok");
            log.setIp("127.0.0.1");
            log.setDuration(100L);
            log.setStatus(1);
            log.setErrorMsg(null);
            log.setCreatedTime(now);

            assertThat(log.getModule()).isEqualTo("用户管理");
            assertThat(log.getOperation()).isEqualTo("新增");
            assertThat(log.getRequestUrl()).isEqualTo("/api/users");
            assertThat(log.getDuration()).isEqualTo(100L);
            assertThat(log.getStatus()).isEqualTo(1);
            assertThat(log.getErrorMsg()).isNull();
        }
    }

    @Nested
    @DisplayName("SysLoginLog 登录日志")
    class SysLoginLogTests {

        @Test
        @DisplayName("所有字段")
        void allFields() {
            SysLoginLog log = new SysLoginLog();
            LocalDateTime now = LocalDateTime.now();

            log.setId(1L);
            log.setUsername("admin");
            log.setIp("127.0.0.1");
            log.setLocation("内网");
            log.setBrowser("Chrome");
            log.setOs("macOS");
            log.setStatus(1);
            log.setMessage("登录成功");
            log.setLoginTime(now);

            assertThat(log.getUsername()).isEqualTo("admin");
            assertThat(log.getIp()).isEqualTo("127.0.0.1");
            assertThat(log.getBrowser()).isEqualTo("Chrome");
            assertThat(log.getStatus()).isEqualTo(1);
            assertThat(log.getMessage()).isEqualTo("登录成功");
            assertThat(log.getLoginTime()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("SysConfig 系统配置")
    class SysConfigTests {

        @Test
        @DisplayName("所有字段")
        void allFields() {
            SysConfig config = new SysConfig();
            LocalDateTime now = LocalDateTime.now();

            config.setId(1L);
            config.setConfigKey("site.name");
            config.setConfigValue("Admin System");
            config.setConfigName("站点名称");
            config.setConfigType(1);
            config.setDescription("站点配置");
            config.setCreatedTime(now);
            config.setUpdatedTime(now);

            assertThat(config.getConfigKey()).isEqualTo("site.name");
            assertThat(config.getConfigValue()).isEqualTo("Admin System");
            assertThat(config.getConfigName()).isEqualTo("站点名称");
            assertThat(config.getConfigType()).isEqualTo(1);
        }
    }
}
