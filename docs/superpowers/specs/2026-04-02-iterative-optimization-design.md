# Admin Management System - Iterative Optimization Design

> Date: 2026-04-02
> Strategy: Balanced iterative improvement (方案三：均衡推进)
> Goal: System is in production, continuous iterative evolution
> Total: 14 optimization items across 4 phases

---

## Phase Overview

| Phase | Theme | Items | Dependencies |
|-------|-------|-------|-------------|
| P0 | Core Infrastructure | 3 | None |
| P1 | Security + Notifications | 5 | P0 (cache infrastructure) |
| P2 | Storage + Import | 3 | P0 (cache), P1 (password policy for import default pwd) |
| P3 | Performance + Quality | 3 | P0-P2 (tests cover all new code) |

---

## P0 - Core Infrastructure

### P0-1: Redis Cache Strategy

**Problem**: Dictionary data, system config, and menu trees are high-read low-write data, currently hitting DB on every request.

**Pattern**: Cache Aside (read cache first, write DB then invalidate cache).

**Cache Schema**:

| Cache Target | Redis Key | TTL | Invalidation |
|-------------|-----------|-----|-------------|
| Dict data by type | `dict:data:{dictType}` | 24h | Dict data CRUD |
| All dict types | `dict:types` | 24h | Dict type CRUD |
| All system config | `config:all` | 24h | Config update |
| Single config | `config:{key}` | 24h | Config update |
| User menu tree | `menu:user:{userId}` | 2h | Menu/role change (clear affected users) |
| User permissions | `perm:user:{userId}` | 2h | Menu/role change (clear affected users) |

**Backend Changes**:

1. `CacheConstants` - Add key prefix constants for all cache items above
2. New `CacheService` utility class:
   - `get(key, Class<T>)` / `set(key, value, ttl)` / `delete(key)` / `deleteByPrefix(prefix)`
   - Wraps `RedisTemplate` with JSON serialization
3. `DictServiceImpl` - Add cache read in query methods, cache delete in write methods
4. `ConfigServiceImpl` - Same pattern
5. `MenuServiceImpl` - Cache user menu tree; on menu/role change, query affected user IDs via role association and batch clear

**Key Decision**: Manual cache control (not Spring `@Cacheable`) for precise granularity, especially for menu/role cascade invalidation.

---

### P0-2: Scheduled Task Engine Integration

**Problem**: `SysJob` has complete CRUD but no actual scheduling execution. Cron expressions are stored in DB but never consumed by a scheduler.

**Architecture**:

```
SysJob (DB) <-> JobService <-> DynamicTaskManager <-> TaskScheduler
                                      |
                              ScheduledFuture Map (in-memory)
                                      |
                              Reflective invocation of invokeTarget
```

**New Components**:

1. `DynamicTaskManager`:
   - `Map<Long, ScheduledFuture<?>>` - tracks running tasks
   - `addTask(job)` - parse cron, register with `ThreadPoolTaskScheduler`
   - `removeTask(jobId)` - cancel `ScheduledFuture`
   - `updateTask(job)` - remove + add
2. `JobRunner`:
   - Reflective invocation of `invokeTarget` (format: `beanName.methodName`)
   - Writes `SysJobLog` before/after execution (status, duration, error)
   - Exception isolation: caught and logged, never breaks scheduler
3. `JobInitializer` (`ApplicationRunner`):
   - On startup, load all `status=1` jobs and register with scheduler
4. `@ScheduledTarget` annotation:
   - Security: only Spring Bean methods annotated with this can be invoked
   - Prevents arbitrary class reflection (RCE prevention)

**JobService Changes**:
- `changeStatus()` - enable calls `addTask`, disable calls `removeTask`
- `create()` - if status=1, also register
- `update()` - calls `updateTask`
- `delete()` - calls `removeTask`
- `run()` - immediate one-off execution via `JobRunner` directly

---

### P0-3: Frontend Dictionary Cache

**Problem**: Frontend pages repeatedly request dict API (gender, status, etc.) on every page load.

**New Files**:

1. `stores/modules/dict.ts` (Pinia store):
   - State: `dictMap: Record<string, DictData[]>`
   - `getDictData(dictType)` - async fetch with in-memory cache
   - `getDictLabel(dictType, value)` - resolve value to label (for table display)
   - `refreshDict(dictType)` - force refresh specific dict
   - `clearAll()` - called on logout
2. `composables/useDict.ts`:
   - `useDict(dictType)` - returns reactive dict data list
   - `useDictLabel(dictType, value)` - returns reactive label text

