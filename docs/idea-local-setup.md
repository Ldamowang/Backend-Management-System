# IntelliJ IDEA 本地开发环境搭建指南

> 本文档指导你从零开始在 IntelliJ IDEA 中运行后端服务，并配合前端 Vite 开发服务器进行本地联调。

---

## 前置条件

| 工具 | 版本要求 | 验证命令 |
|------|---------|---------|
| IntelliJ IDEA | 2023.1+ (社区版/旗舰版均可) | — |
| JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |
| Docker | 20+ | `docker -v` |
| Docker Compose | 2.0+ | `docker compose version` |
| Git | 2.30+ | `git --version` |

---

## 第一步：启动 MySQL 和 Redis

有两种方式，任选其一。

### 方式 A：使用 docker-compose（推荐）

```bash
cd docker

# 1. 创建 .env 文件
cp .env.example .env
```

编辑 `docker/.env`，填入实际值：

```env
# MySQL
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=admin_db

# Redis
REDIS_PASSWORD=redis123456

# Spring Boot（docker-compose 部署时使用，本地 IDEA 开发可忽略）
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root123456

# JWT 密钥（至少 32 字节，可用以下命令生成）
# openssl rand -base64 48
JWT_SECRET=YourBase64EncodedSecretKeyAtLeast32BytesLong==
```

```bash
# 2. 只启动 MySQL 和 Redis（不启动 backend 和 nginx）
docker compose up -d mysql redis
```

```bash
# 3. 验证服务启动成功
docker compose ps
# 应看到 admin-mysql 和 admin-redis 状态为 running(healthy)
```

> **说明**：docker-compose 会自动挂载 `sql/schema.sql` 和 `sql/data.sql` 到 MySQL 容器的初始化目录，**首次启动时自动建表导数据**，无需手动执行 SQL。

### 方式 B：手动启动 Docker 容器

```bash
# MySQL
docker run -d --name admin-mysql \
  -e MYSQL_ROOT_PASSWORD=root123456 \
  -e MYSQL_DATABASE=admin_db \
  -p 127.0.0.1:3306:3306 \
  mysql:8.0 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# Redis
docker run -d --name admin-redis \
  -p 127.0.0.1:6379:6379 \
  redis:7-alpine

# 等待 MySQL 完全启动（约 30 秒），然后手动初始化数据库
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/schema.sql
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/data.sql
```

> 如果使用方式 B，Redis 无密码。后续 IDEA 环境变量中 `REDIS_PASSWORD` 留空即可。

---

## 第二步：IDEA 导入后端项目

### 2.1 打开项目

1. **File → Open** → 选择 `code/admin-backend` 目录
2. IDEA 会自动识别为 Maven 项目，等待依赖下载完成
3. 如果提示 "Trust this project?"，选择 **Trust Project**

### 2.2 配置 JDK 17

1. **File → Project Structure** (`Cmd + ;`)
2. 左侧选 **Project**
3. **SDK** 下拉选择 JDK 17
   - 如果没有，点击 **Add SDK → Download JDK** → 选择 Vendor: `Azul Zulu` / Version: `17`
   - 或者手动指定本机 JDK 路径，例如：
     ```
     /Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
     ```
4. **Language level** 选择 `17`
5. 点击 **Apply → OK**

### 2.3 配置 Maven

1. **Settings → Build, Execution, Deployment → Build Tools → Maven**
2. **Maven home path**: 指向你的 Maven 安装目录
   - 例如: `/Users/yili/Documents/iflytek-file/apache-maven-3.8.6`
3. **User settings file**: 如果有自定义 `settings.xml`（如私服镜像），勾选 Override 并指定路径
4. 点击 **Apply → OK**

---

## 第三步：配置后端 Run Configuration

### 3.1 创建 Spring Boot 运行配置

1. 顶部工具栏点击 **Run/Debug Configurations**（运行配置下拉 → **Edit Configurations**）
2. 点击左上角 **+** → 选择 **Spring Boot**
3. 填写配置：

| 配置项 | 值 |
|--------|-----|
| **Name** | `AdminApplication` |
| **Main class** | `com.iflytek.admin.AdminApplication` |
| **Active profiles** | `dev` |
| **JRE** | 选择 JDK 17 |

### 3.2 配置环境变量（关键步骤）

在运行配置中找到 **Environment variables** 字段，点击右侧的 **...** 按钮，添加以下变量：

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | `root123456` | MySQL 密码（与 Docker 启动时设置的一致） |
| `REDIS_PASSWORD` | `redis123456` | Redis 密码（方式 A）；方式 B 留空 |
| `JWT_SECRET` | 一个 Base64 编码的字符串 | **必须至少 32 字节**，见下方说明 |

