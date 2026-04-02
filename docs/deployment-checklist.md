# 部署准备清单

> 本文档列出了将 Admin Management System 部署到生产环境前需要完成的所有准备工作。

---

## 一、服务器环境要求

### 1.1 硬件最低配置

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 40 GB SSD | 100 GB SSD |
| 带宽 | 1 Mbps | 5 Mbps |

### 1.2 软件依赖

#### Docker 部署方式（推荐）

| 软件 | 版本要求 | 安装检查命令 |
|------|---------|-------------|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.0+ | `docker compose version` |
| Git | 2.30+ | `git --version` |

#### 手动部署方式

| 软件 | 版本要求 | 安装检查命令 |
|------|---------|-------------|
| JDK | 17 | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Node.js | 18+（推荐 20 LTS） | `node -v` |
| npm | 9+ | `npm -v` |
| MySQL | 8.0 | `mysql --version` |
| Redis | 7.0 | `redis-server --version` |
| Nginx | 1.24+ | `nginx -v` |

---

## 二、网络与域名准备

### 2.1 端口规划

| 服务 | 端口 | 对外暴露 | 说明 |
|------|------|---------|------|
| Nginx | 80 / 443 | 是 | 前端 + API 反向代理入口 |
| Spring Boot | 8080 | 否（仅内部） | 后端应用 |
| MySQL | 3306 | 否（仅内部） | 数据库 |
| Redis | 6379 | 否（仅内部） | 缓存 |

### 2.2 防火墙规则

```bash
# 仅开放 80 和 443 端口（以 ufw 为例）
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp    # SSH 管理
sudo ufw enable
```

### 2.3 域名与 SSL

- [ ] 注册域名并完成 DNS 解析，A 记录指向服务器公网 IP
- [ ] 申请 SSL 证书（推荐 Let's Encrypt 免费证书）

```bash
# Let's Encrypt 证书申请示例
sudo apt install certbot
sudo certbot certonly --standalone -d admin.yourdomain.com
```

---

## 三、安全配置（关键）

### 3.1 修改所有默认密码

**这是最重要的步骤，务必在部署前完成。**

编辑 `docker/.env` 文件（从 `.env.example` 复制）：

```bash
cd docker
cp .env.example .env
```

必须修改的变量：

| 变量 | 说明 | 要求 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 至少 16 位，包含大小写字母+数字+特殊字符 |
| `SPRING_DATASOURCE_PASSWORD` | 应用连接数据库密码 | 与 MYSQL_ROOT_PASSWORD 一致（或创建专用用户） |
| `REDIS_PASSWORD` | Redis 认证密码 | 至少 16 位强密码 |
| `JWT_SECRET` | JWT 签名密钥 | 至少 64 位随机字符串 |

生成强密码：

```bash
# 生成 32 位随机密码
openssl rand -base64 32

# 生成 64 位 JWT 密钥
openssl rand -base64 64
```

### 3.2 修改默认用户密码

部署完成后立即登录系统修改默认账户密码：

| 账户 | 默认密码 | 操作 |
|------|---------|------|
| admin | admin123 | **必须修改** |
| user | user123 | **必须修改** |

### 3.3 生产环境配置调整

| 配置项 | 开发值 | 生产值 | 说明 |
|--------|-------|-------|------|
| `SWAGGER_ENABLED` | true | **false** | 关闭 API 文档 |
| `LOG_LEVEL` | debug | **info** 或 **warn** | 降低日志级别 |
| `CORS_ALLOWED_ORIGINS` | localhost | **https://admin.yourdomain.com** | 限制跨域来源 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 1800000 | 1800000（30 分钟）| 可根据需求调整 |

---

## 四、数据库准备

### 4.1 Docker 部署（自动初始化）

Docker Compose 会自动执行 `sql/schema.sql` 和 `sql/data.sql`，无需手动操作。

### 4.2 手动部署

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 执行建库建表脚本
source /path/to/sql/schema.sql;

# 3. 执行初始数据脚本
source /path/to/sql/data.sql;
```

### 4.3 数据库安全加固

- [ ] 创建专用数据库用户（不使用 root）
- [ ] 仅授予必要权限（SELECT, INSERT, UPDATE, DELETE）
- [ ] 禁止远程 root 登录

```sql
-- 创建专用用户
CREATE USER 'admin_app'@'%' IDENTIFIED BY 'your_strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON admin_db.* TO 'admin_app'@'%';
FLUSH PRIVILEGES;
```

### 4.4 数据库备份策略

```bash
# 每日自动备份脚本（添加到 crontab）
# 每天凌晨 2:00 执行备份，保留 30 天
0 2 * * * docker exec admin-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} admin_db | gzip > /backup/admin_db_$(date +\%Y\%m\%d).sql.gz

# 清理 30 天前的备份
0 3 * * * find /backup -name "admin_db_*.sql.gz" -mtime +30 -delete
```

---

## 五、构建与部署步骤

### 方式一：Docker Compose 一键部署（推荐）

```bash
# 1. 克隆代码
git clone <your-repo-url> && cd code

# 2. 构建前端产物
cd admin-frontend
npm ci
npm run build
cd ..

# 3. 配置环境变量
cd docker
cp .env.example .env
vim .env  # 修改所有密码和密钥

# 4. 构建并启动
docker compose up -d --build

# 5. 检查服务状态
docker compose ps
docker compose logs -f  # 查看日志
```

### 方式二：手动部署

```bash
# === 后端 ===
cd admin-backend
mvn clean package -DskipTests
# JAR 包位置: target/admin-backend-0.0.1-SNAPSHOT.jar

# 启动（使用 prod profile）
java -jar target/admin-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:mysql://localhost:3306/admin_db \
  --spring.datasource.password=your_password \
  --spring.data.redis.password=your_redis_password \
  --jwt.secret=your_jwt_secret

