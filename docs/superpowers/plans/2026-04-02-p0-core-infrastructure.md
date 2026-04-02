# P0 核心基础设施 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为系统建立缓存基础设施、定时任务调度引擎和前端字典缓存，提升系统性能和功能完整性。

**Architecture:** 后端新增 CacheService 封装 Redis 缓存操作，为字典/配置/菜单提供 Cache Aside 缓存策略；新增 DynamicTaskManager 集成 Spring TaskScheduler 实现动态定时任务调度；前端新增 Pinia dict store 和 useDict composable 缓存字典数据。

**Tech Stack:** Spring Boot 3.2, Redis (RedisTemplate), Spring TaskScheduler, MyBatis-Plus, Vue 3, Pinia, TypeScript

**设计文档:** `docs/superpowers/specs/2026-04-02-iterative-optimization-design.md` — P0 章节

---

## 文件结构

### 后端新增文件

| 文件 | 职责 |
|------|------|
| `common/service/CacheService.java` | Redis 缓存操作封装（get/set/delete/deleteByPrefix） |
| `common/service/CacheServiceTest.java` | CacheService 单元测试 |
| `common/annotation/ScheduledTarget.java` | 标记可被定时任务反射调用的方法 |
| `modules/system/scheduler/DynamicTaskManager.java` | 动态任务注册/取消/更新 |
| `modules/system/scheduler/JobRunner.java` | 任务执行器（反射调用 + 日志记录） |
| `modules/system/scheduler/JobInitializer.java` | 启动时加载已启用任务 |
| `modules/system/scheduler/SchedulerConfig.java` | TaskScheduler Bean 配置 |
| `modules/system/scheduler/DynamicTaskManagerTest.java` | 调度引擎测试 |

### 后端修改文件

| 文件 | 改动 |
|------|------|
| `common/constant/CacheConstants.java` | 新增缓存 key 前缀常量 |
| `modules/system/service/impl/DictServiceImpl.java` | 查询加缓存读取，写操作加缓存删除 |
| `modules/system/controller/ConfigController.java` | 重构：提取 ConfigService，加缓存 |
| `modules/system/service/impl/JobServiceImpl.java` | 集成 DynamicTaskManager |

### 前端新增文件

| 文件 | 职责 |
|------|------|
| `stores/modules/dict.ts` | Pinia 字典缓存 store |
| `composables/useDict.ts` | 字典数据 composable 封装 |
| `stores/__tests__/dict.test.ts` | 字典 store 测试 |
| `composables/__tests__/useDict.test.ts` | useDict 测试 |

---

## Task 1：CacheService 缓存工具类

**Files:**
- Modify: `admin-backend/src/main/java/com/iflytek/admin/common/constant/CacheConstants.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/service/CacheService.java`
- Create: `admin-backend/src/test/java/com/iflytek/admin/common/service/CacheServiceTest.java`

- [ ] **Step 1: 扩展 CacheConstants 常量**

```java
// CacheConstants.java — 新增以下常量
public static final String DICT_DATA_PREFIX = "dict:data:";
public static final String DICT_TYPES_KEY = "dict:types";
public static final String CONFIG_ALL_KEY = "config:all";
public static final String CONFIG_PREFIX = "config:";
public static final String MENU_USER_PREFIX = "menu:user:";
public static final String PERM_USER_PREFIX = "perm:user:";
```

- [ ] **Step 2: 编写 CacheService 失败测试**

