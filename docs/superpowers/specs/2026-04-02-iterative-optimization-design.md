# 后台管理系统 - 迭代优化设计文档

> 日期：2026-04-02
> 策略：方案三 - 均衡推进
> 目标：系统已在生产环境使用，持续迭代演进
> 总计：4 个阶段，14 个优化项

---

## 阶段总览

| 阶段 | 主题 | 优化项数 | 依赖关系 |
|------|------|---------|---------|
| P0 | 核心基础设施 | 3 | 无 |
| P1 | 安全 + 通知 | 5 | P0（缓存基础设施） |
| P2 | 存储 + 导入 | 3 | P0（缓存）、P1（导入默认密码依赖密码策略） |
| P3 | 性能 + 质量 | 3 | P0-P2（测试覆盖所有新代码） |

---

## P0 - 核心基础设施

### P0-1：Redis 缓存策略

**问题**：字典数据、系统配置、菜单树属于高频读取低频变更的数据，当前每次请求都查询数据库。

**模式**：Cache Aside（旁路缓存）— 先读缓存，未命中则查 DB 并写入缓存；写操作先更新 DB 再删除缓存。

**缓存方案**：

| 缓存对象 | Redis Key | 过期时间 | 失效时机 |
|---------|-----------|---------|---------|
| 按类型的字典数据 | `dict:data:{dictType}` | 24h | 字典数据增删改 |
| 所有字典类型 | `dict:types` | 24h | 字典类型增删改 |
| 全部系统配置 | `config:all` | 24h | 配置更新 |
| 单项配置 | `config:{key}` | 24h | 配置更新 |
| 用户菜单树 | `menu:user:{userId}` | 2h | 菜单/角色变更（清除受影响的用户） |
| 用户权限列表 | `perm:user:{userId}` | 2h | 菜单/角色变更（清除受影响的用户） |

**后端改动**：

1. `CacheConstants` — 新增上述所有缓存项的 key 前缀常量
2. 新增 `CacheService` 工具类：
   - `get(key, Class<T>)` / `set(key, value, ttl)` / `delete(key)` / `deleteByPrefix(prefix)`
   - 封装 `RedisTemplate`，统一 JSON 序列化
3. `DictServiceImpl` — 查询方法加缓存读取，写方法加缓存删除
4. `ConfigServiceImpl` — 同上
5. `MenuServiceImpl` — 缓存用户菜单树；菜单/角色变更时，通过角色关联查询受影响的用户 ID，批量清除缓存

**关键决策**：手动控制缓存（不使用 Spring `@Cacheable`），以便精确控制粒度，尤其是菜单/角色级联失效场景。

---

### P0-2：定时任务调度引擎集成

**问题**：`SysJob` 已有完整的 CRUD，但缺少真正的调度执行能力。Cron 表达式只存储在数据库中，没有被调度器消费。

**架构**：

```
SysJob (DB) <-> JobService <-> DynamicTaskManager <-> TaskScheduler
                                      |
                              ScheduledFuture Map（内存）
                                      |
                              反射调用 invokeTarget
```

**新增组件**：

1. `DynamicTaskManager`：
   - `Map<Long, ScheduledFuture<?>>` — 跟踪运行中的任务
   - `addTask(job)` — 解析 cron，注册到 `ThreadPoolTaskScheduler`
   - `removeTask(jobId)` — 取消 `ScheduledFuture`
   - `updateTask(job)` — 先移除再添加
2. `JobRunner`：
   - 反射调用 `invokeTarget`（格式：`beanName.methodName`）
   - 执行前后写入 `SysJobLog`（状态、耗时、异常信息）
   - 异常隔离：捕获并记录，不影响调度器
3. `JobInitializer`（`ApplicationRunner`）：
   - 应用启动时加载所有 `status=1` 的任务并注册到调度器
