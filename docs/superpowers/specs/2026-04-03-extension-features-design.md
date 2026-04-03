# 企业级后台管理系统 — 扩展功能全景设计

> **定位**：本系统作为真实产品基座，扩展设计围绕生产就绪和业务承载力展开。
> **策略**：分层推进（P0→P1→P2→P3），每层混合业务价值与技术加固，确保每阶段交付可用。
> **日期**：2026-04-03

---

## 目录

- [全景总览](#全景总览)
- [P0：生产必备](#p0生产必备)
- [P1：核心业务](#p1核心业务)
- [P2：规模化](#p2规模化)
- [P3：竞争力](#p3竞争力)
- [依赖关系图](#依赖关系图)
- [数据库变更汇总](#数据库变更汇总)
- [技术选型汇总](#技术选型汇总)

---

## 全景总览

| 层级 | 定位 | 功能数 | 关键词 |
|------|------|--------|--------|
| **P0** | 生产必备 — 不补就不能上线 | 6 | 2FA、数据脱敏、审计增强、并发登录、通用导入导出、防重完善 |
| **P1** | 核心业务 — 高频场景必需 | 6 | 工作流、消息中心、数据报表、行级数据权限、维护模式、回收站 |
| **P2** | 规模化 — 用户量/租户增长 | 6 | 多租户、国际化、消息队列、全文搜索、监控告警、API 版本化 |
| **P3** | 竞争力 — 差异化体验 | 6 | 表单设计器、数据大屏、移动端、白标能力、插件化、AI 辅助 |

共 **24 个扩展功能点**，覆盖安全合规、业务承载、规模化运营、差异化竞争四个维度。

---

## P0：生产必备

### P0.1 双因素认证（2FA）

**现状**：仅用户名+密码单因素认证。

**设计**：
- TOTP 动态验证码（兼容 Google Authenticator / 企业微信）
- 首次登录强制绑定，管理员可为用户重置 2FA
- 可按角色配置是否强制开启 2FA
- 备用恢复码（一次性，10 组）

**数据库变更**：
```sql
CREATE TABLE sys_user_2fa (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    secret_key  VARCHAR(64) NOT NULL,
    enabled     TINYINT DEFAULT 0,
    backup_codes TEXT,              -- JSON 数组，一次性恢复码
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**前端变更**：
- 登录流程增加验证码输入步骤（条件渲染）
- 个人中心增加 2FA 绑定/解绑页面（含二维码展示）

**后端变更**：
- 新增 `TotpService`：生成密钥、生成二维码 URI、验证码校验
- `AuthController` 登录接口增加 `totpCode` 可选参数
- `SecurityConfig` 增加 2FA 验证过滤器

**依赖**：无外部依赖，使用 `com.warrenstrange:googleauth` 库

---

### P0.2 数据脱敏

**现状**：API 返回完整的手机号、邮箱等敏感信息，无脱敏处理。

**设计**：
- 注解驱动：`@Sensitive(type = SensitiveType.PHONE)`
- 支持类型：手机号 `138****5678`、邮箱 `z***@example.com`、身份证 `320***********1234`、银行卡、地址
- 按角色控制：管理员可查看原文，普通用户看脱敏值
- 零侵入现有代码：Jackson 自定义序列化器

**实现**：
- 自定义注解 `@Sensitive`
- `SensitiveSerializer extends JsonSerializer<String>`：根据注解类型执行脱敏
- `SensitiveContextHolder`：从 SecurityContext 读取当前用户角色，决定是否跳过脱敏

**应用范围**：
- `SysUser` 实体：phone、email 字段
- 后续新增实体：按需标注

---

### P0.3 操作审计增强

**现状**：有操作日志但缺少数据变更对比，且可被删除。

**设计**：
- 数据变更快照：修改操作记录 before/after 的字段值（JSON diff）
- 敏感操作二次确认：删除用户、修改权限、导出数据时前端弹窗确认
- 审计日志不可删除：后端不暴露删除接口，数据库层面可设置独立存储策略
- 检索增强：按操作类型、目标对象 ID、目标对象类型、时间范围组合筛选

**数据库变更**：
```sql
ALTER TABLE sys_operation_log
    ADD COLUMN target_type   VARCHAR(64)  COMMENT '目标对象类型（如 user, role）',
    ADD COLUMN target_id     BIGINT       COMMENT '目标对象ID',
    ADD COLUMN before_data   JSON         COMMENT '变更前数据',
    ADD COLUMN after_data    JSON         COMMENT '变更后数据';
```

**后端变更**：
- 扩展 `@Log` 注解：增加 `targetType` 属性
- `LogAspect` 增强：拦截 PUT/DELETE 请求时，先查询原数据作为 before_data，执行后记录 after_data
- 移除审计日志的删除接口

---

### P0.4 并发登录控制

**现状**：同一账号可在多处同时登录，无限制。

**设计**：
- 系统配置项 `login.max-sessions`：最大同时会话数（默认 1）
- 冲突策略配置 `login.conflict-policy`：`kick_old`（踢旧）或 `reject_new`（拒新）
- 被踢出时 WebSocket 实时通知，前端弹窗提示"您的账号在其他地方登录"
- 与现有在线用户管理联动

**实现**：
- Redis 中 `online_user:{userId}` 改为 Sorted Set，score 为登录时间，member 为 token
- 登录时检查当前会话数，根据策略执行踢出或拒绝
- 踢出操作：将旧 token 加入黑名单 + WebSocket 推送下线通知

---

### P0.5 通用数据导入导出

**现状**：仅用户管理支持导出 Excel，无导入功能。

**设计**：
- **通用导出框架**：
  - 后端：`@Exportable` 注解标记 DTO 字段 → EasyExcel 动态生成
  - 前端：`useExport(apiUrl, params)` composable，一键调用
  - 支持 Excel（.xlsx）和 CSV 格式
  - 大数据量（>5000 行）异步导出：后台生成 → 文件管理中存储 → 通知用户下载

- **通用导入框架**：
  - 模板下载：根据 `@Exportable` 注解自动生成空模板
  - 上传 → 预校验（格式、必填、唯一性、外键关联）→ 展示校验结果 → 确认导入
  - 导入错误报告：精确到行号和字段名
  - 前端：`useImport(apiUrl)` composable，统一交互流程

**后端核心类**：
- `ExportService`：通用导出服务
- `ImportService`：通用导入服务（含校验管线）
- `ImportResult`：导入结果（成功数、失败数、错误详情列表）

**技术选型**：后端 EasyExcel（阿里，低内存占用），前端 SheetJS 预览

---

### P0.6 接口幂等性 & 防重提交完善

**现状**：已有幂等性 Token 机制，但仅覆盖 POST，前端缺少统一防重。

**设计**：
- PUT/DELETE 也纳入 `@Idempotent` 保护
- 幂等失败返回具体提示："该操作已提交，请勿重复操作"（HTTP 409 Conflict）
- 前端统一封装：
  - `useSubmit(apiFn)` composable：自动管理 loading 状态 + 节流 + 成功后重置
  - 所有表单提交按钮统一使用，替代各页面手动控制 loading

---

## P1：核心业务

### P1.1 工作流引擎

**现状**：无审批流程能力，每个审批场景需硬编码。

**设计**：
- 集成 Flowable 轻量引擎（Spring Boot Starter）
- 前端可视化流程设计器：BPMN.js 集成到管理端
- 内置流程模板：单级审批、多级会签、条件分支
- 审批操作：提交、同意、驳回、转办、加签、撤回
- 与通知系统联动：审批待办通过 WebSocket 实时推送
- 审批记录完整追溯：每个节点的操作人、时间、意见

**数据库变更**：
- Flowable 自建表（约 30 张，`ACT_` 前缀，引擎自动管理）
- 业务关联表：
```sql
CREATE TABLE biz_process_definition (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    process_key     VARCHAR(64) NOT NULL UNIQUE,
    description     VARCHAR(512),
    category        VARCHAR(64),
    flowable_def_id VARCHAR(64),
    status          TINYINT DEFAULT 1,
    created_by      VARCHAR(64),
    created_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**前端新增页面**：
- 流程定义管理（设计/部署/启停）
- 我的待办 / 我的已办 / 我发起的
- 审批详情页（流程图高亮 + 审批时间线）

**后端新增模块**：`modules/workflow/`
- `ProcessController`：流程定义 CRUD、部署
- `TaskController`：待办查询、审批操作
- `ProcessService`：封装 Flowable RuntimeService / TaskService

---

### P1.2 消息中心升级

**现状**：WebSocket 站内通知，渠道单一，不支持消息模板。

**设计**：
- 多渠道统一发送：站内信（已有）、邮件（SMTP）、短信（阿里云 SMS SDK）、企业微信 Webhook
- 消息模板引擎：变量占位符 `{{username}}`、`{{content}}`，管理端可编辑
- 发送策略配置：事件 → 渠道映射（如：审批通知 → 站内信+邮件）
- 发送记录 & 失败重试（最多 3 次，间隔递增）
- 用户偏好：用户可选择接收渠道

**数据库变更**：
```sql
CREATE TABLE sys_msg_template (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL UNIQUE,
    template_name VARCHAR(128) NOT NULL,
    content       TEXT NOT NULL,
    channels      VARCHAR(256),          -- JSON 数组: ["email","sms","wechat","internal"]
    status        TINYINT DEFAULT 1,
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_msg_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64),
    channel       VARCHAR(32) NOT NULL,
    receiver      VARCHAR(256) NOT NULL,
    content       TEXT NOT NULL,
    status        TINYINT DEFAULT 0,     -- 0=待发送 1=成功 2=失败
    retry_count   INT DEFAULT 0,
    error_msg     VARCHAR(512),
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**后端新增**：
- `MessageService`：统一发送入口
- `EmailSender` / `SmsSender` / `WechatSender`：渠道实现（策略模式）
- `MessageRetryJob`：失败重试定时任务

---

### P1.3 数据报表模块

**现状**：Dashboard 仅有统计卡片和简单图表，不支持自定义报表。

**设计**：
- 预置报表：
  - 用户增长趋势（日/周/月）
  - 登录热力图（按小时/天）
  - 操作频次 Top10（按模块/用户）
  - 部门人员分布
- 自定义报表：选择数据源 → 配置维度/指标 → 选择图表类型 → 保存 → 分享
- 报表导出：PDF / Excel / 图片（html2canvas）
- 定时报表：绑定 Cron 表达式，自动生成并推送邮箱（复用 P1.2 邮件通道和现有定时任务模块）

**前端新增页面**：
- 报表列表页
- 报表设计器（数据源选择 + 图表配置）
- 报表查看页（筛选条件 + 图表渲染）

**后端新增**：
- `ReportController`：报表 CRUD + 数据查询
- `ReportService`：聚合查询引擎（基于配置动态生成 SQL）
- `ReportExportService`：导出服务

**数据库变更**：
```sql
CREATE TABLE sys_report (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name   VARCHAR(128) NOT NULL,
    description   VARCHAR(512),
    config        JSON NOT NULL,         -- 报表配置（数据源、维度、指标、图表类型）
    is_public     TINYINT DEFAULT 0,
    created_by    VARCHAR(64),
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT DEFAULT 0
);
```

---

### P1.4 数据权限（行级控制）

**现状**：RBAC 控制到菜单和按钮级别，无法控制"只能看本部门数据"。

**设计**：
- 数据范围策略枚举：
  - `ALL` — 全部数据
  - `DEPT` — 本部门
  - `DEPT_AND_CHILDREN` — 本部门及下级
  - `SELF` — 仅本人
  - `CUSTOM` — 自定义部门列表
- 策略绑定到角色
- MyBatis 拦截器自动注入过滤条件，业务代码零侵入
- 覆盖范围：所有列表查询接口

**数据库变更**：
```sql
ALTER TABLE sys_role
    ADD COLUMN data_scope   TINYINT DEFAULT 1 COMMENT '数据范围：1=全部 2=本部门 3=本部门及下级 4=仅本人 5=自定义';

CREATE TABLE sys_role_dept (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id  BIGINT NOT NULL,
    dept_id  BIGINT NOT NULL,
    UNIQUE KEY uk_role_dept (role_id, dept_id)
);
```

**后端核心实现**：
- `@DataScope` 注解：标记需要数据权限过滤的 Mapper 方法
- `DataScopeInterceptor implements Interceptor`：MyBatis 拦截器，自动拼接 `AND dept_id IN (...)` 或 `AND created_by = ?`
- `DataScopeHelper`：根据当前用户角色的 `data_scope` 计算可访问的部门 ID 集合

---

### P1.5 系统公告 & 维护模式

**现状**：通知公告面向已登录用户，无法处理系统维护场景。

**设计**：
- 维护模式开关：管理端一键开启/关闭
- 维护模式生效时：非管理员访问返回 503 + 维护页面
- 登录页公告横幅：未登录状态也能展示（如"今晚 22:00-23:00 系统升级"）
- 定时维护窗口：配置起止时间，自动开启/关闭
- 强制全员下线：配合维护窗口，清除所有在线会话

**实现**：
- Redis key `system:maintenance` 存储维护状态和信息
- `MaintenanceFilter`（Spring Security 过滤链）：检查维护状态，非白名单角色返回 503
- 前端：新增 `/maintenance` 页面，路由守卫检查维护状态
- 系统配置（`sys_config`）新增：`system.maintenance.enabled`、`system.maintenance.message`、`system.maintenance.start_time`、`system.maintenance.end_time`

---

### P1.6 操作回收站

**现状**：逻辑删除已实现（`deleted` 字段），但前端无法查看和恢复已删除数据。

**设计**：
- 回收站页面：按模块分类展示已删除数据（用户、角色、部门、字典等）
- 支持单条恢复 / 批量恢复
- 自动清理：超过 N 天（可配置，默认 30 天）的已删除数据物理清除
- 清理任务绑定现有定时任务模块

**实现**：
- 通用查询：各模块 Service 增加 `listDeleted(query)` 方法（`WHERE deleted = 1`）
- 恢复接口：`PATCH /api/{module}/{id}/restore`（`UPDATE SET deleted = 0`）
- 清理任务：`RecycleBinCleanJob` 实现 `@ScheduledTarget`，按配置天数物理删除
- 前端：新增回收站页面 `views/system/recycle/index.vue`，Tab 切换不同模块

---

## P2：规模化

### P2.1 多租户架构

**现状**：单组织使用，无租户隔离。

**设计**：
- 隔离方案：共享数据库 + `tenant_id` 字段（成本最低，适合中小规模）
- 租户管理后台（平台级）：租户 CRUD、套餐绑定、功能开关、数据初始化
- MyBatis 拦截器自动注入 `tenant_id` 条件，业务代码无感知
- 租户级配置：独立的系统配置、主题、Logo
- 租户管理员与平台管理员分离

**数据库变更**：
```sql
CREATE TABLE sys_tenant (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_name   VARCHAR(128) NOT NULL,
    tenant_code   VARCHAR(64) NOT NULL UNIQUE,
    contact_name  VARCHAR(64),
    contact_phone VARCHAR(32),
    package_id    BIGINT,
    expire_time   DATETIME,
    status        TINYINT DEFAULT 1,
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT DEFAULT 0
);

CREATE TABLE sys_tenant_package (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name  VARCHAR(128) NOT NULL,
    menu_ids      TEXT,                  -- 可用菜单 ID 集合
    status        TINYINT DEFAULT 1,
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 业务表统一新增：
ALTER TABLE sys_user       ADD COLUMN tenant_id BIGINT DEFAULT 0;
ALTER TABLE sys_role       ADD COLUMN tenant_id BIGINT DEFAULT 0;
ALTER TABLE sys_department ADD COLUMN tenant_id BIGINT DEFAULT 0;
-- ... 其他业务表同理
```

**核心实现**：
- `TenantContextHolder`（ThreadLocal）：存储当前请求的 tenant_id
- `TenantInterceptor`：MyBatis 拦截器，自动追加 `tenant_id = ?`
- `TenantFilter`：从 JWT claims 或 Header 中解析 tenant_id 并设置到 Context
- 平台管理员可跨租户查看（特殊标记跳过拦截）

---

### P2.2 国际化（i18n）

**现状**：所有文案硬编码中文。

**设计**：
- **前端**：Vue I18n 集成
  - Locale 文件：`locales/zh-CN.json`、`locales/en-US.json`
  - 抽取策略：先框架层（布局、通用组件、错误页），再逐模块推进
  - 语言切换器：Header 右上角，选择后存储到 localStorage + 用户偏好
- **后端**：Spring MessageSource
  - `messages_zh_CN.properties`、`messages_en_US.properties`
  - 错误码对应的提示信息国际化
  - 根据 `Accept-Language` Header 返回对应语言
- **动态内容**：菜单名称、字典标签支持多语言
  - `sys_i18n_message` 表存储：`(locale, message_key, message_value)`
  - 前端启动时批量加载

**数据库变更**：
```sql
CREATE TABLE sys_i18n_message (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    locale        VARCHAR(16) NOT NULL,
    message_key   VARCHAR(256) NOT NULL,
    message_value TEXT NOT NULL,
    module        VARCHAR(64),
    UNIQUE KEY uk_locale_key (locale, message_key)
);
```

---

### P2.3 消息队列集成

**现状**：所有操作同步执行，耗时操作阻塞请求。

**设计**：
- 选型：RocketMQ（国内企业生态好，讯飞背景契合）
- 异步化场景：
  - 操作日志写入（当前同步 AOP → 消息异步写入）
  - 消息通知发送（邮件/短信不阻塞业务）
  - 大文件导出（生成完成后通知下载）
  - 工作流事件分发
- 死信队列 + 重试策略（最多 3 次，指数退避）
- 管理端队列监控面板（消息积压量、消费延迟）

**Docker 变更**：docker-compose 新增 RocketMQ NameServer + Broker 容器

**后端新增**：
- `mq/` 包：Producer / Consumer 基础封装
- `MqConstants`：Topic 和 Tag 常量定义
- 各场景 Consumer：`LogConsumer`、`NoticeConsumer`、`ExportConsumer`

---

### P2.4 全文搜索

**现状**：全局搜索基于前端内存匹配，列表搜索 SQL LIKE 性能差。

**设计**：
- 集成 Elasticsearch 8.x
- 索引场景：操作日志、通知内容、系统全局搜索
- 数据同步：数据库变更通过消息队列（P2.3）同步到 ES
- 搜索增强：高亮、模糊匹配、拼音搜索（ik 分词 + pinyin 插件）、搜索建议
- 降级策略：ES 不可用时自动回退到数据库 LIKE 查询

**Docker 变更**：docker-compose 新增 Elasticsearch + Kibana 容器

**后端新增**：
- `search/` 包：`SearchService` 统一搜索接口
- `EsIndexService`：索引管理（创建/更新 mapping）
- `EsSyncConsumer`：消费 MQ 消息同步数据到 ES

---

### P2.5 监控告警体系

**现状**：系统监控仅展示实时快照，无历史趋势、无告警。

**设计**：
- 指标采集：Spring Boot Actuator + Micrometer → Prometheus
- 可视化：Grafana Dashboard
  - JVM 内存/GC、接口 QPS、响应时间 P50/P95/P99、错误率
  - 数据库连接池状态、Redis 命中率
- 告警规则：
  - CPU > 80% 持续 5 分钟
  - 堆内存 > 90%
  - 接口 P99 > 3s
  - 错误率 > 5%
- 告警通道：与 P1.2 消息中心联动
- 应用健康看板：集成到现有系统监控页面，iframe 嵌入 Grafana 面板

**Docker 变更**：docker-compose 新增 Prometheus + Grafana 容器

**后端变更**：
- `application.yml` 增加 Actuator + Micrometer 配置
- 自定义业务指标：在线用户数、登录失败率等（`MeterRegistry` 注册）

---

### P2.6 API 版本化 & 限流分级

**现状**：API 无版本概念，限流策略统一。

**设计**：
- URL 版本化：`/api/v1/users` → `/api/v2/users`
- 当前所有接口归为 v1，新功能迭代时引入 v2
- 版本兼容：v1 Controller 通过适配器委托到 v2 实现，设置废弃时间线
- 限流分级：
  - 按角色：管理员 100 req/s，普通用户 20 req/s
  - 按接口类型：查询 50 req/s，写入 10 req/s
  - 按租户（配合 P2.1）：不同套餐不同配额
- 扩展 `@RateLimiter` 注解：增加 `role`、`scope` 属性

---

## P3：竞争力

### P3.1 低代码表单设计器

**设计**：
- 拖拽式设计器：左侧组件面板 → 中间画布 → 右侧属性配置
- 组件库：输入框、下拉选择、日期选择器、文件上传、级联选择、富文本、表格子表单
- 表单规则引擎：字段联动、动态校验、必填/只读条件
- 表单绑定数据源：关联数据库表或 API 接口
- 表单版本管理：草稿 → 发布，历史版本回溯
- 与工作流联动（P1.1）：审批节点绑定不同表单视图

**技术方案**：
- 前端：VueDraggable + JSON Schema 描述表单结构
- 后端：通用表单存储引擎（`sys_form_definition` + `sys_form_data`）
- 渲染器：根据 JSON Schema 动态渲染 Element Plus 组件

---

### P3.2 数据可视化大屏

**设计**：
- 大屏设计器：自由拖拽定位，组件可缩放
- 组件库：数字翻牌器、省/市级地图、轮播表格、进度环、实时时钟、3D 柱状图
- 数据接入：静态数据 / API 轮询 / WebSocket 实时推送
- 自适应分辨率：1920x1080 基准，`scale` 等比缩放
- 预置模板：运营监控、业务概览、项目看板
- 独立访问链接：`/screen/:id`，全屏无导航，可投屏

**技术方案**：前端 ECharts 5 + CSS Grid 绝对定位 + ResizeObserver 自适应

---

### P3.3 移动端适配

**设计**：
- 独立 H5 应用，共享后端 API
- 核心场景：审批处理、消息通知、数据看板、个人中心
- 企业微信/钉钉内嵌：JS-SDK 免登
- 推送通知：移动端 Push 通道

**技术方案**：
- 新增 `admin-mobile/` 项目
- Vue 3 + Vite + Vant 4（移动端组件库）
- 与主项目共享 `types/` 和 API 层定义

---

### P3.4 系统主题 & 白标能力

**设计**：
- 品牌配置化：系统名称、Logo、Favicon、登录页背景，管理端可配
- 租户级白标（配合 P2.1）：每个租户独立品牌
- CSS 变量体系完善：间距、圆角、字体 Design Token 全部可配
- 登录页模板：3-5 套布局模板可选
- 自定义 CSS 注入：高级用户直接写 CSS 覆盖

**实现**：`sys_config` 存储品牌配置，前端启动时加载并通过 CSS Variables 动态应用

---

### P3.5 插件化架构

**设计**：
- 插件规范：每个插件独立目录，含前端路由/组件 + 后端 Controller/Service + SQL 迁移
- 生命周期：安装 → 启用 → 禁用 → 卸载
- 插件管理界面：已安装列表、版本、状态切换
- 插件间通信：事件总线（`ApplicationEvent`），插件可监听核心事件
- 示例：将工作流、报表模块封装为标准插件

**技术方案**：
- 后端：PF4J（Plugin Framework for Java）+ Spring 集成
- 前端：动态路由注册 + `defineAsyncComponent` 异步加载

---

### P3.6 AI 辅助能力

**设计**：
- 智能搜索：自然语言 → SQL/API 查询（"上个月新增了多少用户"）
- 操作建议：分析操作日志，识别异常行为模式并主动告警
- 报表解读：图表自动生成文字摘要
- 智能表单填充：根据历史数据推荐填写内容

**技术方案**：
- 后端封装 `AiService`：统一 LLM 调用接口
- 集成讯飞星火大模型 API（与公司技术栈一致）
- Prompt 模板管理：`sys_ai_prompt` 表存储各场景 Prompt
- 流式输出：SSE（Server-Sent Events）实现打字机效果

---

## 依赖关系图

```
P0 (全部独立，可并行开发)
├── P0.1 2FA
├── P0.2 数据脱敏
├── P0.3 审计增强
├── P0.4 并发登录控制
├── P0.5 通用导入导出
└── P0.6 防重完善

P1
├── P1.1 工作流引擎 (独立)
├── P1.2 消息中心升级 (独立)
├── P1.3 数据报表 → 依赖 P1.2（定时报表邮件发送）
├── P1.4 数据权限 (独立)
├── P1.5 维护模式 (独立)
└── P1.6 回收站 (独立)

P2
├── P2.1 多租户 (独立，但影响面最大，建议最先做)
├── P2.2 国际化 (独立)
├── P2.3 消息队列 (独立)
├── P2.4 全文搜索 → 依赖 P2.3（数据同步通道）
├── P2.5 监控告警 → 可选依赖 P1.2（告警通知发送）
└── P2.6 API 版本化 → 可选依赖 P2.1（租户级限流）

P3
├── P3.1 表单设计器 → 可选依赖 P1.1（工作流表单绑定）
├── P3.2 数据大屏 (独立)
├── P3.3 移动端 → 依赖 P1.1（审批是核心移动场景）
├── P3.4 白标能力 → 可选依赖 P2.1（租户级白标）
├── P3.5 插件化 (独立，但建议在 P1/P2 功能稳定后再做)
└── P3.6 AI 辅助 (独立)
```

---

## 数据库变更汇总

| 层级 | 新增表 | 修改表 |
|------|--------|--------|
| P0 | `sys_user_2fa` | `sys_operation_log`（+4 字段） |
| P1 | `biz_process_definition`, `sys_msg_template`, `sys_msg_record`, `sys_report`, `sys_role_dept` | `sys_role`（+data_scope） |
| P2 | `sys_tenant`, `sys_tenant_package`, `sys_i18n_message` | 所有业务表（+tenant_id） |
| P3 | `sys_form_definition`, `sys_form_data`, `sys_ai_prompt` | — |

---

## 技术选型汇总

| 能力 | 技术选型 | 理由 |
|------|----------|------|
| 2FA | googleauth 库 | 轻量、标准 TOTP 实现 |
| 工作流 | Flowable | Spring Boot 原生集成、社区活跃、中文文档丰富 |
| 流程设计器 | BPMN.js | Flowable 官方推荐前端方案 |
| Excel 导入导出 | EasyExcel | 阿里出品，低内存占用，适合大数据量 |
| 消息队列 | RocketMQ | 国内企业生态好，与讯飞技术栈契合 |
| 全文搜索 | Elasticsearch 8.x | 业界标准，支持中文分词 |
| 监控 | Prometheus + Grafana | 云原生标准、开箱即用 |
| 移动端 UI | Vant 4 | Vue 3 生态最成熟的移动端组件库 |
| 插件框架 | PF4J | Java 插件框架，轻量、与 Spring 兼容 |
| AI 大模型 | 讯飞星火 API | 与公司技术栈一致 |
| 表单设计 | VueDraggable + JSON Schema | 灵活、可序列化、前后端解耦 |
| 数据可视化 | ECharts 5 | 功能全面、中文友好、地图支持好 |