**Usage Pattern**:
```vue
<!-- Select dropdown -->
<el-option v-for="item in useDict('sys_gender')" :label="item.dictLabel" :value="item.dictValue" />

<!-- Table column -->
<template #default="{ row }">{{ getDictLabel('sys_status', row.status) }}</template>
```

---

## P1 - Security + Notifications

### P1-1: Password Security Policy

**Problem**: Password validation is only 6-20 char length. No strength rules, expiration, or history check.

**Configurable Rules** (via `sys_config`):

| Config Key | Default | Description |
|-----------|---------|-------------|
| `pwd.min.length` | 8 | Minimum password length |
| `pwd.require.uppercase` | true | At least 1 uppercase letter |
| `pwd.require.lowercase` | true | At least 1 lowercase letter |
| `pwd.require.digit` | true | At least 1 digit |
| `pwd.require.special` | false | At least 1 special character |
| `pwd.expire.days` | 90 | Password validity period (0=never) |
| `pwd.history.count` | 3 | Cannot reuse last N passwords |

**Database Changes**:

```sql
-- New field on sys_user
ALTER TABLE sys_user ADD COLUMN password_changed_time DATETIME NULL COMMENT 'Last password change time';

-- New table
CREATE TABLE sys_password_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  password VARCHAR(100) NOT NULL,
  created_time DATETIME NOT NULL,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Backend Changes**:

1. New `PasswordPolicyService`:
   - `validate(rawPassword)` - read rules from config, return error list
   - `checkHistory(userId, rawPassword)` - BCrypt match against history
   - `recordHistory(userId, encodedPassword)` - save to history table
   - `isExpired(user)` - check `password_changed_time` against `pwd.expire.days`
2. `AuthServiceImpl.login()` - after successful auth, check expiration, set `passwordExpired: true` in response
3. `UserServiceImpl.create/update()` + `ProfileController.updatePassword()` - call validate + checkHistory
4. `LoginResponse` - add `passwordExpired` boolean field

**Frontend Changes**:
- Login flow: if `passwordExpired`, show forced password change dialog
- Password input: real-time strength indicator (weak/medium/strong color bar)

---

### P1-2: API Idempotency

**Problem**: No duplicate submission prevention. Network jitter or double-click can create duplicate records.

**Flow**:

```
1. Frontend enters form -> GET /api/idempotent/token -> Backend generates UUID, stores in Redis (TTL=10min) -> returns token
2. Frontend submits form -> Header: X-Idempotent-Token
3. Backend interceptor -> Redis DELETE token (atomic) -> success=first submit, fail=duplicate
```

**Backend Changes**:

1. New `@Idempotent` annotation:
   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Idempotent {
       int expireSeconds() default 600;
   }
   ```
2. New `IdempotentController`:
   - `GET /api/idempotent/token` - generate and return token
3. New `IdempotentAspect`:
   - Intercepts `@Idempotent` methods
   - Extract `X-Idempotent-Token` from header
   - Atomic Redis DELETE to determine first vs duplicate
   - Duplicate returns `ResultCode.DUPLICATE_SUBMIT (40006)`
4. Apply `@Idempotent` to all POST create endpoints

**Frontend Changes**:

1. New `composables/useIdempotent.ts`:
   - `fetchToken()` - get token on form open
   - Auto-inject into axios header
2. `api/request.ts` - interceptor injects token if present
3. Form dialogs call `fetchToken()` on open

---

### P1-3: WebSocket Real-time Notifications

**Problem**: Notice system is CRUD-only. Users must refresh to see new notifications.

**Architecture**:

```
Backend publishes notice -> NoticeService.publish() -> WebSocketMessageBroker
  -> /topic/notice/broadcast (announcements)
  -> /queue/notice/{userId} (targeted notifications)

Frontend -> STOMP Client subscribes -> receives message -> notice store updates -> header bell badge
```

**Backend Changes**:

1. `pom.xml` - add `spring-boot-starter-websocket`
2. New `WebSocketConfig`:
   - STOMP endpoint: `/ws` (with SockJS fallback)
   - Message broker: `/topic` (broadcast), `/queue` (point-to-point)
3. New `WebSocketAuthInterceptor` (`ChannelInterceptor`):
   - On CONNECT frame: validate JWT from connection params
   - Inject user info into STOMP session
4. `NoticeServiceImpl.publish()`:
   - Announcement type -> broadcast to `/topic/notice/broadcast`
   - Notification type -> send to each target `/queue/notice/{userId}`