4. `@ScheduledTarget` 注解：
   - 安全限制：只有标注此注解的 Spring Bean 方法才可被反射调用
   - 防止任意类反射调用（防 RCE 攻击）

**JobService 改动**：
- `changeStatus()` — 启用调用 `addTask`，停用调用 `removeTask`
- `create()` — 如果 status=1，同时注册到调度器
- `update()` — 调用 `updateTask`
- `delete()` — 调用 `removeTask`
- `run()` — 立即执行一次，直接调用 `JobRunner`

---

### P0-3：前端字典数据缓存

**问题**：前端页面渲染时频繁请求字典接口（如性别、状态等），每次打开页面都重复请求。

**新增文件**：

1. `stores/modules/dict.ts`（Pinia store）：
   - 状态：`dictMap: Record<string, DictData[]>`
   - `getDictData(dictType)` — 带内存缓存的异步获取
   - `getDictLabel(dictType, value)` — 根据值获取标签（用于表格显示）
   - `refreshDict(dictType)` — 强制刷新指定字典
   - `clearAll()` — 登出时清空
2. `composables/useDict.ts`：
   - `useDict(dictType)` — 返回响应式的字典数据列表
   - `useDictLabel(dictType, value)` — 返回响应式的标签文本

**使用示例**：
```vue
<!-- 下拉框 -->
<el-option v-for="item in useDict('sys_gender')" :label="item.dictLabel" :value="item.dictValue" />

<!-- 表格列 -->
<template #default="{ row }">{{ getDictLabel('sys_status', row.status) }}</template>
```

---

## P1 - 安全 + 通知

### P1-1：密码安全策略

**问题**：当前密码仅校验 6-20 字符长度，缺少强度要求、过期机制和历史密码检查。

**可配置规则**（通过 `sys_config` 配置）：

| 配置项 | 默认值 | 说明 |
|--------|-------|------|
| `pwd.min.length` | 8 | 密码最小长度 |
| `pwd.require.uppercase` | true | 至少包含 1 个大写字母 |
| `pwd.require.lowercase` | true | 至少包含 1 个小写字母 |
| `pwd.require.digit` | true | 至少包含 1 个数字 |
| `pwd.require.special` | false | 至少包含 1 个特殊字符 |
| `pwd.expire.days` | 90 | 密码有效期（天），0=永不过期 |
| `pwd.history.count` | 3 | 不能与最近 N 次密码重复 |

**数据库变更**：

```sql
-- sys_user 新增字段
ALTER TABLE sys_user ADD COLUMN password_changed_time DATETIME NULL COMMENT '最后修改密码时间';

-- 新增密码历史表
CREATE TABLE sys_password_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  password VARCHAR(100) NOT NULL,
  created_time DATETIME NOT NULL,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**后端改动**：

1. 新增 `PasswordPolicyService`：
   - `validate(rawPassword)` — 从配置读取规则，校验密码强度，返回错误列表
   - `checkHistory(userId, rawPassword)` — BCrypt 匹配历史密码
   - `recordHistory(userId, encodedPassword)` — 记录密码历史
   - `isExpired(user)` — 检查 `password_changed_time` 是否超过有效期
2. `AuthServiceImpl.login()` — 登录成功后检查密码是否过期，过期则在响应中标记 `passwordExpired: true`
3. `UserServiceImpl.create/update()` + `ProfileController.updatePassword()` — 调用 validate + checkHistory
4. `LoginResponse` — 新增 `passwordExpired` 布尔字段

**前端改动**：
- 登录流程：如果 `passwordExpired` 为 true，弹出强制修改密码对话框
- 密码输入框：实时强度指示器（弱/中/强颜色条）

---

### P1-2：接口幂等性

**问题**：缺少防重复提交机制。网络抖动或用户双击可能导致重复创建数据。

**流程**：

```
1. 前端进入表单页 → GET /api/idempotent/token → 后端生成 UUID 存入 Redis（TTL=10min）→ 返回 token
2. 前端提交表单 → Header 携带 X-Idempotent-Token
3. 后端拦截器 → Redis DELETE token（原子操作）→ 删除成功=首次提交，删除失败=重复提交
```

**后端改动**：

1. 新增 `@Idempotent` 注解：
   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Idempotent {
       int expireSeconds() default 600;
   }
   ```
