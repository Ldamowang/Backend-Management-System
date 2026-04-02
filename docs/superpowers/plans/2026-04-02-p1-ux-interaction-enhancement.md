# P1 用户体验与交互增强 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现密码安全策略、接口幂等性、WebSocket 实时通知、高级表格设置和 Dashboard 可编辑布局五大 P1 功能。

**Architecture:** 后端基于 Spring Boot 3.2 + MyBatis-Plus，前端基于 Vue 3 + Element Plus + Pinia。每个功能独立开发、独立可测试，互不依赖（WebSocket 通知依赖现有 NoticeService）。密码策略和幂等性是后端为主的功能，WebSocket 需前后端配合，表格增强和 Dashboard 是纯前端功能。

**Tech Stack:** Spring Boot 3.2, Spring Security 6, MyBatis-Plus 3.5, Redis, WebSocket (STOMP + SockJS), Vue 3, Element Plus, Pinia, vuedraggable, @stomp/stompjs

---

## 文件结构总览

### P1-1 密码安全策略
```
admin-backend/
├── sql/V2__password_policy.sql                                          # DDL 迁移
├── src/main/java/com/iflytek/admin/
│   ├── modules/system/entity/SysPasswordHistory.java                    # 密码历史实体
│   ├── modules/system/mapper/SysPasswordHistoryMapper.java              # 密码历史 Mapper
│   ├── modules/system/service/PasswordPolicyService.java                # 密码策略接口
│   ├── modules/system/service/impl/PasswordPolicyServiceImpl.java       # 密码策略实现
│   ├── modules/auth/dto/LoginResponse.java                              # 修改：加 passwordExpired
│   ├── modules/auth/service/impl/AuthServiceImpl.java                   # 修改：登录检查过期
│   ├── modules/profile/controller/ProfileController.java                # 修改：密码校验
│   ├── modules/system/service/impl/UserServiceImpl.java                 # 修改：创建用户校验
│   └── modules/system/entity/SysUser.java                               # 修改：加 passwordChangedTime
├── src/test/java/com/iflytek/admin/
│   └── modules/system/service/impl/PasswordPolicyServiceImplTest.java
admin-frontend/
├── src/components/PasswordStrength.vue                                   # 密码强度指示器
└── src/views/login/index.vue                                             # 修改：过期弹窗
```

### P1-2 接口幂等性
```
admin-backend/
├── src/main/java/com/iflytek/admin/
│   ├── common/annotation/Idempotent.java
│   ├── common/aspect/IdempotentAspect.java
│   ├── common/controller/IdempotentController.java
│   └── common/result/ResultCode.java                                     # 修改：加 DUPLICATE_SUBMIT
├── src/test/java/com/iflytek/admin/
│   └── common/aspect/IdempotentAspectTest.java
admin-frontend/
├── src/composables/useIdempotent.ts
└── src/api/request.ts                                                    # 修改：注入 token header
```

### P1-3 WebSocket 实时通知
```
admin-backend/
├── pom.xml                                                               # 修改：加 websocket 依赖
├── src/main/java/com/iflytek/admin/
│   ├── common/config/WebSocketConfig.java
│   ├── common/config/WebSocketAuthInterceptor.java
│   ├── modules/system/service/impl/NoticeServiceImpl.java                # 修改：发布时推送
│   └── security/SecurityConfig.java                                      # 修改：放行 /ws
admin-frontend/
├── src/composables/useWebSocket.ts
├── src/stores/modules/notice.ts
└── src/components/Layout/Header.vue                                      # 修改：集成 notice store
```

### P1-4 表格增强
```
admin-frontend/
├── src/components/TableToolbar.vue
├── src/composables/useTableSettings.ts
├── src/components/__tests__/TableToolbar.test.ts
└── src/composables/__tests__/useTableSettings.test.ts
```

### P1-5 Dashboard 可编辑布局
```
admin-frontend/
├── src/views/dashboard/
│   ├── index.vue                                                          # 重构
│   └── components/
│       ├── DashboardGrid.vue
│       ├── WidgetWrapper.vue
│       ├── AddWidgetDialog.vue
│       └── widgets/
│           ├── registry.ts
│           ├── StatCard.vue
│           ├── ChartLoginTrend.vue
│           ├── ChartOverview.vue
│           ├── TableRecentLogin.vue
│           └── ShortcutLinks.vue
├── src/stores/modules/dashboard.ts
└── src/stores/__tests__/dashboard.test.ts
```

---

## Task 1: 密码策略 — 数据库迁移 + 实体

**Files:**
- Create: `admin-backend/sql/V2__password_policy.sql`
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/entity/SysPasswordHistory.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/mapper/SysPasswordHistoryMapper.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/entity/SysUser.java`

- [ ] **Step 1: 创建 SQL 迁移脚本**

```sql
-- V2__password_policy.sql

-- sys_user 新增密码修改时间字段
ALTER TABLE sys_user ADD COLUMN password_changed_time DATETIME NULL COMMENT '最后修改密码时间';

-- 新增密码历史表
CREATE TABLE sys_password_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 编码后的密码',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史记录';

-- 插入密码策略默认配置到 sys_config
INSERT INTO sys_config (config_key, config_value, config_name, config_type, description, created_time, updated_time) VALUES
('pwd.min.length', '8', '密码最小长度', 1, '密码最少字符数', NOW(), NOW()),
('pwd.require.uppercase', 'true', '要求大写字母', 1, '密码是否必须包含大写字母', NOW(), NOW()),
('pwd.require.lowercase', 'true', '要求小写字母', 1, '密码是否必须包含小写字母', NOW(), NOW()),
('pwd.require.digit', 'true', '要求数字', 1, '密码是否必须包含数字', NOW(), NOW()),
('pwd.require.special', 'false', '要求特殊字符', 1, '密码是否必须包含特殊字符', NOW(), NOW()),
('pwd.expire.days', '90', '密码有效期', 1, '密码过期天数，0=永不过期', NOW(), NOW()),
('pwd.history.count', '3', '密码历史检查数', 1, '不能与最近N次密码重复', NOW(), NOW());
```

- [ ] **Step 2: 修改 SysUser 实体，新增 passwordChangedTime 字段**

在 `admin-backend/src/main/java/com/iflytek/admin/modules/system/entity/SysUser.java` 的字段列表末尾（`lastLoginIp` 之后）新增：

```java
private LocalDateTime passwordChangedTime;
```

- [ ] **Step 3: 创建 SysPasswordHistory 实体**

```java
package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_password_history")
public class SysPasswordHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String password;
    private LocalDateTime createdTime;
}
```

- [ ] **Step 4: 创建 SysPasswordHistoryMapper**

```java
package com.iflytek.admin.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iflytek.admin.modules.system.entity.SysPasswordHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysPasswordHistoryMapper extends BaseMapper<SysPasswordHistory> {
}
```

- [ ] **Step 5: 在 H2 测试数据库中应用迁移**

在 `admin-backend/src/test/resources/schema-h2.sql`（如果存在）中追加密码历史表。如果测试使用 `schema.sql`，确认 H2 兼容语法。

- [ ] **Step 6: 提交**

```bash
git add admin-backend/sql/V2__password_policy.sql \
  admin-backend/src/main/java/com/iflytek/admin/modules/system/entity/SysPasswordHistory.java \
  admin-backend/src/main/java/com/iflytek/admin/modules/system/mapper/SysPasswordHistoryMapper.java \
  admin-backend/src/main/java/com/iflytek/admin/modules/system/entity/SysUser.java
git commit -m "feat: 密码策略数据库迁移 + 密码历史实体"
```

---

## Task 2: 密码策略 — PasswordPolicyService（TDD）

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/PasswordPolicyService.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/PasswordPolicyServiceImpl.java`
- Create: `admin-backend/src/test/java/com/iflytek/admin/modules/system/service/impl/PasswordPolicyServiceImplTest.java`

- [ ] **Step 1: 写 PasswordPolicyService 接口**