5. `SecurityConfig` - add `/ws/**` to public endpoints (auth handled at STOMP layer)

**Frontend Changes**:

1. New dependency: `@stomp/stompjs` + `sockjs-client`
2. New `composables/useWebSocket.ts`:
   - `connect()` - establish connection with token after login
   - `subscribe(destination, callback)` - subscribe to channels
   - `disconnect()` - disconnect on logout
   - Auto-reconnect with exponential backoff (max 30s)
3. New `stores/modules/notice.ts`:
   - State: `unreadCount`, `latestNotices`
   - `fetchUnreadCount()` - initial load
   - `onNewNotice(notice)` - WebSocket callback, update count + show ElNotification
   - `markAsRead(noticeId)` - mark as read
4. `Layout/Header.vue` - bell icon with Badge (unread count), click to expand recent notices panel

---

### P1-4: Table Enhancement (Column Settings / Density)

**Problem**: All data tables lack personalization. Every user sees the same columns and density.

**New Components**:

1. `components/TableToolbar.vue`:
   - Density toggle: large / default / small (maps to el-table `size`)
   - Column visibility: checkbox per column
   - Column ordering: drag to reorder (reuses existing `sortablejs`)
   - Refresh button
   - Reset to defaults
2. New `composables/useTableSettings.ts`:
   - `useTableSettings(tableId)` - returns `{ density, columns, resetColumns }`
   - Persists to LocalStorage keyed by route path

**Integration**: Each list page adds `<TableToolbar>` above the table. Low invasiveness.

---

### P1-5: Dashboard Editable Layout

**Problem**: Dashboard has fixed layout. Users cannot customize widget arrangement.

**Three Operations**:

| Operation | Trigger | Behavior |
|-----------|---------|----------|
| Drag to reorder | Edit mode drag handle | vuedraggable reposition |
| Remove widget | Edit mode X button | Remove from layout, add to available pool |
| Add widget | "+ Add Widget" button | Show panel of available widgets, click to add |

**Widget Registry** (`dashboard/widgets/registry.ts`):

| Widget ID | Name | Type |
|-----------|------|------|
| `stat-users` | User count | stat |
| `stat-roles` | Role count | stat |
| `stat-menus` | Menu count | stat |
| `stat-today-login` | Today logins | stat |
| `stat-online` | Online users | stat |
| `chart-login-trend` | Login trend (7d) | chart |
| `chart-role-dist` | Role distribution | chart |
| `chart-dept-dist` | Dept distribution | chart |
| `table-recent-ops` | Recent operations | table |
| `table-recent-login` | Recent logins | table |
| `table-recent-notice` | Recent notices | table |
| `shortcut-links` | Quick links | shortcut |

**Store** (`stores/modules/dashboard.ts`):

```typescript
interface DashboardState {
  activeWidgets: string[]   // ordered widget IDs in current layout
  editMode: boolean
}

// Actions
addWidget(widgetId)
removeWidget(widgetId)
reorderWidgets(newOrder)
resetLayout()
toggleEditMode()

// Getters
availableWidgets  // in registry but not in activeWidgets
activeWidgetMetas // activeWidgets mapped to full WidgetMeta
```

**Component Structure**:

```
views/dashboard/
├── index.vue
├── components/
│   ├── DashboardGrid.vue        # vuedraggable container
│   ├── WidgetWrapper.vue        # card shell (title, drag handle, delete button)
│   ├── AddWidgetDialog.vue      # grid of available widgets
│   └── widgets/
│       ├── registry.ts
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

**Dynamic Rendering**: `DashboardGrid` uses `defineAsyncComponent` + registry map. Adding new widgets only requires registry entry + component file.

**Persistence**: Layout saved to LocalStorage (`dashboard-layout` key).

---

## P2 - Storage + Import

### P2-1: File Storage Abstraction (Local / OSS)

**Problem**: File upload only supports local disk. Production risks: single-machine capacity, file loss on migration, incompatible with multi-instance deployment.

**Pattern**: Strategy pattern for storage layer.

```
FileController -> FileService -> StorageStrategy (interface)
                                   ├── LocalStorageStrategy   (dev)
                                   └── AliyunOssStrategy      (prod)