2. 新增 `IdempotentController`：
   - `GET /api/idempotent/token` — 生成并返回幂等 token
3. 新增 `IdempotentAspect`：
   - 拦截 `@Idempotent` 标注的方法
   - 从 Header 提取 `X-Idempotent-Token`
   - 通过 Redis 原子 DELETE 操作判断首次/重复提交
   - 重复提交返回 `ResultCode.DUPLICATE_SUBMIT (40006)`
4. 在所有 POST 创建接口上添加 `@Idempotent`

**前端改动**：

1. 新增 `composables/useIdempotent.ts`：
   - `fetchToken()` — 进入表单时获取 token
   - 自动注入到 axios 请求 header
2. `api/request.ts` — 请求拦截器判断是否有幂等 token，有则注入 Header
3. 各表单对话框 `onOpen` 时调用 `fetchToken()`

---

### P1-3：WebSocket 实时通知推送

**问题**：通知公告系统目前只有 CRUD，用户需要刷新页面才能看到新通知，无法做到实时推送。

**架构**：

```
后端发布通知 → NoticeService.publish() → WebSocketMessageBroker
  → /topic/notice/broadcast（公告广播）
  → /queue/notice/{userId}（定向通知）

前端 → STOMP Client 订阅 → 收到消息 → 通知 Store 更新 → Header 铃铛图标显示未读数
```

**后端改动**：

1. `pom.xml` — 新增依赖 `spring-boot-starter-websocket`
2. 新增 `WebSocketConfig`：
   - STOMP 端点：`/ws`（支持 SockJS 降级）
   - 消息代理：`/topic`（广播）、`/queue`（点对点）
3. 新增 `WebSocketAuthInterceptor`（`ChannelInterceptor`）：
   - 在 CONNECT 帧中验证 JWT token
   - 将用户信息注入 STOMP session
4. `NoticeServiceImpl.publish()`：
   - 公告类型 → 广播到 `/topic/notice/broadcast`
   - 通知类型 → 逐个推送到 `/queue/notice/{userId}`
5. `SecurityConfig` — 将 `/ws/**` 加入公开端点（认证在 STOMP 层处理）

**前端改动**：

1. 新增依赖：`@stomp/stompjs` + `sockjs-client`
2. 新增 `composables/useWebSocket.ts`：
   - `connect()` — 登录后携带 token 建立连接
   - `subscribe(destination, callback)` — 订阅频道
   - `disconnect()` — 登出时断开连接
   - 自动重连机制（指数退避，最大 30 秒）
3. 新增 `stores/modules/notice.ts`：
   - 状态：`unreadCount`、`latestNotices`
   - `fetchUnreadCount()` — 初始加载未读数
   - `onNewNotice(notice)` — WebSocket 回调，更新未读数 + 弹出 ElNotification
   - `markAsRead(noticeId)` — 标记已读
4. `Layout/Header.vue` — 铃铛图标 + 未读数角标（Badge），点击展开最近通知面板

---

### P1-4：表格增强（列设置/密度）

**问题**：所有数据表格缺少个性化设置，所有用户看到相同的列和密度。

**新增组件**：

1. `components/TableToolbar.vue`：
   - 密度切换：宽松/默认/紧凑（对应 el-table 的 `size` 属性：large/default/small）
   - 列显隐：每列一个 Checkbox 控制
   - 列排序：拖拽排列（复用已有的 `sortablejs` 依赖）
   - 刷新按钮
   - 重置为默认
2. 新增 `composables/useTableSettings.ts`：
   - `useTableSettings(tableId)` — 返回 `{ density, columns, resetColumns }`
   - 持久化到 LocalStorage，key 为当前路由路径