```java
package com.iflytek.admin.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    private CacheService cacheService;

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
    }

    @Nested
    @DisplayName("get 方法")
    class GetTests {
        @Test
        @DisplayName("key 存在时返回反序列化对象")
        void get_existing_key_returns_value() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("test:key")).thenReturn("hello");

            String result = cacheService.get("test:key", String.class);
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("key 不存在时返回 null")
        void get_missing_key_returns_null() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("test:missing")).thenReturn(null);

            String result = cacheService.get("test:missing", String.class);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Redis 异常时返回 null 不抛异常")
        void get_redis_error_returns_null() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Connection refused"));

            String result = cacheService.get("test:key", String.class);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("set 方法")
    class SetTests {
        @Test
        @DisplayName("正常设置带 TTL 的缓存")
        void set_stores_value_with_ttl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            cacheService.set("test:key", "value", 3600);

            verify(valueOperations).set("test:key", "value", 3600, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Redis 异常时静默失败")
        void set_redis_error_silent() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Connection refused"));

            assertThatCode(() -> cacheService.set("test:key", "value", 3600))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("delete 方法")
    class DeleteTests {
        @Test
        @DisplayName("删除指定 key")
        void delete_removes_key() {
            cacheService.delete("test:key");
            verify(redisTemplate).delete("test:key");
        }
    }

    @Nested
    @DisplayName("deleteByPrefix 方法")
    class DeleteByPrefixTests {
        @Test
        @DisplayName("按前缀批量删除")
        void deleteByPrefix_removes_matching_keys() {
            Set<String> keys = Set.of("dict:data:gender", "dict:data:status");
            when(redisTemplate.keys("dict:data:*")).thenReturn(keys);

            cacheService.deleteByPrefix("dict:data:");

            verify(redisTemplate).delete(keys);
        }

        @Test
        @DisplayName("无匹配 key 时不调用 delete")
        void deleteByPrefix_no_keys_no_delete() {
            when(redisTemplate.keys("dict:data:*")).thenReturn(Set.of());

            cacheService.deleteByPrefix("dict:data:");

            verify(redisTemplate, never()).delete(anyCollection());
        }
    }
}
```

- [ ] **Step 3: 运行测试验证失败**

Run: `cd admin-backend && mvn test -pl . -Dtest=CacheServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，`CacheService` 类不存在

- [ ] **Step 4: 实现 CacheService**

```java
package com.iflytek.admin.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return type.isInstance(value) ? (T) value : null;
        } catch (Exception e) {
            log.warn("Cache get failed for key: {}", key, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAsList(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Cache get failed for key: {}", key, e);
            return null;
        }
    }

    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Cache set failed for key: {}", key, e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Cache delete failed for key: {}", key, e);
        }
    }

    public void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Cache deleteByPrefix failed for prefix: {}", prefix, e);
        }
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd admin-backend && mvn test -pl . -Dtest=CacheServiceTest`
Expected: 全部 PASS

- [ ] **Step 6: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/common/constant/CacheConstants.java \
        admin-backend/src/main/java/com/iflytek/admin/common/service/CacheService.java \
        admin-backend/src/test/java/com/iflytek/admin/common/service/CacheServiceTest.java
git commit -m "feat: 新增 CacheService 缓存工具类及缓存常量"
```

---

## Task 2：字典服务缓存集成

**Files:**
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/DictServiceImpl.java`

- [ ] **Step 1: 修改 DictServiceImpl 注入 CacheService**

在 `DictServiceImpl` 类中新增字段：

```java
private final CacheService cacheService;
```

由于使用 `@RequiredArgsConstructor`，Lombok 会自动在构造器中注入。

- [ ] **Step 2: 为 listTypes 添加缓存**

将 `listTypes()` 方法替换为：

```java
@Override
public List<SysDictType> listTypes() {
    List<SysDictType> cached = cacheService.getAsList(CacheConstants.DICT_TYPES_KEY);
    if (cached != null) return cached;

    List<SysDictType> types = dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
            .orderByDesc(SysDictType::getCreatedTime));
    cacheService.set(CacheConstants.DICT_TYPES_KEY, types, 86400);
    return types;
}
```

- [ ] **Step 3: 为 listDataByType 添加缓存**

将 `listDataByType()` 方法替换为：

```java
@Override
public List<SysDictData> listDataByType(String dictType) {
    String key = CacheConstants.DICT_DATA_PREFIX + dictType;
    List<SysDictData> cached = cacheService.getAsList(key);
    if (cached != null) return cached;

    List<SysDictData> data = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
            .eq(SysDictData::getDictType, dictType)
            .orderByAsc(SysDictData::getSortOrder));
    cacheService.set(key, data, 86400);
    return data;
}
```

- [ ] **Step 4: 在写操作中添加缓存失效**

在 `createType`、`updateType`、`deleteType` 方法末尾添加：

```java
cacheService.delete(CacheConstants.DICT_TYPES_KEY);
```

在 `updateType` 方法（当 dictType 字段改变时）额外添加：

```java
cacheService.delete(CacheConstants.DICT_DATA_PREFIX + oldDictType);
if (dto.getDictType() != null && !oldDictType.equals(dto.getDictType())) {
    cacheService.delete(CacheConstants.DICT_DATA_PREFIX + dto.getDictType());
}
```

在 `deleteType` 方法中添加：

```java
cacheService.delete(CacheConstants.DICT_DATA_PREFIX + type.getDictType());
```

在 `createData`、`updateData`、`deleteData` 方法中添加（需要获取 dictType）：

对于 `createData` 和 `updateData`：
```java
cacheService.delete(CacheConstants.DICT_DATA_PREFIX + dto.getDictType());
```

对于 `deleteData`，需先查出 dictType 再删缓存：
```java
@Override
public void deleteData(Long id) {
    SysDictData data = dictDataMapper.selectById(id);
    dictDataMapper.deleteById(id);
    if (data != null) {
        cacheService.delete(CacheConstants.DICT_DATA_PREFIX + data.getDictType());
    }
}
```

- [ ] **Step 5: 运行已有测试确认不破坏现有功能**

Run: `cd admin-backend && mvn test`
Expected: 全部 PASS（DictServiceImpl 测试如果存在 mock，需要添加 CacheService mock）

- [ ] **Step 6: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/DictServiceImpl.java
git commit -m "feat: 字典服务集成 Redis 缓存（Cache Aside 模式）"
```