```

**Interface**:

```java
public interface StorageStrategy {
    FileUploadResult upload(InputStream input, String fileName, String contentType);
    InputStream download(String storedPath);
    void delete(String storedPath);
    String getAccessUrl(String storedPath);
}
```

**Configuration-driven**:

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

**New Classes**:
1. `StorageProperties` - reads config
2. `StorageAutoConfiguration` - registers bean based on `storage.type`
3. `LocalStorageStrategy` - extracted from current `FileServiceImpl`
4. `AliyunOssStrategy` - uses `aliyun-oss-sdk`

**Database Change**: `sys_file` add `storage_type VARCHAR(20)` - records where each file is stored, enabling mixed storage coexistence.

**Migration**: Existing files default to `storage_type=local`. Download resolves strategy by this field.

---

### P2-2: Data Import (Excel)

**Problem**: Only export exists. Admins need bulk import for users, dict data, etc.

**Import Flow**:

```
1. Download template  GET  /api/users/import-template  -> empty Excel with headers
2. Fill and upload    POST /api/users/import            -> upload filled Excel
3. Backend parse      -> validate row by row -> collect errors
4. Return result      -> { successCount, failCount, errors: [{row, field, message}] }
5. Frontend display   -> success count + error detail table
```

**Generic Framework**:

1. `@ExcelColumn` annotation:
   ```java
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface ExcelColumn {
       String name();              // column header
       int order();                // column position
       boolean required() default false;
   }
   ```
2. `ExcelImportResult`:
   ```java
   public class ExcelImportResult {
       int successCount;
       int failCount;
       List<ImportError> errors;   // [{row, field, message}]
   }
   ```
3. `ExcelHelper` (based on Hutool POI, already a dependency):
   - `readRows(InputStream, Class<T>)` - parse Excel to DTO list
   - `generateTemplate(Class<T>)` - generate empty template from `@ExcelColumn`
   - Max 5000 rows per import (memory protection)

**User Import Specifics**:

1. `UserImportDTO` with `@ExcelColumn` fields: username, nickname, email, phone, deptName (matched to dept ID), roleName (matched to role ID, comma-separated)
2. `UserController` new endpoints:
   - `GET /api/users/import-template` (permission: `sys:user:import`)
   - `POST /api/users/import` (permission: `sys:user:import`)
3. `UserServiceImpl.importUsers()`:
   - Validate required fields per row
   - Check username uniqueness
   - Map dept/role names to IDs
   - Default password from `sys_config` key `user.default.password` (default: `Abc@1234`)
   - Per-row transaction: failures skip, don't rollback successful rows

**Frontend**:

New `components/ImportDialog.vue` (reusable):
- Props: `templateUrl`, `importUrl`, `title`
- UI: download template link, drag-and-drop upload area, result table showing errors
- Reusable for future entity imports (just pass different URLs)

---

### P2-3: Operation Log Change Comparison

**Problem**: Operation logs record who-did-what-when, but not what-changed-from-to. Cannot trace specific data changes for troubleshooting.

**Database Change**:

```sql
ALTER TABLE sys_operation_log
  ADD COLUMN before_data JSON NULL COMMENT 'Data before change',
  ADD COLUMN after_data JSON NULL COMMENT 'Data after change';
```

**@Log Annotation Extension**:

```java
@Log(module = "User Management", operation = "Update User",
     entityClass = SysUser.class,   // associated entity
     idParam = "id")                // path variable for entity ID lookup
```

**LogAspect Change** (for PUT/DELETE only):

```
PUT /api/users/5 triggers:
1. Before: extract id=5 from path, query SysUser(5), serialize to JSON -> beforeData
2. Execute target method (DB update)
3. After: re-query SysUser(5), serialize to JSON -> afterData
4. Save both to sys_operation_log
```

**Security**: Exclude sensitive fields (password, etc.) via `@JsonIgnore` or custom exclusion list. GET requests do not capture snapshots.

**Frontend**:

New `ChangeCompare.vue` component in log detail dialog:
- Parse `before_data` and `after_data` JSON
- Display only changed fields in a diff table (field / before / after)
- Dict values auto-translated using `useDictStore` from P0-3
- Show count of unchanged fields (hidden by default)

---

## P3 - Performance + Quality

### P3-1: Slow Query Optimization

**New Indexes**:

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

**Query Optimizations**:

| Scenario | Problem | Solution |
|----------|---------|----------|
| User list with dept name | Potential N+1 | New mapper XML join query, fetch user + dept in one query |
| User list with roles | Per-user role query | Batch IN query for roles, assemble in Java |
| Log deep pagination | Slow OFFSET on large tables | Cursor-based pagination: `WHERE id < #{lastId} ORDER BY id DESC LIMIT #{size}` |
| Menu tree | Full scan + memory recursion | No change needed (small dataset, memory build is appropriate) |