**集成方式**：各列表页引入 `<TableToolbar>` 放在表格上方即可，侵入性小。

---

### P1-5：仪表盘可编辑布局

**问题**：仪表盘布局固定，用户无法自定义卡片排列。

**三种操作**：

| 操作 | 触发方式 | 行为 |
|------|---------|------|
| 拖拽排序 | 编辑态拖拽手柄 | vuedraggable 换位 |
| 删除卡片 | 编辑态点击 ✕ 按钮 | 从布局中移除，进入"可添加"池 |
| 添加卡片 | 点击"+ 添加卡片"按钮 | 弹出面板展示可用组件，点击即添加到末尾 |

**组件注册表**（`dashboard/widgets/registry.ts`）：

| 组件 ID | 名称 | 类型 |
|---------|------|------|
| `stat-users` | 用户统计 | stat |
| `stat-roles` | 角色统计 | stat |
| `stat-menus` | 菜单统计 | stat |
| `stat-today-login` | 今日登录 | stat |
| `stat-online` | 在线用户数 | stat |
| `chart-login-trend` | 登录趋势（7天） | chart |
| `chart-role-dist` | 角色分布 | chart |
| `chart-dept-dist` | 部门人员分布 | chart |
| `table-recent-ops` | 最近操作日志 | table |
| `table-recent-login` | 最近登录记录 | table |
| `table-recent-notice` | 最新公告 | table |
| `shortcut-links` | 快捷入口 | shortcut |

**Store**（`stores/modules/dashboard.ts`）：

```typescript
interface DashboardState {
  activeWidgets: string[]   // 当前布局中的有序 widget ID 列表
  editMode: boolean
}

// Actions
addWidget(widgetId)          // 添加卡片到末尾
removeWidget(widgetId)       // 从布局中移除
reorderWidgets(newOrder)     // 拖拽后更新顺序
resetLayout()                // 恢复默认布局
toggleEditMode()             // 切换编辑模式

// Getters
availableWidgets             // 注册表中有但当前布局中没有的 = 可添加列表
activeWidgetMetas            // activeWidgets 映射到完整 WidgetMeta 信息
```

**组件结构**：

```
views/dashboard/
├── index.vue
├── components/
│   ├── DashboardGrid.vue        # vuedraggable 容器
│   ├── WidgetWrapper.vue        # 卡片外壳（标题、编辑态拖拽手柄/删除按钮）
│   ├── AddWidgetDialog.vue      # 添加卡片弹窗（网格展示可用组件）
│   └── widgets/
│       ├── registry.ts          # 组件注册表
│       ├── StatCard.vue
│       ├── ChartLoginTrend.vue
│       ├── ChartRoleDist.vue
│       ├── ChartDeptDist.vue
│       ├── TableRecentOps.vue
│       ├── TableRecentLogin.vue
│       ├── TableRecentNotice.vue
│       ├── ShortcutLinks.vue
│       └── StatOnline.vue
```

**动态渲染**：`DashboardGrid` 通过 `defineAsyncComponent` + 注册表映射动态加载组件。新增卡片只需在注册表添加条目 + 创建组件文件即可，无需修改容器代码。

**持久化**：布局保存到 LocalStorage（key: `dashboard-layout`）。

---

## P2 - 存储 + 导入

### P2-1：文件存储抽象（本地/OSS）

**问题**：文件上传仅支持本地磁盘存储。生产环境面临单机容量限制、服务器迁移丢文件、无法多实例部署等风险。

**模式**：策略模式抽象存储层。

```
FileController → FileService → StorageStrategy（接口）
                                  ├── LocalStorageStrategy    （开发环境）
                                  └── AliyunOssStrategy       （生产环境）
```

**接口定义**：