---

## Task 3：提取 ConfigService 并集成缓存

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/ConfigService.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/ConfigServiceImpl.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/controller/ConfigController.java`

- [ ] **Step 1: 创建 ConfigService 接口**

```java
package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.entity.SysConfig;
import java.util.List;

public interface ConfigService {
    List<SysConfig> listAll();
    String getValueByKey(String key);
    void batchUpdate(List<SysConfig> configs);
}
```

- [ ] **Step 2: 创建 ConfigServiceImpl 实现（含缓存）**

```java
package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.entity.SysConfig;
import com.iflytek.admin.modules.system.mapper.SysConfigMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigMapper configMapper;
    private final CacheService cacheService;

    @Override
    public List<SysConfig> listAll() {
        List<SysConfig> cached = cacheService.getAsList(CacheConstants.CONFIG_ALL_KEY);
        if (cached != null) return cached;

        List<SysConfig> configs = configMapper.selectList(null);
        cacheService.set(CacheConstants.CONFIG_ALL_KEY, configs, 86400);
        return configs;
    }

    @Override
    public String getValueByKey(String key) {
        String cacheKey = CacheConstants.CONFIG_PREFIX + key;
        String cached = cacheService.get(cacheKey, String.class);
        if (cached != null) return cached;

        SysConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        String value = config != null ? config.getConfigValue() : null;
        if (value != null) {
            cacheService.set(cacheKey, value, 86400);
        }
        return value;
    }

    @Override
    public void batchUpdate(List<SysConfig> configs) {
        configs.forEach(configMapper::updateById);
        cacheService.delete(CacheConstants.CONFIG_ALL_KEY);
        cacheService.deleteByPrefix(CacheConstants.CONFIG_PREFIX);
    }
}
```

- [ ] **Step 3: 修改 ConfigController 使用 ConfigService**

将 `ConfigController` 的依赖从 `SysConfigMapper` 改为 `ConfigService`：

```java
@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "配置列表")
    @PreAuthorize("hasAuthority('sys:config:list')")
    @GetMapping
    public Result<List<SysConfig>> list() {
        return Result.ok(configService.listAll());
    }

    @Operation(summary = "批量更新配置")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @Log(module = "系统配置", operation = "更新")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody List<SysConfig> configs) {
        configService.batchUpdate(configs);
        return Result.ok();
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `cd admin-backend && mvn test`
Expected: 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/service/ConfigService.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/ConfigServiceImpl.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/controller/ConfigController.java
git commit -m "refactor: 提取 ConfigService 并集成 Redis 缓存"
```

---

## Task 4：菜单/权限缓存集成

**Files:**
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/auth/service/impl/AuthServiceImpl.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/MenuServiceImpl.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/RoleServiceImpl.java`

- [ ] **Step 1: 在 AuthServiceImpl.getUserInfo() 中添加菜单/权限缓存**

在 `getUserInfo()` 方法中，查询用户菜单和权限前先检查缓存：

```java
// 在构建 userInfo 时，对菜单树和权限列表加缓存
String menuCacheKey = CacheConstants.MENU_USER_PREFIX + userId;
String permCacheKey = CacheConstants.PERM_USER_PREFIX + userId;

List<Map<String, Object>> menuTree = cacheService.getAsList(menuCacheKey);
List<String> perms = cacheService.getAsList(permCacheKey);

if (menuTree == null) {
    // 原有的菜单查询逻辑
    menuTree = buildMenuTree(...);
    cacheService.set(menuCacheKey, menuTree, 7200);
}

if (perms == null) {
    // 原有的权限查询逻辑
    perms = userMapper.selectUserPermissions(userId);
    cacheService.set(permCacheKey, perms, 7200);
}
```

注入 `CacheService` 到 `AuthServiceImpl`。

- [ ] **Step 2: 在 MenuServiceImpl 的写操作中清除缓存**

在 `create()`、`update()`、`delete()` 方法末尾添加：

```java
cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
```

注入 `CacheService` 到 `MenuServiceImpl`。

- [ ] **Step 3: 在 RoleServiceImpl 的写操作中清除缓存**

在 `assignMenus()`、`update()`、`delete()` 方法末尾添加：

```java
cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
```

注入 `CacheService` 到 `RoleServiceImpl`。

- [ ] **Step 4: 运行全部测试**

Run: `cd admin-backend && mvn test`
Expected: 全部 PASS（需要在已有测试中添加 CacheService mock）

- [ ] **Step 5: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/auth/service/impl/AuthServiceImpl.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/MenuServiceImpl.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/RoleServiceImpl.java
git commit -m "feat: 菜单树和用户权限集成 Redis 缓存"
```

---

## Task 5：@ScheduledTarget 注解和 SchedulerConfig

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/common/annotation/ScheduledTarget.java`
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/SchedulerConfig.java`

- [ ] **Step 1: 创建 @ScheduledTarget 注解**

```java
package com.iflytek.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记可被定时任务调度器反射调用的方法。
 * 未标注此注解的方法不允许被 JobRunner 调用，防止 RCE 攻击。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledTarget {
}
```

- [ ] **Step 2: 创建 SchedulerConfig**

```java
package com.iflytek.admin.modules.system.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("job-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/common/annotation/ScheduledTarget.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/SchedulerConfig.java
git commit -m "feat: 新增 @ScheduledTarget 安全注解和 TaskScheduler 配置"
```

---

## Task 6：JobRunner 任务执行器

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/JobRunner.java`
- Create: `admin-backend/src/test/java/com/iflytek/admin/modules/system/scheduler/JobRunnerTest.java`

- [ ] **Step 1: 编写 JobRunner 失败测试**

```java
package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.common.annotation.ScheduledTarget;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.mapper.SysJobLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunnerTest {

    private JobRunner jobRunner;

    @Mock private ApplicationContext applicationContext;
    @Mock private SysJobLogMapper jobLogMapper;

    @BeforeEach
    void setUp() {
        jobRunner = new JobRunner(applicationContext, jobLogMapper);
    }

    // 测试用目标 Bean
    public static class TestTaskBean {
        public boolean executed = false;

        @ScheduledTarget
        public void doWork() { executed = true; }

        public void unsafeMethod() { /* 无注解 */ }
    }

    @Nested
    @DisplayName("执行成功")
    class SuccessTests {
        @Test
        @DisplayName("调用标注 @ScheduledTarget 的方法并记录成功日志")
        void execute_annotated_method_success() {
            SysJob job = new SysJob();
            job.setId(1L);
            job.setJobName("测试任务");
            job.setInvokeTarget("testTaskBean.doWork");

            TestTaskBean bean = new TestTaskBean();
            when(applicationContext.getBean("testTaskBean")).thenReturn(bean);

            jobRunner.execute(job);

            assertThat(bean.executed).isTrue();

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            SysJobLog log = logCaptor.getValue();
            assertThat(log.getJobId()).isEqualTo(1L);
            assertThat(log.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("安全限制")
    class SecurityTests {
        @Test
        @DisplayName("拒绝调用未标注 @ScheduledTarget 的方法")
        void reject_unannotated_method() {
            SysJob job = new SysJob();
            job.setId(2L);
            job.setJobName("危险任务");
            job.setInvokeTarget("testTaskBean.unsafeMethod");

            TestTaskBean bean = new TestTaskBean();
            when(applicationContext.getBean("testTaskBean")).thenReturn(bean);

            jobRunner.execute(job);

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            SysJobLog log = logCaptor.getValue();
            assertThat(log.getStatus()).isEqualTo(0);
            assertThat(log.getErrorMsg()).contains("@ScheduledTarget");
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ErrorTests {
        @Test
        @DisplayName("Bean 不存在时记录失败日志")
        void bean_not_found_logs_error() {
            SysJob job = new SysJob();
            job.setId(3L);
            job.setJobName("不存在的任务");
            job.setInvokeTarget("nonExistent.method");

            when(applicationContext.getBean("nonExistent"))
                    .thenThrow(new RuntimeException("No bean named 'nonExistent'"));

            jobRunner.execute(job);

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            assertThat(logCaptor.getValue().getStatus()).isEqualTo(0);
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd admin-backend && mvn test -Dtest=JobRunnerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败，`JobRunner` 类不存在

- [ ] **Step 3: 实现 JobRunner**

```java
package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.common.annotation.ScheduledTarget;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.mapper.SysJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner {

    private final ApplicationContext applicationContext;
    private final SysJobLogMapper jobLogMapper;

    public void execute(SysJob job) {
        long startTime = System.currentTimeMillis();
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setInvokeTarget(job.getInvokeTarget());

        try {
            String invokeTarget = job.getInvokeTarget();
            int lastDot = invokeTarget.lastIndexOf('.');
            String beanName = invokeTarget.substring(0, lastDot);
            String methodName = invokeTarget.substring(lastDot + 1);

            Object bean = applicationContext.getBean(beanName);
            Method method = bean.getClass().getMethod(methodName);

            if (!method.isAnnotationPresent(ScheduledTarget.class)) {
                throw new SecurityException(
                        "方法 " + invokeTarget + " 未标注 @ScheduledTarget，拒绝执行");
            }

            method.invoke(bean);

            jobLog.setStatus(1);
            jobLog.setMessage("执行成功");
        } catch (Exception e) {
            log.error("任务执行失败: {}", job.getInvokeTarget(), e);
            jobLog.setStatus(0);
            jobLog.setErrorMsg(e.getMessage() != null ? e.getMessage() :
                    e.getCause() != null ? e.getCause().getMessage() : "未知错误");
        } finally {
            jobLog.setDuration(System.currentTimeMillis() - startTime);
            jobLog.setCreatedTime(LocalDateTime.now());
            jobLogMapper.insert(jobLog);
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd admin-backend && mvn test -Dtest=JobRunnerTest`
Expected: 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/JobRunner.java \
        admin-backend/src/test/java/com/iflytek/admin/modules/system/scheduler/JobRunnerTest.java
git commit -m "feat: 新增 JobRunner 任务执行器（含 @ScheduledTarget 安全校验）"
```

---

## Task 7：DynamicTaskManager 动态任务管理器

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/DynamicTaskManager.java`
- Create: `admin-backend/src/test/java/com/iflytek/admin/modules/system/scheduler/DynamicTaskManagerTest.java`

- [ ] **Step 1: 编写 DynamicTaskManager 失败测试**

```java
package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.modules.system.entity.SysJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicTaskManagerTest {

    private DynamicTaskManager taskManager;

    @Mock private ThreadPoolTaskScheduler taskScheduler;
    @Mock private JobRunner jobRunner;
    @Mock private ScheduledFuture<?> scheduledFuture;

    @BeforeEach
    void setUp() {
        taskManager = new DynamicTaskManager(taskScheduler, jobRunner);
    }

    private SysJob createJob(Long id, String cron) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setJobName("测试任务");
        job.setInvokeTarget("testBean.method");
        job.setCronExpression(cron);
        return job;
    }

    @Nested
    @DisplayName("addTask")
    class AddTaskTests {
        @Test
        @DisplayName("注册 cron 任务到调度器")
        void addTask_registers_with_scheduler() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                    .thenReturn(scheduledFuture);

            taskManager.addTask(job);

            verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
            assertThat(taskManager.isRunning(1L)).isTrue();
        }

        @Test
        @DisplayName("无效 cron 表达式不注册")
        void addTask_invalid_cron_does_not_register() {
            SysJob job = createJob(2L, "invalid-cron");

            taskManager.addTask(job);

            verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
            assertThat(taskManager.isRunning(2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("removeTask")
    class RemoveTaskTests {
        @Test
        @DisplayName("取消正在运行的任务")
        void removeTask_cancels_future() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                    .thenReturn(scheduledFuture);
            taskManager.addTask(job);

            taskManager.removeTask(1L);

            verify(scheduledFuture).cancel(false);
            assertThat(taskManager.isRunning(1L)).isFalse();
        }

        @Test
        @DisplayName("移除不存在的任务不抛异常")
        void removeTask_nonexistent_no_error() {
            assertThatCode(() -> taskManager.removeTask(999L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {
        @Test
        @DisplayName("更新任务先取消再重新注册")
        void updateTask_removes_then_adds() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
                    .thenReturn(scheduledFuture);
            taskManager.addTask(job);

            SysJob updated = createJob(1L, "0/10 * * * * ?");
            taskManager.updateTask(updated);

            verify(scheduledFuture).cancel(false);
            verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
        }
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd admin-backend && mvn test -Dtest=DynamicTaskManagerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败

- [ ] **Step 3: 实现 DynamicTaskManager**

```java
package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.modules.system.entity.SysJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicTaskManager {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final JobRunner jobRunner;
    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    public void addTask(SysJob job) {
        try {
            CronTrigger trigger = new CronTrigger(job.getCronExpression());
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> jobRunner.execute(job), trigger);
            runningTasks.put(job.getId(), future);
            log.info("任务已注册: id={}, name={}, cron={}", job.getId(), job.getJobName(), job.getCronExpression());
        } catch (IllegalArgumentException e) {
            log.error("无效的 cron 表达式: job={}, cron={}", job.getId(), job.getCronExpression(), e);
        }
    }

    public void removeTask(Long jobId) {
        ScheduledFuture<?> future = runningTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("任务已取消: id={}", jobId);
        }
    }

    public void updateTask(SysJob job) {
        removeTask(job.getId());
        addTask(job);
    }

    public boolean isRunning(Long jobId) {
        return runningTasks.containsKey(jobId);
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd admin-backend && mvn test -Dtest=DynamicTaskManagerTest`
Expected: 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/DynamicTaskManager.java \
        admin-backend/src/test/java/com/iflytek/admin/modules/system/scheduler/DynamicTaskManagerTest.java
git commit -m "feat: 新增 DynamicTaskManager 动态任务调度管理器"
```

---

## Task 8：JobInitializer 启动加载 + JobService 集成

**Files:**
- Create: `admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/JobInitializer.java`
- Modify: `admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/JobServiceImpl.java`

- [ ] **Step 1: 创建 JobInitializer**

```java
package com.iflytek.admin.modules.system.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobInitializer implements ApplicationRunner {

    private final SysJobMapper jobMapper;
    private final DynamicTaskManager taskManager;

    @Override
    public void run(ApplicationArguments args) {
        List<SysJob> activeJobs = jobMapper.selectList(
                new LambdaQueryWrapper<SysJob>().eq(SysJob::getStatus, 1));
        log.info("启动加载定时任务: 共 {} 个", activeJobs.size());
        activeJobs.forEach(taskManager::addTask);
    }
}
```

- [ ] **Step 2: 修改 JobServiceImpl 集成 DynamicTaskManager**

在 `JobServiceImpl` 中注入新依赖：

```java
private final DynamicTaskManager taskManager;
private final JobRunner jobRunner;
```

修改以下方法：

`create()` — 如果 status=1，注册到调度器：
```java
@Override
public void create(JobFormDTO dto) {
    SysJob job = new SysJob();
    applyDTO(job, dto);
    jobMapper.insert(job);
    if (Integer.valueOf(1).equals(job.getStatus())) {
        taskManager.addTask(job);
    }
}
```

`update()` — 调用 updateTask：
```java
@Override
public void update(Long id, JobFormDTO dto) {
    SysJob job = jobMapper.selectById(id);
    if (job == null) throw new BusinessException(404, "任务不存在");
    applyDTO(job, dto);
    jobMapper.updateById(job);
    if (Integer.valueOf(1).equals(job.getStatus())) {
        taskManager.updateTask(job);
    } else {
        taskManager.removeTask(id);
    }
}
```

`delete()` — 先取消再删除：
```java
@Override
public void delete(Long id) {
    taskManager.removeTask(id);
    jobMapper.deleteById(id);
}
```

`changeStatus()` — 启用/停用：
```java
@Override
public void changeStatus(Long id, Integer status) {
    SysJob job = jobMapper.selectById(id);
    if (job == null) throw new BusinessException(404, "任务不存在");
    job.setStatus(status);
    jobMapper.updateById(job);
    if (Integer.valueOf(1).equals(status)) {
        taskManager.addTask(job);
    } else {
        taskManager.removeTask(id);
    }
}
```

`run()` — 使用 JobRunner 执行：
```java
@Override
public void run(Long id) {
    SysJob job = jobMapper.selectById(id);
    if (job == null) throw new BusinessException(404, "任务不存在");
    jobRunner.execute(job);
}
```

删除原有的 `executeJob()` 私有方法和 `applicationContext` 字段（逻辑已移至 JobRunner）。

- [ ] **Step 3: 运行全部测试**

Run: `cd admin-backend && mvn test`
Expected: 全部 PASS（JobServiceImpl 测试需要增加 taskManager 和 jobRunner 的 mock）

- [ ] **Step 4: 提交**

```bash
git add admin-backend/src/main/java/com/iflytek/admin/modules/system/scheduler/JobInitializer.java \
        admin-backend/src/main/java/com/iflytek/admin/modules/system/service/impl/JobServiceImpl.java
git commit -m "feat: 集成定时任务调度引擎（启动加载 + JobService 联动）"
```

---

## Task 9：前端字典 Store

**Files:**
- Create: `admin-frontend/src/stores/modules/dict.ts`
- Create: `admin-frontend/src/stores/__tests__/dict.test.ts`

- [ ] **Step 1: 编写字典 store 失败测试**

```typescript
// admin-frontend/src/stores/__tests__/dict.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDictStore } from '../modules/dict'

vi.mock('@/api/modules/dict', () => ({
  getDictDataByType: vi.fn()
}))

import { getDictDataByType } from '@/api/modules/dict'
const mockedGetDictData = vi.mocked(getDictDataByType)

describe('useDictStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('getDictData', () => {
    it('首次调用请求 API 并缓存结果', async () => {
      const mockData = [
        { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' },
        { id: 2, dictType: 'sys_gender', dictLabel: '女', dictValue: '2', sortOrder: 2, status: 1, description: '', createdTime: '' }
      ]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      const result = await store.getDictData('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledWith('sys_gender')
      expect(result).toEqual(mockData)
    })

    it('第二次调用使用缓存不再请求 API', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      await store.getDictData('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledTimes(1)
    })
  })

  describe('getDictLabel', () => {
    it('根据 value 返回对应 label', async () => {
      const mockData = [
        { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
      ]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      const label = await store.getDictLabel('sys_gender', '1')

      expect(label).toBe('男')
    })

    it('未找到 value 返回原始值', async () => {
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: [] } as any)

      const store = useDictStore()
      const label = await store.getDictLabel('sys_gender', '99')

      expect(label).toBe('99')
    })
  })

  describe('refreshDict', () => {
    it('强制刷新清除缓存并重新请求', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      await store.refreshDict('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledTimes(2)
    })
  })

  describe('clearAll', () => {
    it('清空所有缓存', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      store.clearAll()

      // 再次请求应该重新调用 API
      await store.getDictData('sys_gender')
      expect(mockedGetDictData).toHaveBeenCalledTimes(2)
    })
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd admin-frontend && npx vitest run src/stores/__tests__/dict.test.ts`
Expected: FAIL — `useDictStore` 不存在

- [ ] **Step 3: 实现 useDictStore**

```typescript
// admin-frontend/src/stores/modules/dict.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByType } from '@/api/modules/dict'
import type { DictData } from '@/types/dict'

export const useDictStore = defineStore('dict', () => {
  const dictMap = ref<Record<string, DictData[]>>({})
  const pendingRequests = new Map<string, Promise<DictData[]>>()

  async function getDictData(dictType: string): Promise<DictData[]> {
    if (dictMap.value[dictType]) {
      return dictMap.value[dictType]
    }

    // 防止同一 dictType 并发请求
    if (pendingRequests.has(dictType)) {
      return pendingRequests.get(dictType)!
    }

    const promise = getDictDataByType(dictType).then(({ data }) => {
      dictMap.value = { ...dictMap.value, [dictType]: data }
      pendingRequests.delete(dictType)
      return data
    }).catch(() => {
      pendingRequests.delete(dictType)
      return [] as DictData[]
    })

    pendingRequests.set(dictType, promise)
    return promise
  }

  async function getDictLabel(dictType: string, value: string): Promise<string> {
    const data = await getDictData(dictType)
    const item = data.find(d => d.dictValue === value)
    return item ? item.dictLabel : value
  }

  async function refreshDict(dictType: string): Promise<DictData[]> {
    const { [dictType]: _, ...rest } = dictMap.value
    dictMap.value = rest
    return getDictData(dictType)
  }

  function clearAll() {
    dictMap.value = {}
    pendingRequests.clear()
  }

  return { dictMap, getDictData, getDictLabel, refreshDict, clearAll }
})
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd admin-frontend && npx vitest run src/stores/__tests__/dict.test.ts`
Expected: 全部 PASS

- [ ] **Step 5: 提交**

```bash
git add admin-frontend/src/stores/modules/dict.ts \
        admin-frontend/src/stores/__tests__/dict.test.ts
git commit -m "feat: 新增前端字典缓存 Store（useDictStore）"
```

---

## Task 10：前端 useDict Composable

**Files:**
- Create: `admin-frontend/src/composables/useDict.ts`
- Create: `admin-frontend/src/composables/__tests__/useDict.test.ts`

- [ ] **Step 1: 编写 useDict 失败测试**

```typescript
// admin-frontend/src/composables/__tests__/useDict.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { nextTick } from 'vue'
import { useDict, useDictLabel } from '../useDict'

vi.mock('@/api/modules/dict', () => ({
  getDictDataByType: vi.fn()
}))

import { getDictDataByType } from '@/api/modules/dict'
const mockedGetDictData = vi.mocked(getDictDataByType)

describe('useDict', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('返回响应式的字典数据列表', async () => {
    const mockData = [
      { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
    ]
    mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

    const { data, loading } = useDict('sys_gender')

    expect(loading.value).toBe(true)
    await nextTick()
    // 等待异步完成
    await new Promise(resolve => setTimeout(resolve, 10))

    expect(data.value).toEqual(mockData)
    expect(loading.value).toBe(false)
  })
})

describe('useDictLabel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('返回响应式的标签文本', async () => {
    const mockData = [
      { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
    ]
    mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

    const label = useDictLabel('sys_gender', '1')

    await new Promise(resolve => setTimeout(resolve, 10))

    expect(label.value).toBe('男')
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd admin-frontend && npx vitest run src/composables/__tests__/useDict.test.ts`
Expected: FAIL — `useDict` 不存在

- [ ] **Step 3: 实现 useDict composable**

```typescript
// admin-frontend/src/composables/useDict.ts
import { ref, onMounted } from 'vue'
import { useDictStore } from '@/stores/modules/dict'
import type { DictData } from '@/types/dict'

export function useDict(dictType: string) {
  const data = ref<DictData[]>([])
  const loading = ref(true)

  onMounted(async () => {
    try {
      const store = useDictStore()
      data.value = await store.getDictData(dictType)
    } finally {
      loading.value = false
    }
  })

  return { data, loading }
}

export function useDictLabel(dictType: string, value: string) {
  const label = ref(value)

  onMounted(async () => {
    const store = useDictStore()
    label.value = await store.getDictLabel(dictType, value)
  })

  return label
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `cd admin-frontend && npx vitest run src/composables/__tests__/useDict.test.ts`
Expected: 全部 PASS

- [ ] **Step 5: 在 user store 的 logout 中清空字典缓存**

修改 `admin-frontend/src/stores/modules/user.ts` 的 `resetState()` 方法：

```typescript
import { useDictStore } from './dict'

function resetState() {
  token.value = ''
  userInfo.value = null
  roles.value = []
  permissions.value = []
  menus.value = []
  clearAuth()
  useDictStore().clearAll()
}
```

- [ ] **Step 6: 运行全部前端测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部 PASS

- [ ] **Step 7: 提交**

```bash
git add admin-frontend/src/composables/useDict.ts \
        admin-frontend/src/composables/__tests__/useDict.test.ts \
        admin-frontend/src/stores/modules/user.ts
git commit -m "feat: 新增 useDict composable 和登出时清空字典缓存"
```

---

## Task 11：全部测试验证 + 最终提交

- [ ] **Step 1: 运行后端全部测试**

Run: `cd admin-backend && mvn test`
Expected: 全部 PASS

- [ ] **Step 2: 运行前端全部测试**

Run: `cd admin-frontend && npx vitest run`
Expected: 全部 PASS

- [ ] **Step 3: 运行前端类型检查**

Run: `cd admin-frontend && npx vue-tsc --noEmit`
Expected: 无类型错误

- [ ] **Step 4: 运行前端 lint**

Run: `cd admin-frontend && npx eslint src/`
Expected: 无 error（warning 可接受）

- [ ] **Step 5: 确认 P0 完成**

P0 三个优化项全部完成：
1. ✅ Redis 缓存策略（CacheService + 字典/配置/菜单缓存）
2. ✅ 定时任务调度引擎（DynamicTaskManager + JobRunner + JobInitializer）
3. ✅ 前端字典数据缓存（useDictStore + useDict）