> **生成 JWT_SECRET**：
> ```bash
> openssl rand -base64 48
> ```
> 复制输出结果作为 `JWT_SECRET` 的值。
>
> 或者直接用这个示例值（仅限本地开发）：
> ```
> Y2xhdWRlLWNvZGUtYWRtaW4tc3lzdGVtLWp3dC1zZWNyZXQtMjAyNA==
> ```

也可以用分号分隔写在一行中：
```
DB_USERNAME=root;DB_PASSWORD=root123456;REDIS_PASSWORD=redis123456;JWT_SECRET=Y2xhdWRlLWNvZGUtYWRtaW4tc3lzdGVtLWp3dC1zZWNyZXQtMjAyNA==
```

### 3.3 点击 Apply → OK

---

## 第四步：启动后端服务

1. 选择刚创建的 `AdminApplication` 运行配置
2. 点击 **Run**（▶️）或 **Debug**（🐞）按钮
3. 观察控制台输出，看到以下日志说明启动成功：

```
Started AdminApplication in X.XXX seconds
```

4. 验证后端可用：
   - 浏览器访问 http://localhost:8080/api/auth/login（应返回 405 Method Not Allowed，说明接口可达）
   - 如果启用了 Swagger：http://localhost:8080/swagger-ui.html

---

## 第五步：启动前端开发服务器

```bash
cd admin-frontend

# 安装依赖（首次或 package.json 变更后执行）
npm install

# 启动开发服务器
npm run dev
```

启动成功后，终端会显示：

```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
```

> Vite 已配置代理，所有 `/api` 请求会自动转发到后端 `http://localhost:8080`，无需额外配置 CORS。

---

## 第六步：验证联调

1. 浏览器打开 http://localhost:5173
2. 使用默认账号登录：
   - 管理员: `admin` / `admin123`
   - 普通用户: `user` / `user123`
3. 登录后应能看到左侧菜单和主页面内容
4. 修改前端代码 → 浏览器自动热更新（HMR）
5. 在 IDEA 中修改后端代码 → 重启后端服务生效（或配置 Spring Boot DevTools 实现热加载）

---

## 常见问题排查

### 1. 后端启动报 `Communications link failure`

**原因**: MySQL 未启动或连接配置错误。

**排查**:
```bash
# 检查 MySQL 容器状态
docker ps | grep mysql

# 测试连接
mysql -h 127.0.0.1 -u root -proot123456 -e "SELECT 1"
```

### 2. 后端启动报 `JWT_SECRET` 相关错误

**原因**: 环境变量 `JWT_SECRET` 未配置或长度不够。

**解决**: 确保 Run Configuration 中配置了 `JWT_SECRET`，且值至少 32 字节。

### 3. Redis 连接失败

**原因**: Redis 未启动或密码不匹配。

**排查**:
```bash
# 检查 Redis 容器状态
docker ps | grep redis

# 测试连接（有密码时）
docker exec admin-redis redis-cli -a redis123456 ping
# 应返回 PONG

# 测试连接（无密码时）
docker exec admin-redis redis-cli ping
```

### 4. 前端请求返回 502 或网络错误

**原因**: 后端未启动，Vite 代理目标不可达。

**解决**: 确保后端 8080 端口已启动。在前端终端可以看到代理错误日志。

### 5. 前端登录后页面空白、无菜单

**原因**: 数据库未初始化，菜单和角色数据为空。

**解决**: 重新执行 `schema.sql` 和 `data.sql`：
```bash
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/schema.sql
mysql -h 127.0.0.1 -u root -proot123456 admin_db < sql/data.sql
```

### 6. IDEA 中 Maven 依赖下载慢

**解决**: 配置 Maven 镜像源。编辑 Maven 的 `settings.xml`（通常在 `~/.m2/settings.xml`），添加阿里云镜像：

```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 端口总览

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 (Vite) | 5173 | 开发服务器，自动代理 `/api` 到后端 |
| 后端 (Spring Boot) | 8080 | REST API + Swagger |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 + 会话 |

---

## 调试技巧

### 后端调试
- 使用 IDEA 的 **Debug** 模式启动，可在代码中设置断点
- SQL 日志已在 `application-dev.yml` 中开启，所有 SQL 语句会打印在控制台
- Swagger UI（http://localhost:8080/swagger-ui.html）可直接测试 API

### 前端调试
- 浏览器安装 **Vue.js DevTools** 扩展，查看组件树和 Pinia 状态
- 浏览器 DevTools → **Network** 标签页查看 API 请求/响应
- Vite HMR 支持热更新，修改代码后无需手动刷新