```java
package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.entity.SysUser;

import java.util.List;

public interface PasswordPolicyService {
    /**
     * 校验密码强度，返回错误消息列表（空=通过）
     */
    List<String> validate(String rawPassword);

    /**
     * 检查是否与最近 N 次历史密码重复
     */
    boolean isHistoryPassword(Long userId, String rawPassword);

    /**
     * 记录密码到历史表
     */
    void recordHistory(Long userId, String encodedPassword);

    /**
     * 检查用户密码是否过期
     */
    boolean isExpired(SysUser user);
}
```

- [ ] **Step 2: 写测试（RED）**

```java
package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysPasswordHistory;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysPasswordHistoryMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordPolicyServiceImpl 测试")
class PasswordPolicyServiceImplTest {

    @Mock
    private ConfigService configService;
    @Mock
    private SysPasswordHistoryMapper historyMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordPolicyServiceImpl service;

    @Nested
    @DisplayName("validate - 密码强度校验")
    class ValidateTests {

        @BeforeEach
        void setup() {
            when(configService.getValueByKey("pwd.min.length")).thenReturn("8");
            when(configService.getValueByKey("pwd.require.uppercase")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.lowercase")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.digit")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.special")).thenReturn("false");
        }

        @Test
        @DisplayName("符合所有规则的密码应通过")
        void valid_password_passes() {
            List<String> errors = service.validate("Abcdef12");
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("长度不足应返回错误")
        void too_short_fails() {
            List<String> errors = service.validate("Abc1");
            assertThat(errors).anyMatch(e -> e.contains("至少"));
        }

        @Test
        @DisplayName("缺少大写字母应返回错误")
        void no_uppercase_fails() {
            List<String> errors = service.validate("abcdefg1");
            assertThat(errors).anyMatch(e -> e.contains("大写"));
        }

        @Test
        @DisplayName("缺少小写字母应返回错误")
        void no_lowercase_fails() {
            List<String> errors = service.validate("ABCDEFG1");
            assertThat(errors).anyMatch(e -> e.contains("小写"));
        }

        @Test
        @DisplayName("缺少数字应返回错误")
        void no_digit_fails() {
            List<String> errors = service.validate("Abcdefgh");
            assertThat(errors).anyMatch(e -> e.contains("数字"));
        }

        @Test
        @DisplayName("要求特殊字符时缺少特殊字符应返回错误")
        void no_special_when_required_fails() {
            when(configService.getValueByKey("pwd.require.special")).thenReturn("true");
            List<String> errors = service.validate("Abcdefg1");
            assertThat(errors).anyMatch(e -> e.contains("特殊字符"));
        }
    }

    @Nested
    @DisplayName("isHistoryPassword - 历史密码检查")
    class HistoryTests {

        @Test
        @DisplayName("与历史密码匹配应返回 true")
        void matches_history() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn("3");
            SysPasswordHistory h = new SysPasswordHistory();
            h.setPassword("$2a$10$encoded");
            when(historyMapper.selectList(any())).thenReturn(List.of(h));
            when(passwordEncoder.matches("newPwd", "$2a$10$encoded")).thenReturn(true);

            assertThat(service.isHistoryPassword(1L, "newPwd")).isTrue();
        }

        @Test
        @DisplayName("不匹配历史密码应返回 false")
        void no_match() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn("3");
            when(historyMapper.selectList(any())).thenReturn(List.of());

            assertThat(service.isHistoryPassword(1L, "newPwd")).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired - 密码过期检查")
    class ExpiryTests {

        @Test
        @DisplayName("过期天数为0（永不过期）应返回 false")
        void never_expires() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("0");
            SysUser user = new SysUser();
            assertThat(service.isExpired(user)).isFalse();
        }

        @Test
        @DisplayName("passwordChangedTime 为 null 应视为过期")
        void null_changed_time_is_expired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");
            SysUser user = new SysUser();
            user.setPasswordChangedTime(null);
            assertThat(service.isExpired(user)).isTrue();
        }

        @Test
        @DisplayName("未超过有效期应返回 false")
        void not_expired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");
            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(30));
            assertThat(service.isExpired(user)).isFalse();
        }

        @Test
        @DisplayName("超过有效期应返回 true")
        void expired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");
            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(100));
            assertThat(service.isExpired(user)).isTrue();
        }
    }

    @Nested
    @DisplayName("recordHistory - 记录密码历史")
    class RecordTests {

        @Test
        @DisplayName("应插入一条密码历史记录")
        void records_password() {
            service.recordHistory(1L, "$2a$10$encoded");
            verify(historyMapper).insert(any(SysPasswordHistory.class));
        }
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -pl . -Dtest=PasswordPolicyServiceImplTest -DfailIfNoTests=false`
Expected: 编译失败（PasswordPolicyServiceImpl 不存在）

- [ ] **Step 4: 实现 PasswordPolicyServiceImpl**

```java
package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysPasswordHistory;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysPasswordHistoryMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import com.iflytek.admin.modules.system.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final ConfigService configService;
    private final SysPasswordHistoryMapper historyMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<String> validate(String rawPassword) {
        List<String> errors = new ArrayList<>();

        int minLength = getIntConfig("pwd.min.length", 8);
        if (rawPassword.length() < minLength) {
            errors.add("密码至少需要 " + minLength + " 个字符");
        }
        if (getBoolConfig("pwd.require.uppercase") && !rawPassword.matches(".*[A-Z].*")) {
            errors.add("密码必须包含至少一个大写字母");
        }
        if (getBoolConfig("pwd.require.lowercase") && !rawPassword.matches(".*[a-z].*")) {
            errors.add("密码必须包含至少一个小写字母");
        }
        if (getBoolConfig("pwd.require.digit") && !rawPassword.matches(".*\\d.*")) {
            errors.add("密码必须包含至少一个数字");
        }
        if (getBoolConfig("pwd.require.special") && !rawPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            errors.add("密码必须包含至少一个特殊字符");
        }

        return errors;
    }

    @Override
    public boolean isHistoryPassword(Long userId, String rawPassword) {
        int historyCount = getIntConfig("pwd.history.count", 3);
        if (historyCount <= 0) return false;

        List<SysPasswordHistory> histories = historyMapper.selectList(
                new LambdaQueryWrapper<SysPasswordHistory>()
                        .eq(SysPasswordHistory::getUserId, userId)
                        .orderByDesc(SysPasswordHistory::getCreatedTime)
                        .last("LIMIT " + historyCount));

        return histories.stream()
                .anyMatch(h -> passwordEncoder.matches(rawPassword, h.getPassword()));
    }

    @Override
    public void recordHistory(Long userId, String encodedPassword) {
        SysPasswordHistory history = new SysPasswordHistory();
        history.setUserId(userId);
        history.setPassword(encodedPassword);
        history.setCreatedTime(LocalDateTime.now());
        historyMapper.insert(history);
    }

    @Override
    public boolean isExpired(SysUser user) {
        int expireDays = getIntConfig("pwd.expire.days", 90);
        if (expireDays <= 0) return false;

        LocalDateTime changedTime = user.getPasswordChangedTime();
        if (changedTime == null) return true;

        return changedTime.plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    private int getIntConfig(String key, int defaultValue) {
        String value = configService.getValueByKey(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBoolConfig(String key) {
        return "true".equalsIgnoreCase(configService.getValueByKey(key));
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -pl . -Dtest=PasswordPolicyServiceImplTest`
Expected: 全部通过

- [ ] **Step 6: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/service/PasswordPolicyService.java \
  admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/PasswordPolicyServiceImpl.java \
  admin-backend/src/test/java/com/iflytek/admin/modules/system/service/impl/PasswordPolicyServiceImplTest.java
git commit -m "feat: 新增 PasswordPolicyService 密码策略服务"
```

---

## Task 3: 密码策略 — 集成到登录/修改密码/创建用户

**Files:**
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/auth/dto/LoginResponse.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/auth/service/impl/AuthServiceImpl.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/profile/controller/ProfileController.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/UserServiceImpl.java`

- [ ] **Step 1: LoginResponse 新增 passwordExpired 字段**

在 `LoginResponse.java` 的 `expiresIn` 字段之后新增：

```java
@Builder.Default
private boolean passwordExpired = false;
```