```java
public interface StorageStrategy {
    FileUploadResult upload(InputStream input, String fileName, String contentType);
    InputStream download(String storedPath);
    void delete(String storedPath);
    String getAccessUrl(String storedPath);
}
```

**配置驱动**：

```yaml
storage:
  type: ${STORAGE_TYPE:local}       # local | aliyun-oss
  local:
    base-path: ${UPLOAD_PATH:./uploads}
  aliyun-oss:
    endpoint: ${OSS_ENDPOINT:}
    access-key-id: ${OSS_ACCESS_KEY_ID:}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET:}
    bucket-name: ${OSS_BUCKET:}
    base-dir: ${OSS_BASE_DIR:admin-files}
```

**新增类**：
1. `StorageProperties` — 读取配置
2. `StorageAutoConfiguration` — 根据 `storage.type` 注册对应 Bean
3. `LocalStorageStrategy` — 从现有 `FileServiceImpl` 中提取存储逻辑
4. `AliyunOssStrategy` — 使用 `aliyun-oss-sdk`

**数据库变更**：`sys_file` 新增 `storage_type VARCHAR(20)` 字段，记录每个文件的存储位置，支持混合存储共存。

**迁移兼容**：已有文件的 `storage_type` 默认为 `local`。下载时根据此字段选择对应 Strategy 读取，新旧文件可共存。

---

### P2-2：数据导入（Excel）

**问题**：当前只有导出功能，缺少导入。管理员批量导入用户、字典数据等常见需求无法满足。

**导入流程**：

```
1. 下载模板     GET  /api/users/import-template     → 返回带表头的空 Excel
2. 填写上传     POST /api/users/import              → 上传填好的 Excel 文件
3. 后端解析校验  → 逐行校验 → 收集错误
4. 返回结果      → { successCount, failCount, errors: [{row, field, message}] }
5. 前端展示      → 成功数 + 失败明细表格
```

**通用框架**：

1. `@ExcelColumn` 注解：
   ```java
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface ExcelColumn {
       String name();              // 列标题
       int order();                // 列位置
       boolean required() default false;
   }
   ```
2. `ExcelImportResult`：
   ```java
   public class ExcelImportResult {
       int successCount;
       int failCount;
       List<ImportError> errors;   // [{row, field, message}]
   }
   ```
3. `ExcelHelper`（基于 Hutool POI，项目已有此依赖）：
   - `readRows(InputStream, Class<T>)` — 解析 Excel 到 DTO 列表
   - `generateTemplate(Class<T>)` — 根据 `@ExcelColumn` 生成空模板
   - 单次最大 5000 行限制（防止内存溢出）

**用户导入具体实现**：

1. `UserImportDTO` 标注 `@ExcelColumn`：username、nickname、email、phone、deptName（按名称匹配部门 ID）、roleName（按名称匹配角色 ID，多个用逗号分隔）
2. `UserController` 新增接口：
   - `GET /api/users/import-template`（权限：`sys:user:import`）
   - `POST /api/users/import`（权限：`sys:user:import`）
3. `UserServiceImpl.importUsers()`：
   - 逐行校验必填字段
   - 检查用户名唯一性
   - 部门名称 → 部门 ID 映射
   - 角色名称 → 角色 ID 映射
   - 默认密码：从 `sys_config` 读取 `user.default.password`（默认 `Abc@1234`）
   - 逐条事务：失败行跳过，不影响其他行（非整体回滚）

**前端改动**：

新增 `components/ImportDialog.vue`（可复用）：
- Props：`templateUrl`、`importUrl`、`title`
- UI：下载模板链接、拖拽上传区域、导入结果错误明细表格
- 可复用：其他模块导入只需传不同 URL 即可

---

### P2-3：操作日志变更对比

**问题**：当前操作日志只记录了"谁在什么时间做了什么操作"，但没有记录"数据从什么变成了什么"，排查问题时无法追溯具体变更内容。

**数据库变更**：