**Slow Query Monitor**:

New `SlowQueryInterceptor` (MyBatis Interceptor):
- Logs SQL exceeding threshold (default 500ms, configurable via `sys_config`)
- Output to `logs/slow-query.log`: SQL + params + duration + call location
- Non-blocking: log only, never interrupts request

---

### P3-2: Dashboard Chart Data APIs

**New Backend Endpoints** (in `DashboardController`):

| Endpoint | Response | SQL Logic |
|----------|----------|-----------|
| `GET /api/dashboard/login-trend?days=7` | `[{date, count}]` | `sys_login_log` GROUP BY DATE(login_time), status=1 only |
| `GET /api/dashboard/role-distribution` | `[{roleName, count}]` | `sys_user_role` JOIN `sys_role` GROUP BY role_id |
| `GET /api/dashboard/dept-distribution` | `[{deptName, count}]` | `sys_user` GROUP BY dept_id JOIN `sys_department` |
| `GET /api/dashboard/online-count` | `{count}` | Redis KEYS `online_user:*` count |

**Implementation Notes**:
- `getLoginTrend(days)` - zero-fill dates with no login records
- `getRoleDistribution()` - exclude deleted roles and users
- `getDeptDistribution()` - exclude deleted depts, aggregate at top-level only
- Cache all with Redis TTL=300s (key: `dashboard:{endpoint}`)

---

### P3-3: Test Coverage to 80%

**Current State**:
- Backend: 41 test files. Missing: Dept, Dict, File, Job, Notice, OnlineUser, Dashboard controllers and services
- Frontend: 18 test files. Missing: new stores, composables, components from P0-P2
- E2E: None

**Backend Test Plan**:

P0-P2 new code tests (written alongside development):
- `CacheServiceTest`, `PasswordPolicyServiceTest`, `IdempotentAspectTest`
- `StorageStrategyTest` (LocalStorageStrategy), `ExcelHelperTest`, `SlowQueryInterceptorTest`
- `DynamicTaskManagerTest`, `JobRunnerTest`

Legacy gap tests:
- `DeptControllerTest` + `DeptServiceImplTest`
- `DictControllerTest` + `DictServiceImplTest`
- `FileControllerTest` + `FileServiceImplTest`
- `JobControllerTest` + `JobServiceImplTest`
- `NoticeControllerTest` + `NoticeServiceImplTest`
- `OnlineUserControllerTest` + `OnlineUserServiceImplTest`
- `DashboardServiceImplTest`

**Frontend Test Plan**:

- Stores: `dict.test.ts`, `notice.test.ts`, `dashboard.test.ts`
- Composables: `useDict.test.ts`, `useIdempotent.test.ts`, `useWebSocket.test.ts`, `useTableSettings.test.ts`
- Components: `ImportDialog.test.ts`, `TableToolbar.test.ts`, `DashboardGrid.test.ts`

**E2E Tests** (Playwright):

| Scenario | Coverage |
|----------|----------|
| Login flow | Login -> see dashboard -> logout |
| User CRUD | List -> create -> edit -> status toggle -> delete |
| Role permission | Create role -> assign menus -> verify permission takes effect |
| Data import/export | Download template -> upload -> view results |
| Dashboard layout | Edit mode -> drag -> remove -> add -> save -> refresh persists |

**Coverage Enforcement**:
- Backend: Add JaCoCo Maven plugin, line coverage >= 80%
- Frontend: `vitest --coverage`, branch coverage >= 80%
- Both enforced in CI pipeline

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Redis unavailability | Cache miss, falls back to DB (degraded performance, not outage) | Cache reads wrapped in try-catch, fallback to DB |
| WebSocket connection loss | No real-time notifications | Auto-reconnect with exponential backoff; unread count synced on page load |
| OSS misconfiguration | File upload failure | Validate OSS connection on startup; keep local strategy as fallback |
| Job reflection abuse | RCE vulnerability | `@ScheduledTarget` whitelist annotation; only annotated methods callable |
| Large Excel import OOM | Memory exhaustion | 5000 row limit; streaming read (Hutool SAX mode for large files) |
| Password policy lock-out | Users unable to login | Admin can bypass policy to reset password; grace period on first login after policy change |

---

## Non-Goals (Explicitly Out of Scope)

- Mobile-responsive redesign (separate initiative)
- Multi-tenant architecture
- Microservice decomposition
- SSO / OAuth2 integration
- Kubernetes deployment
- Real-time collaborative editing