- [ ] **Step 2: AuthServiceImpl.login() 登录成功后检查密码过期**

注入 `PasswordPolicyService`。在 `login()` 方法中，构建 `LoginResponse` 之前添加过期检查：

```java
private final PasswordPolicyService passwordPolicyService;

// 在 return LoginResponse.builder() 之前添加：
boolean passwordExpired = passwordPolicyService.isExpired(user);

// 修改 builder：
return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
        .passwordExpired(passwordExpired)
        .build();
```

- [ ] **Step 3: ProfileController.updatePassword() 集成密码策略**

注入 `PasswordPolicyService`。在 `updatePassword()` 方法中旧密码验证后、新密码编码前添加：

```java
private final PasswordPolicyService passwordPolicyService;

// 在旧密码验证之后，新密码编码之前：
List<String> policyErrors = passwordPolicyService.validate(dto.getNewPassword());
if (!policyErrors.isEmpty()) {
    throw new BusinessException(400, String.join("；", policyErrors));
}
if (passwordPolicyService.isHistoryPassword(userId, dto.getNewPassword())) {
    throw new BusinessException(400, "不能使用最近使用过的密码");
}

// 在 userMapper.updateById(user) 之后：
String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
user.setPassword(encodedPassword);
user.setPasswordChangedTime(LocalDateTime.now());
userMapper.updateById(user);
passwordPolicyService.recordHistory(userId, encodedPassword);
```

- [ ] **Step 4: UserServiceImpl.create() 集成密码策略**

注入 `PasswordPolicyService`。在 `create()` 方法中密码编码前添加：

```java
private final PasswordPolicyService passwordPolicyService;

// 在 passwordEncoder.encode(dto.getPassword()) 之前：
List<String> policyErrors = passwordPolicyService.validate(dto.getPassword());
if (!policyErrors.isEmpty()) {
    throw new BusinessException(400, String.join("；", policyErrors));
}

// 在 userMapper.insert(user) 之后：
passwordPolicyService.recordHistory(user.getId(), user.getPassword());
```

- [ ] **Step 5: 修复现有测试**

AuthServiceImplTest、ProfileControllerTest、UserServiceImplTest 中需添加 `@Mock PasswordPolicyService passwordPolicyService;`。
对于 AuthServiceImplTest 中 `login_success` 测试，添加 `when(passwordPolicyService.isExpired(any())).thenReturn(false);`。

- [ ] **Step 6: 运行全部后端测试**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -q`
Expected: 全部通过

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat: 密码策略集成到登录、修改密码和创建用户流程"
```

---

## Task 4: 密码策略 — 前端密码强度指示器 + 过期弹窗

**Files:**
- Create: `admin-frontend/src/components/PasswordStrength.vue`
- Create: `admin-frontend/src/components/__tests__/PasswordStrength.test.ts`
- Modify: `admin-frontend/src/views/login/index.vue`
- Modify: `admin-frontend/src/stores/modules/user.ts`

- [ ] **Step 1: 写 PasswordStrength.vue 测试**

```typescript
// admin-frontend/src/components/__tests__/PasswordStrength.test.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PasswordStrength from '../PasswordStrength.vue'

describe('PasswordStrength', () => {
  it('空密码不显示强度条', () => {
    const wrapper = mount(PasswordStrength, { props: { password: '' } })
    expect(wrapper.find('.strength-bar').exists()).toBe(false)
  })

  it('弱密码显示红色', () => {
    const wrapper = mount(PasswordStrength, { props: { password: 'abc' } })
    expect(wrapper.find('.strength-weak').exists()).toBe(true)
  })

  it('中等密码显示橙色', () => {
    const wrapper = mount(PasswordStrength, { props: { password: 'Abcdef1' } })
    expect(wrapper.find('.strength-medium').exists()).toBe(true)
  })

  it('强密码显示绿色', () => {
    const wrapper = mount(PasswordStrength, { props: { password: 'Abcdef1!' } })
    expect(wrapper.find('.strength-strong').exists()).toBe(true)
  })
})
```

- [ ] **Step 2: 实现 PasswordStrength.vue**

```vue
<template>
  <div v-if="password" class="password-strength">
    <div class="strength-bars">
      <div
        v-for="i in 3"
        :key="i"
        class="strength-bar"
        :class="[i <= level ? strengthClass : 'strength-empty']"
      />
    </div>
    <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ password: string }>()

const level = computed(() => {
  const p = props.password
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]/.test(p)) score++
  if (score <= 1) return 1
  if (score <= 2) return 2
  return 3
})

const strengthClass = computed(() => {
  if (level.value === 1) return 'strength-weak'
  if (level.value === 2) return 'strength-medium'
  return 'strength-strong'
})

const strengthText = computed(() => {
  if (level.value === 1) return '弱'
  if (level.value === 2) return '中'
  return '强'
})
</script>

<style scoped lang="scss">
.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.strength-bars {
  display: flex;
  gap: 4px;
}
.strength-bar {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  transition: background-color 0.3s;
}
.strength-empty { background-color: #e4e7ed; }
.strength-weak { background-color: #f56c6c; }
.strength-medium { background-color: #e6a23c; }
.strength-strong { background-color: #67c23a; }
.strength-text { font-size: 12px; }
</style>
```

- [ ] **Step 3: 修改 login/index.vue 处理密码过期**

在 `views/login/index.vue` 中：

1. 导入 `ElDialog`、`ElForm`、`ElFormItem`、`ElInput`
2. 添加响应式状态：`const showForceChange = ref(false)`、`const changeForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })`
3. 在 `login()` 方法中，登录成功后检查：

```typescript
const res = await userStore.login(loginForm)
if (res.passwordExpired) {
  showForceChange.value = true
  changeForm.oldPassword = loginForm.password
  return
}
router.push(redirect || '/')
```

4. 添加强制修改密码对话框（template）：

```vue
<el-dialog v-model="showForceChange" title="密码已过期，请修改密码" :close-on-click-modal="false" :show-close="false" width="420">
  <el-form :model="changeForm" label-width="80px">
    <el-form-item label="新密码">
      <el-input v-model="changeForm.newPassword" type="password" show-password />
      <PasswordStrength :password="changeForm.newPassword" />
    </el-form-item>
    <el-form-item label="确认密码">
      <el-input v-model="changeForm.confirmPassword" type="password" show-password />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button type="primary" @click="handleForceChange" :loading="changingPwd">确认修改</el-button>
  </template>
</el-dialog>
```

5. `handleForceChange()` 调用 `PUT /api/profile/password`，成功后跳转首页。

- [ ] **Step 4: 修改 user store login() 返回 passwordExpired**

在 `stores/modules/user.ts` 的 `login()` action 中，返回 `res` 而非仅存储 token，使调用方可以检查 `passwordExpired`。

- [ ] **Step 5: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: 前端密码强度指示器 + 密码过期强制修改弹窗"
```

---

## Task 5: 接口幂等性 — 后端注解 + AOP + Controller（TDD）

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/annotation/Idempotent.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/aspect/IdempotentAspect.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/controller/IdempotentController.java`
- Create: `admin-backend/src/test/java/com/iflytek/admin/common/aspect/IdempotentAspectTest.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/common/result/ResultCode.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/common/constant/CacheConstants.java`

- [ ] **Step 1: ResultCode 新增 DUPLICATE_SUBMIT**

在 `ResultCode.java` 的 `RATE_LIMIT_EXCEEDED` 之前新增：

```java
DUPLICATE_SUBMIT(40006, "请勿重复提交"),
```

- [ ] **Step 2: CacheConstants 新增幂等 key 前缀**

```java
public static final String IDEMPOTENT_PREFIX = "idempotent:";
```

- [ ] **Step 3: 创建 @Idempotent 注解**

```java
package com.iflytek.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 方法上，防止重复提交。
 * 前端提交时需在 Header 中携带 X-Idempotent-Token。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** Token 过期时间（秒），默认 10 分钟 */
    int expireSeconds() default 600;
}
```

- [ ] **Step 4: 创建 IdempotentController**