```sql
ALTER TABLE sys_operation_log
  ADD COLUMN before_data JSON NULL COMMENT '变更前数据',
  ADD COLUMN after_data JSON NULL COMMENT '变更后数据';
```

**@Log 注解扩展**：

```java
@Log(module = "用户管理", operation = "更新用户",
     entityClass = SysUser.class,   // 关联实体类
     idParam = "id")                // 从 URL 路径参数取实体 ID
```

**LogAspect 改动**（仅对 PUT/DELETE 操作）：

```
PUT /api/users/5 触发流程：
1. 前置：从路径提取 id=5，查询 SysUser(id=5)，序列化为 JSON → beforeData
2. 执行目标方法（更新数据库）
3. 后置：重新查询 SysUser(id=5)，序列化为 JSON → afterData
4. 将 beforeData 和 afterData 写入 sys_operation_log
```

**安全措施**：通过 `@JsonIgnore` 或自定义排除列表排除敏感字段（如 password）。GET 请求不捕获数据快照。

**前端改动**：

在操作日志详情对话框中新增 `ChangeCompare.vue` 组件：
- 解析 `before_data` 和 `after_data` JSON
- 仅展示有差异的字段（字段名/变更前/变更后）
- 字典值自动翻译（如 status: 1 → "正常"），复用 P0-3 的 `useDictStore`
- 显示未变更字段数量（默认隐藏）

---

## P3 - 性能 + 质量

### P3-1：慢查询优化

**新增索引**：

```sql
ALTER TABLE sys_operation_log ADD INDEX idx_created_time (created_time);
ALTER TABLE sys_operation_log ADD INDEX idx_user_id_created (user_id, created_time);
ALTER TABLE sys_login_log ADD INDEX idx_login_time (login_time);
ALTER TABLE sys_login_log ADD INDEX idx_username_login_time (username, login_time);
ALTER TABLE sys_job_log ADD INDEX idx_job_id_created (job_id, created_time);
ALTER TABLE sys_dict_data ADD INDEX idx_dict_type_sort (dict_type, sort_order);
ALTER TABLE sys_user ADD INDEX idx_dept_id (dept_id);
ALTER TABLE sys_user_notice ADD INDEX idx_user_read (user_id, is_read);
```

**查询优化**：

| 场景 | 问题 | 解决方案 |
|------|------|---------|
| 用户列表含部门名称 | 可能存在 N+1 查询 | Mapper XML 新增联表查询，一次查出用户+部门名 |
| 用户列表含角色列表 | 每个用户单独查角色 | 批量 IN 查询角色，Java 侧组装 |
| 日志深分页 | 大表 OFFSET 性能差 | 改用游标分页：`WHERE id < #{lastId} ORDER BY id DESC LIMIT #{size}` |
| 菜单树 | 全量扫描 + 内存递归 | 无需改动（数据量小，内存构建合理） |

**慢查询监控**：

新增 `SlowQueryInterceptor`（MyBatis 拦截器）：
- 记录执行时间超过阈值（默认 500ms，可通过 `sys_config` 配置）的 SQL
- 输出到 `logs/slow-query.log`：SQL + 参数 + 耗时 + 调用位置
- 非阻断：仅记录，不中断请求

---

### P3-2：仪表盘图表数据接口

**后端新增接口**（`DashboardController` 扩展）：

| 接口 | 返回格式 | SQL 逻辑 |
|------|---------|---------|
| `GET /api/dashboard/login-trend?days=7` | `[{date, count}]` | `sys_login_log` 按 DATE(login_time) 分组，仅统计 status=1 |
| `GET /api/dashboard/role-distribution` | `[{roleName, count}]` | `sys_user_role` JOIN `sys_role` 按 role_id 分组 |
| `GET /api/dashboard/dept-distribution` | `[{deptName, count}]` | `sys_user` 按 dept_id 分组 JOIN `sys_department` |
| `GET /api/dashboard/online-count` | `{count}` | Redis KEYS `online_user:*` 计数 |