# === 前端 ===
cd admin-frontend
npm ci
npm run build
# 产物目录: dist/

# 将 dist/ 目录部署到 Nginx
sudo cp -r dist/* /usr/share/nginx/html/
```

---

## 六、Nginx 配置（手动部署时）

项目已提供 Nginx 配置模板：`docker/nginx/nginx.conf`

关键配置点：

```nginx
# 1. 反向代理后端 API
location /api/ {
    proxy_pass http://localhost:8080;  # 手动部署改为 localhost
}

# 2. 前端 SPA 路由支持
location / {
    root /usr/share/nginx/html;
    try_files $uri $uri/ /index.html;
}

# 3. 启用 HTTPS（生产必须）
# 取消 nginx.conf 中 HTTPS server 块的注释，配置证书路径：
ssl_certificate     /etc/letsencrypt/live/admin.yourdomain.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/admin.yourdomain.com/privkey.pem;
```

---

## 七、健康检查与验证

部署完成后执行以下验证：

```bash
# 1. 检查所有容器运行状态
docker compose ps
# 所有服务应显示 "Up" 和 "healthy"

# 2. 检查后端健康
curl http://localhost:8080/api/auth/info
# 应返回 JSON 响应

# 3. 检查前端页面
curl -I http://localhost
# 应返回 200 OK

# 4. 检查 MySQL 连接
docker exec admin-mysql mysqladmin ping -u root -p${MYSQL_ROOT_PASSWORD}

# 5. 检查 Redis 连接
docker exec admin-redis redis-cli -a ${REDIS_PASSWORD} ping
# 应返回 PONG

# 6. 测试登录功能
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 应返回包含 token 的 JSON
```

---

## 八、HTTPS 启用（生产必须）

### 8.1 使用 Let's Encrypt

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d admin.yourdomain.com

# 自动续期（certbot 默认会添加定时任务）
sudo certbot renew --dry-run
```

### 8.2 修改 Nginx 配置

取消 `docker/nginx/nginx.conf` 中 HTTPS 相关配置的注释，并确保：
- HTTP 80 端口自动跳转到 HTTPS 443
- 启用 HSTS 头
- 使用 TLSv1.2 和 TLSv1.3

---

## 九、日志与监控

### 9.1 日志查看

```bash
# Docker 容器日志
docker compose logs -f backend    # 后端日志
docker compose logs -f nginx      # Nginx 访问日志
docker compose logs -f mysql      # 数据库日志

# 日志文件（容器内）
docker exec admin-backend cat /app/logs/application.log
```

### 9.2 建议的监控项

| 监控项 | 工具建议 | 告警阈值 |
|--------|---------|---------|
| 服务器 CPU / 内存 | Prometheus + Grafana | CPU > 80%, 内存 > 85% |
| 磁盘使用率 | Node Exporter | > 80% |
| Docker 容器状态 | cAdvisor | 容器非 healthy 状态 |
| API 响应时间 | Nginx access log 分析 | P99 > 2s |
| MySQL 慢查询 | slow_query_log | > 1s |
| SSL 证书过期 | certbot / 外部监控 | < 14 天 |

---

## 十、部署前最终检查清单

### 环境准备
- [ ] 服务器满足最低硬件要求
- [ ] Docker / Docker Compose 已安装（或手动部署所需软件已安装）
- [ ] 防火墙仅开放 80、443、22 端口

### 安全
- [ ] `docker/.env` 中所有默认密码已更换为强密码
- [ ] JWT_SECRET 已替换为随机生成的密钥
- [ ] SWAGGER_ENABLED 设为 false
- [ ] CORS_ALLOWED_ORIGINS 设为实际域名
- [ ] 默认用户密码（admin/user）部署后立即修改

### 域名与 HTTPS
- [ ] 域名已注册，DNS A 记录已配置
- [ ] SSL 证书已申请
- [ ] Nginx HTTPS 配置已启用
- [ ] HTTP 自动跳转 HTTPS

### 数据
- [ ] 数据库已初始化（schema.sql + data.sql）
- [ ] 数据库备份策略已配置
- [ ] 如使用专用数据库用户，已创建并授权

### 验证
- [ ] 所有容器/服务运行正常（healthy）
- [ ] 前端页面可正常访问
- [ ] 登录功能正常
- [ ] API 接口可正常调用
- [ ] HTTPS 访问正常

### 运维
- [ ] 日志收集已配置
- [ ] 数据库定时备份已设置
- [ ] SSL 证书自动续期已配置
- [ ] 服务器基础监控已部署

---

## 附录：常见问题

### Q1: Docker 容器启动失败？
```bash
# 查看详细日志
docker compose logs <service-name>
# 常见原因：端口占用、内存不足、密码配置错误
```

### Q2: 前端页面白屏？
- 检查 `admin-frontend/dist/` 是否已构建
- 检查 Nginx 配置中 root 路径是否正确
- 检查浏览器控制台是否有 JS 错误

### Q3: API 返回 502？
- 后端服务可能还在启动中，等待 30 秒后重试
- 检查后端日志：`docker compose logs backend`
- 确认 MySQL 和 Redis 都已启动且健康

### Q4: 数据库连接失败？
- 确认 `.env` 中 `MYSQL_ROOT_PASSWORD` 和 `SPRING_DATASOURCE_PASSWORD` 一致
- 确认 MySQL 容器已启动且健康：`docker compose ps mysql`

### Q5: 如何更新部署？
```bash
# 1. 拉取最新代码
git pull origin main

# 2. 重新构建前端
cd admin-frontend && npm ci && npm run build && cd ..

# 3. 重新构建并重启
cd docker && docker compose up -d --build
```