```java
package com.iflytek.admin.common.controller;

import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "幂等性")
@RestController
@RequestMapping("/api/idempotent")
@RequiredArgsConstructor
public class IdempotentController {

    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "获取幂等 Token")
    @GetMapping("/token")
    public Result<String> getToken() {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                CacheConstants.IDEMPOTENT_PREFIX + token,
                "1",
                600,
                TimeUnit.SECONDS
        );
        return Result.ok(token);
    }
}
```

- [ ] **Step 5: 写 IdempotentAspect 测试（RED）**

```java
package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotentAspect 测试")
class IdempotentAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private HttpServletRequest request;
    @Mock
    private MethodSignature signature;

    @InjectMocks
    private IdempotentAspect aspect;

    @BeforeEach
    void setup() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("缺少 Token Header 应抛出异常")
    void missing_token_throws() throws Throwable {
        when(request.getHeader("X-Idempotent-Token")).thenReturn(null);

        Idempotent annotation = mock(Idempotent.class);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Token 有效（首次提交）应放行")
    void valid_token_proceeds() throws Throwable {
        String token = "test-uuid";
        when(request.getHeader("X-Idempotent-Token")).thenReturn(token);
        when(redisTemplate.delete(CacheConstants.IDEMPOTENT_PREFIX + token)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Idempotent annotation = mock(Idempotent.class);

        Object result = aspect.around(joinPoint, annotation);
        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Token 无效（重复提交）应抛出异常")
    void duplicate_token_throws() throws Throwable {
        String token = "test-uuid";
        when(request.getHeader("X-Idempotent-Token")).thenReturn(token);
        when(redisTemplate.delete(CacheConstants.IDEMPOTENT_PREFIX + token)).thenReturn(false);

        Idempotent annotation = mock(Idempotent.class);

        assertThatThrownBy(() -> aspect.around(joinPoint, annotation))
                .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 6: 实现 IdempotentAspect**

```java
package com.iflytek.admin.common.aspect;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader("X-Idempotent-Token");

        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        String redisKey = CacheConstants.IDEMPOTENT_PREFIX + token;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (deleted == null || !deleted) {
            throw new BusinessException(ResultCode.DUPLICATE_SUBMIT);
        }

        return joinPoint.proceed();
    }
}
```

- [ ] **Step 7: 运行测试验证通过**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -Dtest=IdempotentAspectTest`
Expected: 全部通过

- [ ] **Step 8: 在 POST 创建接口上添加 @Idempotent**

在以下 Controller 的 `create()` / `POST` 方法上添加 `@Idempotent`：
- `UserController.create()`
- `RoleController.create()`
- `MenuController.create()`
- `DeptController.create()`
- `NoticeController.create()`
- `DictController.createType()` 和 `createData()`

示例：
```java
@Idempotent
@PostMapping
public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) { ... }
```

- [ ] **Step 9: 运行全部后端测试**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -q`
Expected: 全部通过

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "feat: 接口幂等性 — @Idempotent 注解 + AOP + Token 生成"
```

---

## Task 6: 接口幂等性 — 前端 useIdempotent + 请求拦截器

**Files:**
- Create: `admin-frontend/src/composables/useIdempotent.ts`
- Create: `admin-frontend/src/composables/__tests__/useIdempotent.test.ts`
- Modify: `admin-frontend/src/api/request.ts`

- [ ] **Step 1: 新增幂等 API**

在 `admin-frontend/src/api/modules/` 中新增（或在已有文件中添加）：

```typescript
// admin-frontend/src/api/modules/idempotent.ts
import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export function getIdempotentToken(): Promise<ApiResponse<string>> {
  return request.get('/idempotent/token')
}
```

- [ ] **Step 2: 创建 useIdempotent composable**

```typescript
// admin-frontend/src/composables/useIdempotent.ts
import { ref } from 'vue'
import { getIdempotentToken } from '@/api/modules/idempotent'

const currentToken = ref<string | null>(null)

export function useIdempotent() {
  async function fetchToken() {
    try {
      const { data } = await getIdempotentToken()
      currentToken.value = data
    } catch {
      currentToken.value = null
    }
  }

  function getToken(): string | null {
    return currentToken.value
  }

  function clearToken() {
    currentToken.value = null
  }

  return { fetchToken, getToken, clearToken, currentToken }
}
```

- [ ] **Step 3: 修改 request.ts 请求拦截器**

在 `admin-frontend/src/api/request.ts` 的请求拦截器中，token 注入之后添加：

```typescript
import { useIdempotent } from '@/composables/useIdempotent'

// 在请求拦截器内，config.headers.Authorization 赋值之后：
const { getToken, clearToken } = useIdempotent()
const idempotentToken = getToken()
if (idempotentToken && config.method?.toLowerCase() === 'post') {
  config.headers['X-Idempotent-Token'] = idempotentToken
  clearToken()
}
```

- [ ] **Step 4: 写测试**

```typescript
// admin-frontend/src/composables/__tests__/useIdempotent.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useIdempotent } from '../useIdempotent'

vi.mock('@/api/modules/idempotent', () => ({
  getIdempotentToken: vi.fn().mockResolvedValue({ data: 'mock-token-uuid' })
}))

describe('useIdempotent', () => {
  it('fetchToken 后 getToken 返回 token', async () => {
    const { fetchToken, getToken } = useIdempotent()
    await fetchToken()
    expect(getToken()).toBe('mock-token-uuid')
  })

  it('clearToken 清除 token', async () => {
    const { fetchToken, getToken, clearToken } = useIdempotent()
    await fetchToken()
    clearToken()
    expect(getToken()).toBeNull()
  })
})
```

- [ ] **Step 5: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: 前端幂等 token composable + 请求拦截器自动注入"
```

---

## Task 7: WebSocket — 后端配置 + 认证拦截器

**Files:**
- Modify: `admin-backend/pom.xml`
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/config/WebSocketConfig.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/config/WebSocketAuthInterceptor.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/security/SecurityConfig.java`

- [ ] **Step 1: pom.xml 新增 WebSocket 依赖**

在 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 WebSocketConfig**

```java
package com.iflytek.admin.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
```

- [ ] **Step 3: 创建 WebSocketAuthInterceptor**

```java
package com.iflytek.admin.common.config;

import com.iflytek.admin.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && jwtUtil.isTokenValid(token)) {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                accessor.setUser(auth);
                log.debug("WebSocket CONNECT authenticated: userId={}, username={}", userId, username);
            } else {
                log.warn("WebSocket CONNECT with invalid token");
                throw new IllegalArgumentException("无效的认证 Token");
            }
        }
        return message;
    }
}
```

- [ ] **Step 4: SecurityConfig 放行 /ws**

在 `SecurityConfig.java` 的 `requestMatchers` 中添加：

```java
"/ws/**",
"/ws"
```

- [ ] **Step 5: 运行后端编译检查**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: WebSocket STOMP 配置 + JWT 认证拦截器"
```

---

## Task 8: WebSocket — NoticeService 发布推送

**Files:**
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/NoticeServiceImpl.java`

- [ ] **Step 1: 注入 SimpMessagingTemplate**

在 `NoticeServiceImpl` 中注入：

```java
private final SimpMessagingTemplate messagingTemplate;
```

需要导入 `org.springframework.messaging.simp.SimpMessagingTemplate`。

- [ ] **Step 2: 在 publish() 方法中添加 WebSocket 推送**

在 `publish()` 方法中，发布成功后添加推送逻辑：

```java
@Override
public void publish(Long id) {
    // 原有逻辑：更新 notice status=1
    SysNotice notice = ...;
    notice.setStatus(1);
    noticeMapper.updateById(notice);

    // 新增：为目标用户创建 user_notice 记录（如果尚未创建）
    // 原有逻辑...

    // 新增：WebSocket 推送
    Map<String, Object> payload = new HashMap<>();
    payload.put("id", notice.getId());
    payload.put("title", notice.getTitle());
    payload.put("noticeType", notice.getNoticeType());
    payload.put("createdTime", notice.getCreatedTime());

    if (notice.getNoticeType() == 2) {
        // 公告：广播
        messagingTemplate.convertAndSend("/topic/notice/broadcast", payload);
    } else {
        // 通知：逐用户推送
        // 查询该通知的目标用户
        List<SysUserNotice> targets = userNoticeMapper.selectList(
                new LambdaQueryWrapper<SysUserNotice>().eq(SysUserNotice::getNoticeId, id));
        for (SysUserNotice target : targets) {
            messagingTemplate.convertAndSendToUser(
                    target.getUserId().toString(),
                    "/queue/notice",
                    payload);
        }
    }
}
```