**实现要点**：
- `getLoginTrend(days)` — 无登录记录的日期补零处理
- `getRoleDistribution()` — 排除已删除的角色和用户
- `getDeptDistribution()` — 排除已删除部门，仅聚合一级部门
- 所有接口使用 Redis 缓存，TTL=300 秒（key: `dashboard:{接口名}`）

---

### P3-3：测试覆盖率提升至 80%

**现状**：
- 后端：41 个测试文件。缺失：Dept、Dict、File、Job、Notice、OnlineUser、Dashboard 的 Controller 和 Service 测试
- 前端：18 个测试文件。缺失：P0-P2 新增的 store、composable、component 测试
- E2E：无

**后端测试计划**：

P0-P2 新增代码测试（随开发同步编写）：
- `CacheServiceTest`、`PasswordPolicyServiceTest`、`IdempotentAspectTest`
- `StorageStrategyTest`（LocalStorageStrategy）、`ExcelHelperTest`、`SlowQueryInterceptorTest`
- `DynamicTaskManagerTest`、`JobRunnerTest`

存量缺失测试补齐：
- `DeptControllerTest` + `DeptServiceImplTest`
- `DictControllerTest` + `DictServiceImplTest`
- `FileControllerTest` + `FileServiceImplTest`
- `JobControllerTest` + `JobServiceImplTest`
- `NoticeControllerTest` + `NoticeServiceImplTest`
- `OnlineUserControllerTest` + `OnlineUserServiceImplTest`
- `DashboardServiceImplTest`

**前端测试计划**：

- Store 测试：`dict.test.ts`、`notice.test.ts`、`dashboard.test.ts`
- Composable 测试：`useDict.test.ts`、`useIdempotent.test.ts`、`useWebSocket.test.ts`、`useTableSettings.test.ts`
- Component 测试：`ImportDialog.test.ts`、`TableToolbar.test.ts`、`DashboardGrid.test.ts`

**E2E 测试**（Playwright）：

| 测试场景 | 覆盖范围 |
|---------|---------|
| 登录流程 | 正常登录 → 看到仪表盘 → 退出 |
| 用户管理 CRUD | 列表 → 新增 → 编辑 → 状态切换 → 删除 |
| 角色权限分配 | 创建角色 → 分配菜单 → 验证权限生效 |
| 数据导入导出 | 下载模板 → 上传 → 查看结果 |
| 仪表盘布局 | 编辑模式 → 拖拽 → 删除 → 添加 → 保存 → 刷新后保持 |

**覆盖率强制执行**：
- 后端：新增 JaCoCo Maven 插件，行覆盖率 >= 80%
- 前端：`vitest --coverage`，分支覆盖率 >= 80%
- 两者均在 CI 流水线中强制检查

---

## 风险评估

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| Redis 不可用 | 缓存未命中，降级查 DB（性能下降，不会宕机） | 缓存读取包裹 try-catch，降级到 DB |
| WebSocket 连接断开 | 无法实时推送通知 | 自动重连（指数退避）；页面加载时同步未读数 |
| OSS 配置错误 | 文件上传失败 | 启动时验证 OSS 连接；保留本地策略作为降级方案 |
| 定时任务反射滥用 | RCE 远程代码执行漏洞 | `@ScheduledTarget` 白名单注解；仅允许调用标注的方法 |
| 大量 Excel 导入导致 OOM | 内存耗尽 | 5000 行限制；大文件使用 Hutool SAX 流式读取 |
| 密码策略导致用户锁定 | 用户无法登录 | 管理员可绕过策略重置密码；策略变更后首次登录给予宽限期 |

---

## 不在范围内（明确排除）

- 移动端响应式重构（独立计划）
- 多租户架构
- 微服务拆分
- SSO / OAuth2 集成
- Kubernetes 部署
- 实时协同编辑
