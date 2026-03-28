# CI/CD 指南

## 目录

- [概览](#概览)
- [工作流文件](#工作流文件)
- [前端 CI](#前端-ci)
- [后端 CI](#后端-ci)
- [Docker 构建与部署](#docker-构建与部署)
- [GitHub Secrets 配置](#github-secrets-配置)
- [分支策略与触发规则](#分支策略与触发规则)
- [常见问题](#常见问题)

---

## 概览

项目使用 GitHub Actions 实现自动化 CI/CD，包含三条流水线：

| 工作流 | 触发条件 | 职责 |
|--------|----------|------|
| `frontend-ci.yml` | 前端代码变更时 | Lint → Type Check → Test → Build |
| `backend-ci.yml` | 后端代码/SQL 变更时 | Compile → Test → Package |
| `docker-deploy.yml` | Push 到 main 或打 tag 时 | CI 检查 → Docker 构建 → 部署 |

```
PR / Push
  │
  ├─ admin-frontend/** 变更 → Frontend CI
  ├─ admin-backend/** 或 sql/** 变更 → Backend CI
  │
  └─ Push to main / v* tag → Docker Build & Deploy
       ├─ 门控: 前后端 CI 检查
       ├─ 构建: Docker 镜像 → ghcr.io
       └─ 部署: SSH → docker compose up
```

---

## 工作流文件

```
.github/workflows/
├── frontend-ci.yml       # 前端持续集成
├── backend-ci.yml        # 后端持续集成
└── docker-deploy.yml     # Docker 构建 + 部署
```

---

## 前端 CI

**文件**: `.github/workflows/frontend-ci.yml`

### 触发条件

- Push 到 `main` 或 `develop` 分支，且 `admin-frontend/` 下有文件变更
- PR 指向 `main` 或 `develop`，同上

### 流水线步骤

```
Checkout → Setup Node.js → npm ci → npm audit → Lint → Type Check → Test → Build
```

| 步骤 | 命令 | 说明 |
|------|------|------|
| Install | `npm ci` | 根据 lock 文件精确安装 |
| Audit | `npm audit --audit-level=high` | 依赖安全审计 (警告级) |
| Lint | `npm run lint` | ESLint 代码规范检查 |
| Type Check | `npm run type-check` | vue-tsc TypeScript 类型检查 |
| Test | `npm run test` | Vitest 单元测试 |
| Coverage | `npm run test:coverage` | 仅 Node 20 运行，生成覆盖率报告 |
| Build | `npm run build` | Vite 生产构建 |

### 矩阵测试

同时在 Node.js 18 和 20 上运行，确保兼容性。

### 产物

- `frontend-coverage/` — 测试覆盖率报告 (Node 20)
- `frontend-dist/` — 构建产物 (Node 20)

保留 7 天。

---

## 后端 CI

**文件**: `.github/workflows/backend-ci.yml`

### 触发条件

- Push 到 `main` 或 `develop`，且 `admin-backend/` 或 `sql/` 下有变更
- PR 指向 `main` 或 `develop`，同上

### 服务容器

CI 环境自动启动 MySQL 8.0 和 Redis 7：

| 服务 | 端口 | 健康检查 |
|------|------|----------|
| MySQL | 3306 | `mysqladmin ping` |
| Redis | 6379 | `redis-cli ping` |

### 流水线步骤

```
Checkout → Setup JDK 17 → 初始化数据库 → Compile → Test → Dependency Check → Package
```

| 步骤 | 命令 | 说明 |
|------|------|------|
| DB Init | `mysql < schema.sql && data.sql` | 导入表结构和初始数据 |
| Compile | `mvn compile -B` | 编译 Java 源码 |
| Test | `mvn test -B -Dspring.profiles.active=ci` | 运行单元/集成测试 |
| Dep Check | `mvn dependency:analyze` | 检查未使用/未声明依赖 (警告级) |
| Package | `mvn package -B -DskipTests` | 打包 JAR |

### 产物

- `backend-jar/` — Spring Boot JAR 文件，保留 7 天

---

## Docker 构建与部署

**文件**: `.github/workflows/docker-deploy.yml`

### 触发条件

| 触发 | 条件 |
|------|------|
| Push | `main` 分支 或 `v*` tag |
| 手动 | `workflow_dispatch`，可选 staging/production |

### 完整流程

```
┌─────────────────────────────────────────┐
│            门控 (并行)                    │
│  Frontend CI Check ──┐                  │
│  Backend CI Check  ──┤                  │
└──────────────────────┼──────────────────┘
                       ↓
┌──────────────────────┼──────────────────┐
│         Docker Build (并行)              │
│  Build Backend Image  ──┐               │
│  Build Frontend Image ──┤               │
│                         │               │
│  特性:                    │               │
│  - Docker Buildx 加速     │               │
│  - GHA 缓存层复用         │               │
│  - 自动生成镜像 tag        │               │
└─────────────────────────┼───────────────┘
                          ↓
┌─────────────────────────┼───────────────┐
│              Deploy                      │
│  SSH → docker compose pull              │
│  SSH → docker compose up -d             │
│  SSH → Health check 验证                 │
└──────────────────────────────────────────┘
```

### 镜像 Tag 策略

| 触发类型 | Tag 格式 | 示例 |
|----------|----------|------|
| 分支 Push | `{branch}` | `main` |
| 语义版本 Tag | `{version}` | `1.2.0` |
| 每次 Push | `{sha}` | `a1b2c3d` |

镜像推送到 `ghcr.io/{owner}/{repo}-backend` 和 `ghcr.io/{owner}/{repo}-frontend`。

### Docker 构建缓存

使用 GitHub Actions Cache (`type=gha`) 缓存 Docker 构建层，显著减少重复构建时间。

### 部署后健康检查

部署完成后自动执行：
1. 等待 15 秒让服务启动
2. 请求 `/api/auth/info` 检查后端状态
3. HTTP 200 或 401 (未认证但服务正常) 表示通过
4. 失败时输出最近 50 行日志辅助排查

### 并发控制

- CI 流水线: 同一分支的新 Push 会取消正在运行的旧流水线
- Deploy 流水线: 同一环境的部署不会取消，排队执行 (避免部署中断)

---

## GitHub Secrets 配置

在 GitHub 仓库 Settings → Secrets and variables → Actions 中配置：

### 必需 Secrets

| Secret | 用途 | 示例 |
|--------|------|------|
| `DEPLOY_HOST` | 部署服务器 IP/域名 | `10.0.1.100` |
| `DEPLOY_USER` | SSH 用户名 | `deploy` |
| `DEPLOY_SSH_KEY` | SSH 私钥 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `DEPLOY_PATH` | 服务器上项目路径 | `/opt/admin` |

### 自动提供

| Secret | 说明 |
|--------|------|
| `GITHUB_TOKEN` | GitHub 自动注入，用于推送 Docker 镜像到 ghcr.io |

### Environments 配置

在 Settings → Environments 中创建：

| Environment | 用途 | 建议配置 |
|-------------|------|----------|
| `staging` | 预发布环境 | 无需审批 |
| `production` | 生产环境 | 需要审批人确认 |

---

## 分支策略与触发规则

```
main ──────────────────────────────────────────→ (生产)
  │                                    ↑
  └─ develop ─────────────────────────→ PR merge
       │                   ↑
       └─ feature/xxx ────→ PR merge
```

| 事件 | Frontend CI | Backend CI | Docker Deploy |
|------|-------------|------------|---------------|
| Push to `feature/*` | - | - | - |
| PR → `develop` | 如有前端变更 | 如有后端/SQL 变更 | - |
| PR → `main` | 如有前端变更 | 如有后端/SQL 变更 | - |
| Push to `develop` | 如有前端变更 | 如有后端/SQL 变更 | - |
| Push to `main` | 如有前端变更 | 如有后端/SQL 变更 | 触发 |
| Push `v*` tag | - | - | 触发 |
| 手动触发 | - | - | 触发 |

---

## 常见问题

### Q: CI 中 npm audit 报警怎么办？

**A:** `npm audit` 设为警告级别 (不阻塞构建)。本地运行 `npm audit` 查看详情，使用 `npm audit fix` 修复，或评估风险后忽略。

### Q: Backend CI 测试找不到数据库怎么办？

**A:** CI 使用 GitHub Actions 服务容器提供 MySQL/Redis。确保：
1. `sql/schema.sql` 和 `sql/data.sql` 已提交
2. 后端有 `ci` profile 配置，或测试通过环境变量连接

### Q: Docker 构建缓存不生效？

**A:** GitHub Actions Cache 有 10GB 总量限制。旧缓存会被自动清理。首次构建或缓存过期时会全量构建。

### Q: 如何手动触发部署？

**A:**
1. 进入 GitHub → Actions → "Docker Build & Deploy"
2. 点击 "Run workflow"
3. 选择环境 (staging/production)
4. 点击 "Run workflow" 确认

### Q: 部署失败如何回滚？

**A:** 镜像按 git SHA 打 tag，回滚方式：
```bash
# 在服务器上手动指定旧版本镜像
docker compose pull ghcr.io/{repo}-backend:{old-sha}
docker compose up -d
```

### Q: 如何添加新的部署环境？

**A:**
1. 在 `docker-deploy.yml` 的 `workflow_dispatch.inputs.environment.options` 中添加选项
2. 在 GitHub Settings → Environments 创建对应环境
3. 为新环境配置 Secrets (DEPLOY_HOST 等)