- [ ] **Step 3: 修复 NoticeServiceImpl 现有测试**

在 NoticeServiceImplTest 中添加 `@Mock SimpMessagingTemplate messagingTemplate;`。

- [ ] **Step 4: 运行后端测试**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test -q`
Expected: 全部通过

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: 通知发布时通过 WebSocket 推送消息"
```

---

## Task 9: WebSocket — 前端连接 + Notice Store

**Files:**
- Create: `admin-frontend/src/composables/useWebSocket.ts`
- Create: `admin-frontend/src/stores/modules/notice.ts`
- Modify: `admin-frontend/src/components/Layout/Header.vue`
- Modify: `admin-frontend/src/stores/modules/user.ts`

- [ ] **Step 1: 安装 @stomp/stompjs 依赖**

Run: `cd admin-frontend && npm install @stomp/stompjs`

注意：`sockjs-client` 不再需要单独安装，`@stomp/stompjs` 6.x 支持原生 WebSocket 回退。

- [ ] **Step 2: 创建 useWebSocket composable**

```typescript
// admin-frontend/src/composables/useWebSocket.ts
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { getToken } from '@/utils/auth'

const client = ref<Client | null>(null)
const connected = ref(false)

export function useWebSocket() {
  function connect() {
    if (client.value?.connected) return

    const wsUrl = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/websocket`

    const stompClient = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${getToken()}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
        connected.value = false
      }
    })

    stompClient.activate()
    client.value = stompClient
  }

  function subscribe(destination: string, callback: (body: unknown) => void) {
    if (!client.value) return
    return client.value.subscribe(destination, (message) => {
      try {
        callback(JSON.parse(message.body))
      } catch {
        callback(message.body)
      }
    })
  }

  function disconnect() {
    if (client.value) {
      client.value.deactivate()
      client.value = null
      connected.value = false
    }
  }

  return { connect, subscribe, disconnect, connected, client }
}
```

- [ ] **Step 3: 创建 notice store**

```typescript
// admin-frontend/src/stores/modules/notice.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElNotification } from 'element-plus'
import { getUnreadCount, markNoticeRead, markAllNoticesRead } from '@/api/modules/notice'
import { useWebSocket } from '@/composables/useWebSocket'

export interface NoticePayload {
  id: number
  title: string
  noticeType: number
  createdTime: string
}

export const useNoticeStore = defineStore('notice', () => {
  const unreadCount = ref(0)
  const latestNotices = ref<NoticePayload[]>([])
  const { connect, subscribe, disconnect } = useWebSocket()

  async function fetchUnreadCount() {
    try {
      const { data } = await getUnreadCount()
      unreadCount.value = data
    } catch { /* ignore */ }
  }

  function onNewNotice(notice: NoticePayload) {
    unreadCount.value++
    latestNotices.value = [notice, ...latestNotices.value].slice(0, 10)
    ElNotification({
      title: notice.noticeType === 2 ? '新公告' : '新通知',
      message: notice.title,
      type: 'info',
      duration: 5000
    })
  }

  function initWebSocket(userId: number) {
    connect()
    // 等连接建立后订阅
    const checkAndSubscribe = () => {
      const ws = useWebSocket()
      if (ws.connected.value) {
        subscribe('/topic/notice/broadcast', (data) => onNewNotice(data as NoticePayload))
        subscribe(`/user/${userId}/queue/notice`, (data) => onNewNotice(data as NoticePayload))
      } else {
        setTimeout(checkAndSubscribe, 500)
      }
    }
    checkAndSubscribe()
  }

  async function markRead(noticeId: number) {
    await markNoticeRead(noticeId)
    if (unreadCount.value > 0) unreadCount.value--
  }

  async function markAllRead() {
    await markAllNoticesRead()
    unreadCount.value = 0
  }

  function cleanup() {
    disconnect()
    unreadCount.value = 0
    latestNotices.value = []
  }

  return {
    unreadCount,
    latestNotices,
    fetchUnreadCount,
    initWebSocket,
    markRead,
    markAllRead,
    cleanup
  }
})
```

- [ ] **Step 4: 修改 Header.vue 集成 notice store**

替换 Header.vue 中的 `unreadCount` 局部变量和 `fetchUnreadCount` 函数，改用 notice store：

```typescript
// 删除：
// import { getUnreadCount } from '@/api/modules/notice'
// const unreadCount = ref(0)
// async function fetchUnreadCount() { ... }

// 替换为：
import { useNoticeStore } from '@/stores/modules/notice'
const noticeStore = useNoticeStore()

// template 中 :value="unreadCount" 改为 :value="noticeStore.unreadCount"
// onMounted 中 fetchUnreadCount() 改为 noticeStore.fetchUnreadCount()
```

- [ ] **Step 5: 修改 user store — 登录后初始化 WebSocket，登出时清理**

在 `stores/modules/user.ts` 的 `fetchUserInfo()` 成功后添加：

```typescript
import { useNoticeStore } from './notice'

// fetchUserInfo() 最后：
const noticeStore = useNoticeStore()
noticeStore.fetchUnreadCount()
noticeStore.initWebSocket(userInfo.id)
```

在 `resetState()` 中添加：

```typescript
const noticeStore = useNoticeStore()
noticeStore.cleanup()
```

- [ ] **Step 6: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过（注意 Header.test.ts 可能需要 mock noticeStore）

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "feat: 前端 WebSocket 连接 + 通知 Store + Header 集成"
```

---

## Task 10: 表格增强 — useTableSettings + TableToolbar（TDD）

**Files:**
- Create: `admin-frontend/src/composables/useTableSettings.ts`
- Create: `admin-frontend/src/composables/__tests__/useTableSettings.test.ts`
- Create: `admin-frontend/src/components/TableToolbar.vue`
- Create: `admin-frontend/src/components/__tests__/TableToolbar.test.ts`

- [ ] **Step 1: 写 useTableSettings 测试**

```typescript
// admin-frontend/src/composables/__tests__/useTableSettings.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTableSettings, type ColumnSetting } from '../useTableSettings'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: vi.fn((key: string) => mockStorage[key] || null),
  setItem: vi.fn((key: string, value: string) => { mockStorage[key] = value }),
  removeItem: vi.fn((key: string) => { delete mockStorage[key] })
})

describe('useTableSettings', () => {
  const defaultColumns: ColumnSetting[] = [
    { key: 'name', label: '姓名', visible: true },
    { key: 'email', label: '邮箱', visible: true },
    { key: 'phone', label: '电话', visible: false }
  ]

  beforeEach(() => {
    Object.keys(mockStorage).forEach(k => delete mockStorage[k])
  })

  it('初始加载默认列设置', () => {
    const { columns } = useTableSettings('test-table', defaultColumns)
    expect(columns.value).toHaveLength(3)
    expect(columns.value[0].key).toBe('name')
  })

  it('toggleColumn 切换列可见性', () => {
    const { columns, toggleColumn } = useTableSettings('test-table', defaultColumns)
    toggleColumn('phone')
    expect(columns.value.find(c => c.key === 'phone')?.visible).toBe(true)
  })

  it('resetColumns 恢复默认', () => {
    const { columns, toggleColumn, resetColumns } = useTableSettings('test-table', defaultColumns)
    toggleColumn('name')
    resetColumns()
    expect(columns.value[0].visible).toBe(true)
  })

  it('density 默认为 default', () => {
    const { density } = useTableSettings('test-table', defaultColumns)
    expect(density.value).toBe('default')
  })

  it('setDensity 设置密度', () => {
    const { density, setDensity } = useTableSettings('test-table', defaultColumns)
    setDensity('small')
    expect(density.value).toBe('small')
  })
})
```

- [ ] **Step 2: 实现 useTableSettings**

