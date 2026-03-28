# 系统架构文档

## 目录

- [总体架构](#总体架构)
- [技术栈](#技术栈)
- [前端架构](#前端架构)
- [后端架构](#后端架构)
- [数据流](#数据流)
- [安全架构](#安全架构)
- [部署架构](#部署架构)

---

## 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端 (Browser)                       │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTPS
┌─────────────────────▼───────────────────────────────────────┐
│                     Nginx (反向代理)                          │
│  ┌──────────────────────┐  ┌──────────────────────────────┐ │
│  │  / → 静态文件 (SPA)   │  │  /api/ → proxy_pass :8080   │ │
│  └──────────────────────┘  └──────────────┬───────────────┘ │
└─────────────────────────────────────────────┼───────────────┘
                                             │
┌────────────────────────────────────────────▼────────────────┐
│                Spring Boot 3.2 (后端服务)                     │
│                                                              │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │  Controller  │→│   Service    │→│  Mapper (MyBatis+) │   │
│  └──────┬──────┘  └──────┬───────┘  └────────┬─────────┘   │
│         │                │                    │              │
│  ┌──────▼──────┐  ┌──────▼───────┐           │              │
│  │  Security   │  │   Redis      │           │              │
│  │  (JWT 认证)  │  │  (缓存/Token) │           │              │
│  └─────────────┘  └──────────────┘           │              │
└──────────────────────────────────────────────┼──────────────┘
                                               │
┌──────────────────────────────────────────────▼──────────────┐
│                     MySQL 8.0 (数据库)                       │
│                  admin_db / utf8mb4                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **前端框架** | Vue 3 | 3.5+ | 响应式 UI 框架 (Composition API) |
| **前端构建** | Vite | 7.0 | 开发服务器 + 生产构建 |
| **UI 组件库** | Element Plus | 2.13+ | 企业级 UI 组件 |
| **状态管理** | Pinia | 2.3+ | Vue 3 官方状态管理 |
| **HTTP 客户端** | Axios | 1.7+ | 请求拦截 / Token 自动注入 |
| **图表** | ECharts + vue-echarts | 5.5+ | 仪表盘数据可视化 |
| **后端框架** | Spring Boot | 3.2.5 | Java 后端框架 |
| **安全** | Spring Security 6 | 6.x | 认证与授权 |
| **ORM** | MyBatis-Plus | 3.5.5 | 数据库访问层 |
| **JWT** | jjwt | 0.12.3 | Token 签发与验证 |
| **对象映射** | MapStruct | 1.5.5 | DTO/Entity 转换 |
| **API 文档** | SpringDoc OpenAPI | 2.3.0 | Swagger UI |
| **数据库** | MySQL | 8.0 | 关系型数据库 |
| **缓存** | Redis | 7.x | Token 存储 / 数据缓存 |

---

## 前端架构

### 分层结构

```
views/         ← 页面视图 (路由绑定)
  ↓ 调用
api/           ← API 请求层 (Axios 封装)
  ↓ 依赖
stores/        ← 状态管理层 (Pinia)
  ↓ 使用
composables/   ← 组合式函数 (复用逻辑)
components/    ← 全局公共组件
directives/    ← 自定义指令 (v-permission)
types/         ← TypeScript 类型定义
utils/         ← 工具函数
```

### 路由体系

- **静态路由**: 登录页、404、首页等基础页面
- **动态路由**: 根据用户权限从后端菜单树动态生成，无权限的菜单不展示

### 请求拦截链

```
Request → Axios 请求拦截器 → 注入 Bearer Token → 发送请求
                                                      ↓
Response ← Axios 响应拦截器 ← 判断状态码
    │                                │
    │  401: Token 过期               │  200: 正常返回
    │  → 尝试 Refresh Token          │
    │  → 失败则跳转登录页              │
    └────────────────────────────────┘
```

---

## 后端架构

### 分层结构

```
Controller (接口层)
  │  接收请求, 参数校验, 权限检查 (@PreAuthorize)
  ↓
Service (业务层)
  │  业务逻辑, 事务管理, DTO 转换
  ↓
Mapper (数据访问层)
  │  MyBatis-Plus CRUD, 自定义 SQL
  ↓
MySQL (数据库)
```

### 模块划分

| 模块 | 路径 | 职责 |
|------|------|------|
| auth | `modules/auth/` | 登录、登出、Token 刷新 |
| system | `modules/system/` | 用户、角色、菜单、配置、日志管理 |
| profile | `modules/profile/` | 个人中心 (修改信息/密码) |
| common | `common/` | 通用配置、异常处理、工具类 |
| security | `security/` | Spring Security + JWT 过滤器 |

### 异常处理

全局异常处理器 (`@RestControllerAdvice`) 捕获所有异常，统一返回 `{ code, message, data }` 格式。

---

## 数据流

### 认证流程

```
1. POST /api/auth/login
   ├─ 验证用户名密码 (Spring Security AuthenticationManager)
   ├─ 签发 Access Token (30min) + Refresh Token (7d)
   └─ 返回 Token 对

2. 携带 Token 访问 API
   ├─ JwtAuthenticationFilter 解析 Token
   ├─ 验证签名 + 过期时间
   ├─ 加载用户权限到 SecurityContext
   └─ @PreAuthorize 检查接口权限

3. Token 过期
   ├─ 前端检测到 401 响应
   ├─ POST /api/auth/refresh (携带 Refresh Token)
   ├─ 签发新 Token 对
   └─ 重放原始请求

4. Refresh Token 过期
   └─ 跳转登录页，清除本地存储
```

### RBAC 权限模型

```
User ──M:N──> Role ──M:N──> Menu/Permission
 │                            │
 │                     ┌──────┴──────┐
 │                     │  menuType   │
 │                     ├─────────────┤
 │                     │ 1 = 目录     │ → 侧边栏分组
 │                     │ 2 = 菜单     │ → 动态路由
 │                     │ 3 = 按钮     │ → v-permission 指令
 │                     └─────────────┘
 │
 └─ 权限标识格式: module:entity:action
    例: sys:user:add, sys:role:delete
```

---

## 安全架构

### 多层防护

| 层级 | 措施 |
|------|------|
| **网络层** | Nginx 隐藏版本号, 安全响应头 (CSP, X-Frame-Options 等) |
| **传输层** | HTTPS (生产环境), CORS 白名单 |
| **认证层** | JWT (HS256), Access + Refresh 双 Token 机制 |
| **授权层** | Spring Security RBAC, @PreAuthorize 注解 |
| **应用层** | 输入校验 (@Valid), 全局异常处理, 日志审计 (@Log) |
| **数据层** | 参数化查询 (MyBatis-Plus), 逻辑删除, 密码 BCrypt 加密 |
| **容器层** | 非 root 用户运行, 内部端口不暴露 |

### 敏感数据处理

- 密码: BCrypt 单向哈希，不可逆
- JWT Secret: 环境变量注入，不硬编码
- 数据库凭据: Docker Compose `.env` 文件管理，不提交到仓库
- Redis 密码: 同上

---

## 部署架构

### Docker Compose 架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                           │
│                                                              │
│  ┌──────────┐  ┌───────────┐  ┌──────────┐  ┌───────────┐ │
│  │  Nginx   │  │  Backend  │  │  MySQL   │  │   Redis   │ │
│  │  :80     │→│  :8080    │→│  :3306   │  │   :6379   │ │
│  │  (公开)   │  │  (内部)    │  │  (内部)   │  │   (内部)   │ │
│  └──────────┘  └───────────┘  └──────────┘  └───────────┘ │
│                                     │                        │
│                              mysql-data (卷)                 │
└─────────────────────────────────────────────────────────────┘
```

### 启动顺序

1. **MySQL** → 健康检查通过 (mysqladmin ping)
2. **Redis** → 健康检查通过 (redis-cli ping)
3. **Backend** → 依赖 MySQL + Redis 就绪
4. **Nginx** → 依赖 Backend 就绪

### CI/CD 流水线

```
Push to main/tag
  ├─ Frontend CI Check (lint → type-check → build)
  ├─ Backend CI Check (compile → package)
  ↓
Docker Build (并行)
  ├─ Build Backend Image → ghcr.io
  ├─ Build Frontend Image → ghcr.io
  ↓
Deploy (SSH)
  ├─ docker compose pull
  ├─ docker compose up -d
  └─ Health check 验证
```

详细流水线配置参见 [CI/CD 指南](./ci-cd-guide.md)。
