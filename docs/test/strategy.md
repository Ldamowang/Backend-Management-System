# 测试策略文档

> 状态：草稿
> 日期：2026-03-17
> 作者：测试工程师（待指定）

## 1. 测试目标

- 单元测试覆盖率 >= 80%（前后端各自达标）
- E2E 覆盖全部核心用户路径
- API 接口 100% 覆盖（正向 + 反向）
- 零 P0/P1 Bug 上线

## 2. 测试分层

```
┌──────────────────────┐
│     E2E 测试          │  Playwright - 核心用户路径
├──────────────────────┤
│   API 接口测试        │  Postman/Insomnia - 全部端点
├──────────────────────┤
│   集成测试            │  MockMvc(后端) / MSW(前端) - 模块间交互
├──────────────────────┤
│   单元测试            │  JUnit5(后端) / Vitest(前端) - 函数/组件
└──────────────────────┘
```

## 3. 工具选型

| 测试类型 | 后端 | 前端 |
|---------|------|------|
| 单元测试 | JUnit 5 + Mockito | Vitest + Vue Test Utils |
| 集成测试 | MockMvc + SpringBootTest | MSW (Mock Service Worker) |
| E2E 测试 | — | Playwright |
| API 测试 | Postman Collection | — |
| 性能测试 | JMeter / k6 | Lighthouse |
| 安全测试 | OWASP ZAP / 手动 | — |
| 覆盖率 | JaCoCo | Istanbul (Vitest built-in) |

## 4. 测试范围

### 4.1 后端单元测试

| 模块 | 当前状态 | 待补充 |
|------|---------|--------|
| AuthServiceImpl | ✅ 已有 | 并发刷新 Token 场景 |
| UserServiceImpl | ✅ 已有 | 重复邮箱、超长字段边界 |
| RoleServiceImpl | ✅ 已有 | 角色关联菜单完整性 |
| MenuServiceImpl | ✅ 已有 | 删除有子菜单的父菜单 |
| DtoValidation | ✅ 已有 | — |
| **Controller 层** | ❌ 全部缺失 | AuthController/UserController/RoleController/MenuController/ConfigController/LogController/DashboardController/ProfileController |
| LogAspect | ❌ 缺失 | AOP 日志记录验证 |
| JwtUtil | ❌ 缺失 | Token 生成/解析/过期 |
| GlobalExceptionHandler | ❌ 缺失 | 各类异常映射 |

### 4.2 前端单元测试

| 模块 | 当前状态 | 待补充 |
|------|---------|--------|
| utils/auth | ✅ 已有 | — |
| utils/tree | ✅ 已有 | — |
| utils/formatter | ✅ 已有 | — |
| stores/app | ✅ 已有 | — |
| stores/user | ✅ 已有 | — |
| **stores/permission** | ❌ 缺失 | 动态路由生成逻辑 |
| **utils/validate** | ❌ 缺失 | 校验函数 |
| **views/** | ❌ 全部缺失 | 关键页面组件测试 |
| **components/** | ❌ 全部缺失 | Layout 等公共组件 |
| **composables/** | ❌ 缺失 | usePagination/usePermission |

### 4.3 E2E 测试用例

| 场景 | 优先级 | 步骤 |
|------|--------|------|
| 登录成功 | P0 | 输入 admin/admin123 → 跳转仪表盘 |
| 登录失败 | P0 | 输入错误密码 → 显示错误提示 |
| 用户 CRUD | P0 | 新增用户 → 编辑 → 禁用 → 删除 |
| 角色 CRUD | P1 | 新增角色 → 分配权限 → 编辑 → 删除 |
| 权限拦截 | P0 | user 角色登录 → 访问无权限页面 → 显示 403 |
| Token 刷新 | P1 | 等待 Token 过期 → 自动刷新 → 请求成功 |
| 个人中心 | P2 | 修改昵称 → 修改密码 → 重新登录 |

### 4.4 安全测试

| 测试项 | 方法 |
|--------|------|
| SQL 注入 | 搜索框输入 `' OR 1=1 --`，验证参数化查询 |
| XSS | 输入框输入 `<script>alert(1)</script>`，验证转义 |
| 越权访问 | user 角色直接调用 admin 接口，验证 403 |
| Token 伪造 | 修改 JWT payload 发请求，验证 401 |
| 接口限流 | 快速重复请求同一接口，验证限流响应 |

### 4.5 性能测试

| 场景 | 指标 |
|------|------|
| 并发登录 | 100 用户并发登录，响应 < 2s |
| 用户列表 | 10000 条数据分页查询 < 500ms |
| 仪表盘 | 统计数据加载 < 1s |
| 前端首屏 | Lighthouse Performance Score >= 80 |

## 5. 测试流程

```
开发提交代码
    ↓
自动运行单元测试 (CI)
    ↓
部署到测试环境
    ↓
执行 API 接口测试
    ↓
执行 E2E 自动化测试
    ↓
手动探索性测试
    ↓
提交 Bug 报告
    ↓
开发修复 → 回归验证
    ↓
输出测试报告
```

## 6. Bug 管理

### 优先级定义

- **P0（阻塞）**：系统崩溃、数据丢失、安全漏洞 → 立即修复
- **P1（严重）**：核心功能异常、权限绕过 → 当天修复
- **P2（一般）**：交互异常、非核心功能缺陷 → 3 天内修复
- **P3（建议）**：体验优化、文案调整 → 下一迭代

### Bug 报告模板

```markdown
## Bug 标题

**环境**：开发/测试/生产
**优先级**：P0/P1/P2/P3
**复现步骤**：
1. ...
2. ...
3. ...

**期望结果**：...
**实际结果**：...
**截图/日志**：...
```

## 7. 准入/准出标准

### 准入（开始测试的条件）

- [ ] 功能开发完成，开发自测通过
- [ ] 单元测试通过，无编译错误
- [ ] 测试环境可用

### 准出（允许上线的条件）

- [ ] 单元测试覆盖率 >= 80%
- [ ] E2E 核心路径全部通过
- [ ] 零 P0/P1 Bug
- [ ] P2 Bug 数量 < 5
- [ ] 安全测试无高危漏洞
- [ ] 性能指标达标