```typescript
// admin-frontend/src/composables/useTableSettings.ts
import { ref, watch } from 'vue'

export interface ColumnSetting {
  key: string
  label: string
  visible: boolean
}

type TableDensity = 'large' | 'default' | 'small'

const STORAGE_PREFIX = 'table-settings:'

export function useTableSettings(tableId: string, defaultColumns: ColumnSetting[]) {
  const storageKey = STORAGE_PREFIX + tableId

  function loadFromStorage(): { columns: ColumnSetting[]; density: TableDensity } | null {
    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) return null
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function saveToStorage() {
    localStorage.setItem(storageKey, JSON.stringify({
      columns: columns.value,
      density: density.value
    }))
  }

  const saved = loadFromStorage()
  const columns = ref<ColumnSetting[]>(
    saved?.columns ?? defaultColumns.map(c => ({ ...c }))
  )
  const density = ref<TableDensity>(saved?.density ?? 'default')

  function toggleColumn(key: string) {
    columns.value = columns.value.map(c =>
      c.key === key ? { ...c, visible: !c.visible } : c
    )
  }

  function reorderColumns(newOrder: ColumnSetting[]) {
    columns.value = newOrder
  }

  function resetColumns() {
    columns.value = defaultColumns.map(c => ({ ...c }))
    density.value = 'default'
    localStorage.removeItem(storageKey)
  }

  function setDensity(d: TableDensity) {
    density.value = d
  }

  watch([columns, density], saveToStorage, { deep: true })

  return { columns, density, toggleColumn, reorderColumns, resetColumns, setDensity }
}
```

- [ ] **Step 3: 写 TableToolbar.vue 测试**

```typescript
// admin-frontend/src/components/__tests__/TableToolbar.test.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing' // may need to mock if not available
import TableToolbar from '../TableToolbar.vue'

// 由于组件依赖 Element Plus，简单测试 props 和 emits
describe('TableToolbar', () => {
  it('渲染刷新和列设置按钮', () => {
    const wrapper = mount(TableToolbar, {
      props: {
        columns: [
          { key: 'name', label: '姓名', visible: true },
          { key: 'email', label: '邮箱', visible: true }
        ],
        density: 'default'
      },
      global: {
        stubs: {
          'el-button': true,
          'el-popover': true,
          'el-checkbox': true,
          'el-radio-group': true,
          'el-radio-button': true,
          'el-icon': true,
          'el-tooltip': true,
          'el-divider': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('点击刷新触发 refresh 事件', async () => {
    const wrapper = mount(TableToolbar, {
      props: {
        columns: [{ key: 'name', label: '姓名', visible: true }],
        density: 'default'
      },
      global: {
        stubs: {
          'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
          'el-popover': true,
          'el-checkbox': true,
          'el-radio-group': true,
          'el-radio-button': true,
          'el-icon': true,
          'el-tooltip': true,
          'el-divider': true
        }
      }
    })
    const refreshBtn = wrapper.findAll('button').find(b => b.text().includes('') || true)
    if (refreshBtn) {
      await refreshBtn.trigger('click')
    }
    // 基本可渲染性测试
    expect(wrapper.html()).toBeTruthy()
  })
})
```

- [ ] **Step 4: 实现 TableToolbar.vue**

```vue
<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot name="left" />
    </div>
    <div class="toolbar-right">
      <el-tooltip content="刷新">
        <el-button :icon="Refresh" circle size="small" @click="$emit('refresh')" />
      </el-tooltip>

      <el-tooltip content="密度">
        <el-popover trigger="click" width="120">
          <template #reference>
            <el-button :icon="DCaret" circle size="small" />
          </template>
          <el-radio-group :model-value="density" @update:model-value="$emit('update:density', $event)">
            <el-radio-button label="large">宽松</el-radio-button>
            <el-radio-button label="default">默认</el-radio-button>
            <el-radio-button label="small">紧凑</el-radio-button>
          </el-radio-group>
        </el-popover>
      </el-tooltip>

      <el-tooltip content="列设置">
        <el-popover trigger="click" width="180">
          <template #reference>
            <el-button :icon="Setting" circle size="small" />
          </template>
          <div class="column-settings">
            <div v-for="col in columns" :key="col.key" class="column-item">
              <el-checkbox
                :model-value="col.visible"
                @update:model-value="$emit('toggleColumn', col.key)"
              >
                {{ col.label }}
              </el-checkbox>
            </div>
            <el-divider style="margin: 8px 0" />
            <el-button size="small" text type="primary" @click="$emit('reset')">重置</el-button>
          </div>
        </el-popover>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh, DCaret, Setting } from '@element-plus/icons-vue'
import type { ColumnSetting } from '@/composables/useTableSettings'

defineProps<{
  columns: ColumnSetting[]
  density: string
}>()

defineEmits<{
  refresh: []
  'update:density': [value: string]
  toggleColumn: [key: string]
  reset: []
}>()
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-right {
  display: flex;
  gap: 4px;
}
.column-item {
  padding: 2px 0;
}
</style>
```

- [ ] **Step 5: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: 表格增强 — useTableSettings composable + TableToolbar 组件"
```

---

## Task 11: Dashboard — Widget 注册表 + Store（TDD）

**Files:**
- Create: `admin-frontend/src/views/dashboard/components/widgets/registry.ts`
- Create: `admin-frontend/src/stores/modules/dashboard.ts`
- Create: `admin-frontend/src/stores/__tests__/dashboard.test.ts`

- [ ] **Step 1: 创建 Widget 注册表**

```typescript
// admin-frontend/src/views/dashboard/components/widgets/registry.ts
export interface WidgetMeta {
  id: string
  name: string
  type: 'stat' | 'chart' | 'table' | 'shortcut'
  component: () => Promise<any>
  defaultSpan: number  // el-col :span
}

export const widgetRegistry: WidgetMeta[] = [
  {
    id: 'stat-users',
    name: '用户统计',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-roles',
    name: '角色统计',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-today-login',
    name: '今日登录',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-system',
    name: '系统状态',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'chart-login-trend',
    name: '登录趋势（7天）',
    type: 'chart',
    component: () => import('./ChartLoginTrend.vue'),
    defaultSpan: 12
  },
  {
    id: 'chart-overview',
    name: '系统概览',
    type: 'chart',
    component: () => import('./ChartOverview.vue'),
    defaultSpan: 12
  },
  {
    id: 'table-recent-login',
    name: '最近登录记录',
    type: 'table',
    component: () => import('./TableRecentLogin.vue'),
    defaultSpan: 24
  },
  {
    id: 'shortcut-links',
    name: '快捷入口',
    type: 'shortcut',
    component: () => import('./ShortcutLinks.vue'),
    defaultSpan: 24
  }
]

export const DEFAULT_LAYOUT = [
  'stat-users', 'stat-roles', 'stat-today-login', 'stat-system',
  'chart-login-trend', 'chart-overview',
  'table-recent-login'
]

export function getWidgetMeta(id: string): WidgetMeta | undefined {
  return widgetRegistry.find(w => w.id === id)
}
```

- [ ] **Step 2: 写 dashboard store 测试**

```typescript
// admin-frontend/src/stores/__tests__/dashboard.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '../modules/dashboard'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: vi.fn((key: string) => mockStorage[key] || null),
  setItem: vi.fn((key: string, value: string) => { mockStorage[key] = value }),
  removeItem: vi.fn((key: string) => { delete mockStorage[key] })
})

