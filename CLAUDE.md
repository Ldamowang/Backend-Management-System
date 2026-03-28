# Admin Management System

## Project Overview

企业级后台管理系统，前后端分离架构。

| Layer | Tech Stack |
|-------|------------|
| Frontend | Vue 3 + TypeScript + Element Plus + Vite + Pinia + Axios |
| Backend | Java 17 + Spring Boot 3.2 + Spring Security 6 + MyBatis-Plus + JWT |
| Database | MySQL 8 + Redis 7 |
| Deploy | Docker Compose + Nginx |

## Project Structure

```
code/
├── admin-frontend/          # Vue 3 前端项目
│   ├── src/
│   │   ├── api/             # API 请求层 (Axios)
│   │   ├── assets/          # 静态资源 & 样式
│   │   ├── components/      # 全局公共组件
│   │   ├── composables/     # 组合式函数 (Hooks)
│   │   ├── directives/      # 自定义指令 (v-permission)
│   │   ├── plugins/         # 插件 (Element Plus)
│   │   ├── router/          # 路由 (静态 + 动态)
│   │   ├── stores/          # Pinia 状态管理
│   │   ├── types/           # TypeScript 类型定义
│   │   ├── utils/           # 工具函数
│   │   └── views/           # 页面视图
│   └── .env.*               # 环境变量
├── admin-backend/           # Spring Boot 后端项目
│   └── src/main/java/com/iflytek/admin/
│       ├── common/          # 通用模块 (config/exception/result/utils)
│       ├── security/        # Spring Security + JWT
│       ├── modules/         # 业务模块 (auth/system/profile)
│       └── base/            # 基础类
├── sql/                     # SQL 脚本 (schema + data)
├── docker/                  # Docker 配置
└── docs/                    # 项目文档
```

## Development Commands

### Frontend
```bash
cd admin-frontend
npm install          # 安装依赖
npm run dev          # 启动开发服务器 (localhost:5173)
npm run build        # 生产构建
npm run type-check   # TypeScript 类型检查
npm run lint         # ESLint 检查
npm run test         # Vitest 单元测试
```

### Backend
```bash
cd admin-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 启动开发模式 (localhost:8080)
mvn clean package                                      # 打包
mvn test                                               # 运行测试
```

### Docker
```bash
cd docker
docker-compose up -d    # 一键启动 (MySQL + Redis + Backend + Nginx)
docker-compose down     # 停止所有服务
```

## Architecture Decisions

### Authentication Flow
1. 用户提交用户名密码 -> POST `/api/auth/login`
2. 后端验证 -> 返回 Access Token (30min) + Refresh Token (7d)
3. 前端 Axios 拦截器自动在 Header 注入 `Authorization: Bearer <token>`
4. Token 过期 -> 自动使用 Refresh Token 静默续期
5. Refresh Token 过期 -> 跳转登录页

### Permission Model (RBAC)
- User -> Role -> Menu/Permission (多对多关系)
- 菜单权限控制路由可见性（动态路由）
- 按钮权限使用 `v-permission` 指令控制
- 权限标识格式: `module:entity:action` (如 `sys:user:add`)

### API Convention
- Base URL: `/api`
- RESTful 风格: GET(查) / POST(增) / PUT(改) / DELETE(删) / PATCH(部分改)
- 统一响应: `{ code: number, message: string, data: T }`
- 分页响应: `{ code, message, data: { list: T[], total, page, size } }`
- 错误码: 200 成功, 400 参数错误, 401 未认证, 403 无权限, 500 服务器错误

### Database Convention
- 表名前缀: `sys_`
- 主键: `id` BIGINT AUTO_INCREMENT
- 逻辑删除: `deleted` TINYINT (0正常/1删除)
- 审计字段: `created_by`, `created_time`, `updated_by`, `updated_time`
- 所有时间字段使用 DATETIME 类型

## Key Files

| Purpose | Frontend | Backend |
|---------|----------|---------|
| Entry | `src/main.ts` | `AdminApplication.java` |
| Config | `vite.config.ts` | `application.yml` |
| Auth | `stores/modules/user.ts` | `security/SecurityConfig.java` |
| Routes | `router/index.ts` | Controller 层 |
| API Layer | `api/request.ts` | `common/result/Result.java` |
| State | `stores/` | N/A |

## Default Credentials

- Admin: `admin` / `admin123`
- User: `user` / `user123`

## Ports

| Service | Port |
|---------|------|
| Frontend (Vite dev) | 5173 |
| Backend (Spring Boot) | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
