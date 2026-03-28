# 部署指南

## 目录

- [环境要求](#环境要求)
- [快速部署 (Docker Compose)](#快速部署-docker-compose)
- [手动部署](#手动部署)
- [生产环境配置](#生产环境配置)
- [健康检查与监控](#健康检查与监控)
- [常见问题](#常见问题)

---

## 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Docker | 20.10+ | 24+ |
| Docker Compose | 2.0+ | 2.24+ |
| JDK (手动部署) | 17 | 17 |
| Node.js (手动部署) | 18 | 20 LTS |
| MySQL | 8.0 | 8.0 |
| Redis | 7.0 | 7.2 |
| Nginx | 1.24+ | 1.25+ |

**服务器配置建议：**

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发/测试 | 2 核 | 4 GB | 20 GB |
| 生产 | 4 核+ | 8 GB+ | 50 GB+ |

---

## 快速部署 (Docker Compose)

### 1. 准备环境变量

```bash
# 在项目根目录
cp .env.example .env

# 编辑 .env 文件，修改以下关键配置:
# - MYSQL_ROOT_PASSWORD: 数据库密码
# - JWT_SECRET: JWT 签名密钥 (生产环境必须更换)
```

### 2. 构建后端 JAR

```bash
cd admin-backend
mvn clean package -DskipTests
cd ..
```

### 3. 启动所有服务

```bash
cd docker
docker-compose up -d
```

**启动顺序：** MySQL (健康检查通过) → Redis → Backend → Nginx

### 4. 验证部署

```bash
# 检查服务状态
docker-compose ps

# 检查后端健康
curl http://localhost:8080/api/auth/info

# 检查前端
curl -I http://localhost

# 查看日志
docker-compose logs -f backend
```

### 5. 访问系统

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost |
| 后端 API | http://localhost:8080/api |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |

**默认账户：**
- 管理员: `admin` / `admin123`
- 普通用户: `user` / `user123`

### 6. 停止服务

```bash
cd docker
docker-compose down

# 停止并删除数据卷 (清空数据库)
docker-compose down -v
```

---

## 手动部署

适用于无法使用 Docker 的环境。

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE admin_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构和初始数据
mysql -u root -p admin_db < sql/schema.sql
mysql -u root -p admin_db < sql/data.sql
```

### 2. Redis

确保 Redis 服务已启动并可访问。

```bash
redis-cli ping
# 应返回 PONG
```

### 3. 后端部署

```bash
cd admin-backend

# 打包
mvn clean package -DskipTests

# 运行 (使用环境变量覆盖配置)
java -jar target/*.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url="jdbc:mysql://DB_HOST:3306/admin_db?useSSL=false&serverTimezone=Asia/Shanghai" \
  --spring.datasource.username=root \
  --spring.datasource.password=YOUR_DB_PASSWORD \
  --spring.data.redis.host=REDIS_HOST \
  --jwt.secret=YOUR_JWT_SECRET
```

**使用 systemd 管理 (推荐)：**

```ini
# /etc/systemd/system/admin-backend.service
[Unit]
Description=Admin Backend Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=appuser
WorkingDirectory=/opt/admin
ExecStart=/usr/bin/java -jar /opt/admin/app.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable admin-backend
sudo systemctl start admin-backend
```

### 4. 前端部署

```bash
cd admin-frontend
npm ci
npm run build
# 产物在 dist/ 目录
```

将 `dist/` 目录内容部署到 Nginx:

```bash
sudo cp -r dist/* /usr/share/nginx/html/
```

### 5. Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
    }
}
```

---

## 生产环境配置

### 安全配置清单

- [ ] **修改默认密码**: admin/user 账户密码必须更换
- [ ] **更换 JWT Secret**: 使用随机生成的 256 位密钥
- [ ] **数据库密码**: 使用强密码，禁止使用默认值
- [ ] **HTTPS**: 配置 SSL 证书，强制 HTTPS
- [ ] **防火墙**: 仅开放 80/443 端口，数据库/Redis 端口不对外暴露
- [ ] **日志**: 配置日志轮转，避免磁盘写满

### 生成 JWT Secret

```bash
# 生成随机 Base64 密钥
openssl rand -base64 48
```

### HTTPS 配置 (Nginx + Let's Encrypt)

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    # ... 其余配置同上
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

### 数据库备份

```bash
# 每日备份脚本
#!/bin/bash
BACKUP_DIR=/opt/backups/mysql
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u root -p"${MYSQL_ROOT_PASSWORD}" admin_db > "${BACKUP_DIR}/admin_db_${DATE}.sql"

# 保留最近 30 天备份
find ${BACKUP_DIR} -name "*.sql" -mtime +30 -delete
```

建议添加到 crontab:
```bash
# 每天凌晨 2 点备份
0 2 * * * /opt/scripts/backup-db.sh >> /var/log/backup.log 2>&1
```

---

## 健康检查与监控

### 服务健康检查

```bash
# 后端健康 (返回 401 表示服务正常但未认证)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/info
# 期望: 401

# MySQL
mysqladmin -h localhost -u root -p ping
# 期望: mysqld is alive

# Redis
redis-cli ping
# 期望: PONG

# Nginx
curl -s -o /dev/null -w "%{http_code}" http://localhost/
# 期望: 200
```

### Docker 容器监控

```bash
# 资源使用
docker stats

# 服务日志
docker-compose logs -f --tail=100 backend
docker-compose logs -f --tail=100 mysql
```

---

## 常见问题

### Q: 后端启动失败，提示数据库连接超时

**A:** 确认 MySQL 服务已启动，Docker Compose 部署时 MySQL 需要约 30 秒初始化:
```bash
docker-compose logs mysql | tail -20
```

### Q: 前端页面空白，控制台 404 错误

**A:** 检查 Nginx 配置中 `try_files` 是否正确配置为 SPA 模式:
```nginx
try_files $uri $uri/ /index.html;
```

### Q: Token 刷新失败，频繁跳转登录页

**A:** 检查 Redis 连接是否正常，Token 刷新依赖 Redis 存储:
```bash
redis-cli -h REDIS_HOST ping
```

### Q: Docker 构建前端镜像失败

**A:** 确认 `npm ci` 所需的 `package-lock.json` 已提交到仓库:
```bash
cd admin-frontend
npm install
git add package-lock.json
```

### Q: 数据库初始化未执行

**A:** Docker Compose 仅在首次创建数据卷时执行 SQL 初始化。如需重新初始化:
```bash
docker-compose down -v
docker-compose up -d
```