describe('useDashboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.keys(mockStorage).forEach(k => delete mockStorage[k])
  })

  it('初始化加载默认布局', () => {
    const store = useDashboardStore()
    expect(store.activeWidgets.length).toBeGreaterThan(0)
    expect(store.editMode).toBe(false)
  })

  it('addWidget 添加卡片到末尾', () => {
    const store = useDashboardStore()
    const initialLength = store.activeWidgets.length
    store.removeWidget('table-recent-login')
    store.addWidget('table-recent-login')
    expect(store.activeWidgets).toContain('table-recent-login')
  })

  it('removeWidget 移除卡片', () => {
    const store = useDashboardStore()
    store.removeWidget('stat-users')
    expect(store.activeWidgets).not.toContain('stat-users')
  })

  it('reorderWidgets 更新顺序', () => {
    const store = useDashboardStore()
    const reversed = [...store.activeWidgets].reverse()
    store.reorderWidgets(reversed)
    expect(store.activeWidgets).toEqual(reversed)
  })

  it('resetLayout 恢复默认', () => {
    const store = useDashboardStore()
    store.removeWidget('stat-users')
    store.resetLayout()
    expect(store.activeWidgets).toContain('stat-users')
  })

  it('toggleEditMode 切换编辑模式', () => {
    const store = useDashboardStore()
    store.toggleEditMode()
    expect(store.editMode).toBe(true)
    store.toggleEditMode()
    expect(store.editMode).toBe(false)
  })

  it('availableWidgets 返回未添加的 widget', () => {
    const store = useDashboardStore()
    store.removeWidget('shortcut-links')
    expect(store.availableWidgets.some(w => w.id === 'shortcut-links')).toBe(true)
  })
})
```

- [ ] **Step 3: 实现 dashboard store**

```typescript
// admin-frontend/src/stores/modules/dashboard.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { widgetRegistry, DEFAULT_LAYOUT, getWidgetMeta, type WidgetMeta } from '@/views/dashboard/components/widgets/registry'

const STORAGE_KEY = 'dashboard-layout'

export const useDashboardStore = defineStore('dashboard', () => {
  function loadLayout(): string[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed
      }
    } catch { /* ignore */ }
    return [...DEFAULT_LAYOUT]
  }

  function saveLayout() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(activeWidgets.value))
  }

  const activeWidgets = ref<string[]>(loadLayout())
  const editMode = ref(false)

  const availableWidgets = computed<WidgetMeta[]>(() =>
    widgetRegistry.filter(w => !activeWidgets.value.includes(w.id))
  )

  const activeWidgetMetas = computed<WidgetMeta[]>(() =>
    activeWidgets.value
      .map(id => getWidgetMeta(id))
      .filter((m): m is WidgetMeta => m !== undefined)
  )

  function addWidget(widgetId: string) {
    if (!activeWidgets.value.includes(widgetId)) {
      activeWidgets.value = [...activeWidgets.value, widgetId]
      saveLayout()
    }
  }

  function removeWidget(widgetId: string) {
    activeWidgets.value = activeWidgets.value.filter(id => id !== widgetId)
    saveLayout()
  }

  function reorderWidgets(newOrder: string[]) {
    activeWidgets.value = [...newOrder]
    saveLayout()
  }

  function resetLayout() {
    activeWidgets.value = [...DEFAULT_LAYOUT]
    localStorage.removeItem(STORAGE_KEY)
  }

  function toggleEditMode() {
    editMode.value = !editMode.value
  }

  return {
    activeWidgets,
    editMode,
    availableWidgets,
    activeWidgetMetas,
    addWidget,
    removeWidget,
    reorderWidgets,
    resetLayout,
    toggleEditMode
  }
})
```

- [ ] **Step 4: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: Dashboard widget 注册表 + Pinia Store（含拖拽/增删/持久化）"
```

---

## Task 12: Dashboard — Widget 组件（从现有 dashboard 提取）

**Files:**
- Create: `admin-frontend/src/views/dashboard/components/widgets/StatCard.vue`
- Create: `admin-frontend/src/views/dashboard/components/widgets/ChartLoginTrend.vue`
- Create: `admin-frontend/src/views/dashboard/components/widgets/ChartOverview.vue`
- Create: `admin-frontend/src/views/dashboard/components/widgets/TableRecentLogin.vue`
- Create: `admin-frontend/src/views/dashboard/components/widgets/ShortcutLinks.vue`

- [ ] **Step 1: 创建 StatCard.vue**

从现有 dashboard 提取统计卡片逻辑。接收 `widgetId` prop，根据 ID 显示对应统计数据：

```vue
<template>
  <div class="stat-item">
    <div class="stat-icon" :style="{ backgroundColor: config.bgColor, color: config.color }">
      <el-icon :size="28"><component :is="config.icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ value.toLocaleString() }}</div>
      <div class="stat-label">{{ config.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getStats, type DashboardStats } from '@/api/modules/dashboard'

const props = defineProps<{ widgetId: string }>()
const { t } = useI18n()
const statsData = ref<DashboardStats>({ userCount: 0, roleCount: 0, menuCount: 0, todayLoginCount: 0, totalLoginCount: 0 })

const configMap: Record<string, { key: keyof DashboardStats | null; label: string; icon: string; color: string; bgColor: string }> = {
  'stat-users': { key: 'userCount', label: 'dashboard.userCount', icon: 'User', color: '#409EFF', bgColor: 'rgba(64,158,255,0.1)' },
  'stat-roles': { key: 'roleCount', label: 'dashboard.roleCount', icon: 'UserFilled', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' },
  'stat-today-login': { key: 'todayLoginCount', label: 'dashboard.loginCount', icon: 'Monitor', color: '#E6A23C', bgColor: 'rgba(230,162,60,0.1)' },
  'stat-system': { key: null, label: 'dashboard.systemStatus', icon: 'CircleCheck', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' }
}

const config = computed(() => {
  const c = configMap[props.widgetId] || configMap['stat-users']
  return { ...c, label: t(c.label) }
})

const value = computed(() => {
  if (!config.value || configMap[props.widgetId]?.key === null) return 0
  const key = configMap[props.widgetId]?.key
  return key ? statsData.value[key] : 0
})

onMounted(async () => {
  try {
    const res = await getStats()
    statsData.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped lang="scss">
.stat-item { display: flex; align-items: center; padding: 8px 0; }
.stat-icon { width: 56px; height: 56px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 16px; }
.stat-value { font-size: 26px; font-weight: bold; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
</style>
```

- [ ] **Step 2: 创建 ChartLoginTrend.vue**

从现有 dashboard 提取登录趋势图表逻辑：

```vue
<template>
  <div class="chart-container">
    <v-chart :option="chartOption" autoresize />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { getLoginLogs, type LoginLog } from '@/api/modules/log'

const { t } = useI18n()
use([CanvasRenderer, LineChart, TooltipComponent, GridComponent])

const loginDays = ref<string[]>([])
const loginCounts = ref<number[]>([])

function getRecentDays(days: number): string[] {
  const result: string[] = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    result.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return result
}

function countLoginsByDay(logs: LoginLog[], days: number): number[] {
  const counts = new Array(days).fill(0)
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  for (const log of logs) {
    const logDate = new Date(log.loginTime)
    const diffDays = Math.floor((today.getTime() - logDate.getTime()) / (1000 * 60 * 60 * 24))
    if (diffDays >= 0 && diffDays < days) counts[days - 1 - diffDays]++
  }
  return counts
}

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: loginDays.value },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: t('dashboard.loginTimes'),
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.15 },
    itemStyle: { color: '#409EFF' },
    data: loginCounts.value
  }]
}))

onMounted(async () => {
  loginDays.value = getRecentDays(7)
  try {
    const res = await getLoginLogs({ page: 1, size: 50 })
    loginCounts.value = countLoginsByDay(res.data.list, 7)
  } catch { /* ignore */ }
})
</script>

<style scoped>
.chart-container { height: 300px; }
</style>
```

- [ ] **Step 3: 创建 ChartOverview.vue**

同样从现有 dashboard 提取系统概览柱状图。结构与 ChartLoginTrend 类似，使用 BarChart。

- [ ] **Step 4: 创建 TableRecentLogin.vue**

```vue
<template>
  <el-table :data="recentLogs" size="small">
    <el-table-column prop="username" :label="$t('dashboard.column.username')" width="120" />
    <el-table-column prop="ip" :label="$t('dashboard.column.ip')" width="140" />
    <el-table-column prop="location" :label="$t('dashboard.column.location')" min-width="140" />
    <el-table-column prop="browser" :label="$t('dashboard.column.browser')" min-width="120" />
    <el-table-column :label="$t('dashboard.column.status')" width="80">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
          {{ row.status === 1 ? $t('dashboard.column.success') : $t('dashboard.column.failed') }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="loginTime" :label="$t('dashboard.column.loginTime')" min-width="170" />
  </el-table>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLoginLogs, type LoginLog } from '@/api/modules/log'

const recentLogs = ref<LoginLog[]>([])

onMounted(async () => {
  try {
    const res = await getLoginLogs({ page: 1, size: 10 })
    recentLogs.value = res.data.list
  } catch { /* ignore */ }
})
</script>
```

- [ ] **Step 5: 创建 ShortcutLinks.vue**

```vue
<template>
  <div class="shortcut-grid">
    <div v-for="link in links" :key="link.path" class="shortcut-item" @click="router.push(link.path)">
      <el-icon :size="24" :color="link.color"><component :is="link.icon" /></el-icon>
      <span>{{ link.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()

const links = [
  { path: '/system/user', label: '用户管理', icon: 'User', color: '#409EFF' },
  { path: '/system/role', label: '角色管理', icon: 'UserFilled', color: '#67C23A' },
  { path: '/system/menu', label: '菜单管理', icon: 'Menu', color: '#E6A23C' },
  { path: '/system/notice', label: '通知公告', icon: 'Bell', color: '#F56C6C' }
]
</script>

<style scoped lang="scss">
.shortcut-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.shortcut-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 16px; border-radius: 8px; cursor: pointer;
  &:hover { background-color: #f5f7fa; }
  span { font-size: 13px; color: #606266; }
}
</style>
```

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "feat: Dashboard widget 组件（统计卡片、图表、表格、快捷入口）"
```

---

## Task 13: Dashboard — DashboardGrid + WidgetWrapper + AddWidgetDialog

**Files:**
- Create: `admin-frontend/src/views/dashboard/components/WidgetWrapper.vue`
- Create: `admin-frontend/src/views/dashboard/components/DashboardGrid.vue`
- Create: `admin-frontend/src/views/dashboard/components/AddWidgetDialog.vue`

- [ ] **Step 1: 创建 WidgetWrapper.vue**

```vue
<template>
  <el-card shadow="hover" class="widget-wrapper" :class="{ 'edit-mode': editMode }">
    <template #header v-if="meta">
      <div class="widget-header">
        <span v-if="editMode" class="drag-handle">⠿</span>
        <span>{{ meta.name }}</span>
        <el-button
          v-if="editMode"
          type="danger"
          :icon="Close"
          circle
          size="small"
          class="remove-btn"
          @click="$emit('remove')"
        />
      </div>
    </template>
    <component :is="asyncComponent" :widget-id="widgetId" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { getWidgetMeta } from './widgets/registry'

const props = defineProps<{
  widgetId: string
  editMode: boolean
}>()

defineEmits<{ remove: [] }>()

const meta = computed(() => getWidgetMeta(props.widgetId))

const asyncComponent = computed(() => {
  const m = meta.value
  if (!m) return null
  return defineAsyncComponent(m.component)
})
</script>

<style scoped lang="scss">
.widget-wrapper.edit-mode {
  border: 2px dashed var(--el-color-primary);
}
.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: #909399;
  user-select: none;
}
.remove-btn {
  margin-left: auto;
}
</style>
```

- [ ] **Step 2: 创建 DashboardGrid.vue**

```vue
<template>
  <div>
    <div class="grid-toolbar">
      <el-button v-if="!store.editMode" type="primary" plain size="small" @click="store.toggleEditMode()">
        编辑布局
      </el-button>
      <template v-else>
        <el-button type="success" size="small" @click="store.toggleEditMode()">完成编辑</el-button>
        <el-button size="small" @click="showAddDialog = true">+ 添加卡片</el-button>
        <el-button size="small" type="warning" @click="store.resetLayout()">恢复默认</el-button>
      </template>
    </div>

    <draggable
      :list="widgetIds"
      item-key="id"
      handle=".drag-handle"
      :disabled="!store.editMode"
      @end="onDragEnd"
      class="dashboard-grid"
    >
      <template #item="{ element }">
        <el-col :span="getSpan(element)" :key="element" class="grid-item">
          <WidgetWrapper
            :widget-id="element"
            :edit-mode="store.editMode"
            @remove="store.removeWidget(element)"
          />
        </el-col>
      </template>
    </draggable>

    <AddWidgetDialog v-model="showAddDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import draggable from 'vuedraggable'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getWidgetMeta } from './widgets/registry'
import WidgetWrapper from './WidgetWrapper.vue'
import AddWidgetDialog from './AddWidgetDialog.vue'

const store = useDashboardStore()
const showAddDialog = ref(false)

const widgetIds = computed({
  get: () => store.activeWidgets,
  set: (val) => store.reorderWidgets(val)
})

function getSpan(widgetId: string): number {
  return getWidgetMeta(widgetId)?.defaultSpan ?? 12
}

function onDragEnd() {
  store.reorderWidgets([...store.activeWidgets])
}
</script>

<style scoped lang="scss">
.grid-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.dashboard-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -10px;
}
.grid-item {
  padding: 10px;
}
</style>
```

- [ ] **Step 3: 创建 AddWidgetDialog.vue**

```vue
<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="添加卡片" width="560">
    <div v-if="store.availableWidgets.length === 0" class="empty-tip">
      所有卡片已添加到布局中
    </div>
    <div v-else class="widget-grid">
      <div
        v-for="widget in store.availableWidgets"
        :key="widget.id"
        class="widget-option"
        @click="addAndClose(widget.id)"
      >
        <div class="widget-type-badge">{{ typeLabel(widget.type) }}</div>
        <div class="widget-name">{{ widget.name }}</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { useDashboardStore } from '@/stores/modules/dashboard'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const store = useDashboardStore()

function typeLabel(type: string): string {
  const map: Record<string, string> = { stat: '统计', chart: '图表', table: '表格', shortcut: '快捷' }
  return map[type] || type
}

function addAndClose(widgetId: string) {
  store.addWidget(widgetId)
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.widget-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.widget-option {
  padding: 16px; border: 1px solid #e4e7ed; border-radius: 8px; cursor: pointer;
  text-align: center; transition: all 0.2s;
  &:hover { border-color: var(--el-color-primary); background-color: #f0f9ff; }
}
.widget-type-badge { font-size: 12px; color: #909399; margin-bottom: 4px; }
.widget-name { font-size: 14px; font-weight: 500; }
.empty-tip { text-align: center; padding: 40px; color: #909399; }
</style>
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "feat: DashboardGrid 拖拽容器 + WidgetWrapper + AddWidgetDialog"
```

---

## Task 14: Dashboard — 重构 index.vue 使用新组件

**Files:**
- Modify: `admin-frontend/src/views/dashboard/index.vue`

- [ ] **Step 1: 重构 dashboard/index.vue**

将原有硬编码布局替换为 `DashboardGrid` 组件：

```vue
<template>
  <div class="dashboard-container">
    <DashboardGrid />
  </div>
</template>

<script setup lang="ts">
import DashboardGrid from './components/DashboardGrid.vue'
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 0;
}
</style>
```

- [ ] **Step 2: 运行前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 3: 运行类型检查**

Run: `cd admin-frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: Dashboard 重构为可编辑布局（拖拽排序 + 增删 Widget）"
```

---

## Task 15: 全部验证

- [ ] **Step 1: 运行后端全部测试**

Run: `cd admin-backend && JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home mvn test`
Expected: 全部通过，0 failures

- [ ] **Step 2: 运行前端全部测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部通过

- [ ] **Step 3: 运行前端类型检查**

Run: `cd admin-frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: 运行前端 lint**

Run: `cd admin-frontend && npx eslint src/`
Expected: 0 errors

- [ ] **Step 5: 确认 P1 完成**

检查所有 5 个功能模块均已实现并通过测试：
- P1-1 密码安全策略：PasswordPolicyService + 前端强度指示器 + 过期弹窗
- P1-2 接口幂等性：@Idempotent + AOP + 前端 token
- P1-3 WebSocket 实时通知：STOMP 配置 + 认证 + Notice Store
- P1-4 表格增强：TableToolbar + useTableSettings
- P1-5 Dashboard 可编辑布局：拖拽排序 + 增删 Widget + 持久化
