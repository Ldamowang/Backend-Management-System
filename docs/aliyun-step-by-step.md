# 阿里云部署操作手册（从零到上线）

> 基于方案 B（ECS u2a 4核8G）+ Docker Compose 部署
> 预计操作时间：2-3 小时（不含备案等待时间）

---

## 目录

- [第一步：购买阿里云 ECS 服务器](#第一步购买阿里云-ecs-服务器)
- [第二步：注册域名](#第二步注册域名)
- [第三步：ICP 备案](#第三步icp-备案)
- [第四步：连接服务器 & 初始化环境](#第四步连接服务器--初始化环境)
- [第五步：安装 Docker & Docker Compose](#第五步安装-docker--docker-compose)
- [第六步：上传项目代码](#第六步上传项目代码)
- [第七步：本地构建项目](#第七步本地构建项目)
- [第八步：配置环境变量](#第八步配置环境变量)
- [第九步：启动服务](#第九步启动服务)
- [第十步：配置域名解析](#第十步配置域名解析)
- [第十一步：配置 HTTPS](#第十一步配置-https)
- [第十二步：验证部署](#第十二步验证部署)
- [第十三步：安全加固](#第十三步安全加固)
- [第十四步：配置自动备份](#第十四步配置自动备份)
- [附录：日常运维命令](#附录日常运维命令)

---

## 第一步：购买阿里云 ECS 服务器

### 1.1 登录阿里云

1. 打开浏览器，访问 https://www.aliyun.com
2. 点击右上角「登录」，使用支付宝/手机号注册登录
3. 完成实名认证（个人认证需身份证，企业认证需营业执照）

### 1.2 进入 ECS 购买页

1. 登录后，在顶部搜索栏搜索「ECS」
2. 点击「云服务器 ECS」进入产品页
3. 点击「立即购买」或查看活动页面获取优惠

### 1.3 选择配置

按以下参数逐项选择：

| 配置项 | 选择值 | 截图位置 |
|--------|--------|---------|
| **付费模式** | 包年包月 | 页面顶部第一行 |
| **购买时长** | 1 年 | 页面顶部（年付更划算） |
| **地域** | 华北 2（北京）或 华东 1（杭州） | 第二行 |
| **可用区** | 随机分配 | 第二行 |
| **实例规格** | 通用算力型 u2a → ecs.u2a.xlarge (4核8G) | 实例规格选择区 |
| **镜像** | Alibaba Cloud Linux 3.2104 64位 | 镜像选择区 |
| **系统盘** | ESSD 云盘 40G PL0 | 存储配置区 |
| **数据盘** | 新增数据盘 → ESSD 100G PL0 | 存储配置区，点「增加一块数据盘」 |

> 如果找不到 u2a 规格，搜索「通用算力」或直接在活动页下单。

### 1.4 网络与安全组配置

| 配置项 | 选择值 |
|--------|--------|
| **网络** | 默认 VPC（自动创建） |
| **公网 IP** | 分配 IPv4 公网 IP |
| **带宽计费** | 按固定带宽 |
| **带宽值** | 5 Mbps |
| **安全组** | 新建安全组（后面会配置规则） |

### 1.5 系统配置

| 配置项 | 选择值 |
|--------|--------|
| **登录凭证** | 自定义密码（记住！）或 SSH 密钥对（推荐） |
| **实例名称** | admin-server |
| **主机名** | admin-server |

> **推荐使用 SSH 密钥对**：更安全，不用记密码。选择「密钥对」→「创建密钥对」，下载 .pem 文件保存好。

### 1.6 确认下单

1. 检查配置和价格
2. 勾选「服务协议」
3. 点击「确认下单」→ 完成支付

### 1.7 配置安全组规则

购买完成后，需要配置安全组：

1. 进入 ECS 控制台 → 实例列表 → 点击你的实例
2. 左侧菜单 → 「安全组」 → 点击安全组 ID
3. 点击「手动添加」，依次添加以下规则：

| 优先级 | 授权策略 | 协议 | 端口范围 | 授权对象 | 描述 |
|--------|---------|------|---------|---------|------|
| 1 | 允许 | TCP | 22/22 | 你的公网 IP/32 | SSH管理 |
| 1 | 允许 | TCP | 80/80 | 0.0.0.0/0 | HTTP |
| 1 | 允许 | TCP | 443/443 | 0.0.0.0/0 | HTTPS |

> **重要**：SSH 的授权对象设置为你的 IP（通过 https://myip.com 查询），不要设为 0.0.0.0/0。

### 1.8 记录服务器信息

购买完成后，在 ECS 控制台记录以下信息（后面都会用到）：

```
服务器公网 IP：____________
服务器内网 IP：____________
登录用户名：root
登录密码/密钥文件：____________
```

---

## 第二步：注册域名

### 2.1 购买域名

1. 访问 https://wanwang.aliyun.com/domain
2. 在搜索框输入你想要的域名（如 `admin.yourdomain.com`）
3. 选择 `.com`（85 元/年）或 `.cn`（38 元/年）
4. 加入购物车 → 确认订单 → 支付

### 2.2 域名实名认证

1. 支付后，进入 域名控制台
2. 点击你的域名 → 「域名持有者实名认证」
3. 上传身份证/营业执照照片
4. 等待审核（通常 1-3 个工作日）

> **注意**：域名不做实名认证会被暂停解析，无法使用。

---

## 第三步：ICP 备案

> **这是必须步骤**：国内服务器部署网站必须完成 ICP 备案，否则 80/443 端口不可用。
> 备案期间可以先用 IP + 非标端口（如 8080）进行测试。

### 3.1 准备材料

| 材料 | 个人备案 | 企业备案 |
|------|---------|---------|
| 身份证 | 正反面照片 | 法人身份证正反面 |
| 营业执照 | 不需要 | 需要 |
| 域名证书 | 在域名控制台下载 | 在域名控制台下载 |
| 手机号 | 本人实名手机号 | 法人手机号 |
| 应急手机 | 另一个手机号 | 另一个手机号 |
| 邮箱 | 个人邮箱 | 企业邮箱 |

### 3.2 提交备案

1. 访问 https://beian.aliyun.com
2. 点击「开始备案」
3. 填写主体信息（个人/企业信息）
4. 填写网站信息：
   - 网站名称：如「XX管理系统」
   - 域名：你注册的域名
   - 网站内容：企业管理/后台系统
5. 上传材料（身份证、营业执照等）
6. 进行人脸核验（手机支付宝扫码完成）
7. 提交阿里云初审

### 3.3 备案流程与时间

```
提交 → 阿里云初审（1个工作日）
     → 工信部审核（5-15个工作日）
     → 备案通过，发放 ICP 备案号
```

> **在等待备案期间**，你可以先完成服务器环境配置（第四步~第九步），使用 `http://公网IP:8080` 直接测试后端。

---

## 第四步：连接服务器 & 初始化环境

### 4.1 通过 SSH 连接服务器

打开你 Mac 的终端：

```bash
# 方式一：密码登录
ssh root@你的服务器公网IP

# 方式二：密钥登录（推荐）
chmod 400 ~/Downloads/your-key.pem
ssh -i ~/Downloads/your-key.pem root@你的服务器公网IP
```

看到类似以下提示表示连接成功：

```
Welcome to Alibaba Cloud Elastic Compute Service !
[root@admin-server ~]#
```

### 4.2 更新系统

```bash
# 更新系统包
yum update -y

# 安装常用工具
yum install -y git vim wget curl unzip
```

### 4.3 配置时区

```bash
timedatectl set-timezone Asia/Shanghai
timedatectl   # 确认显示 CST 时区
```

### 4.4 格式化并挂载数据盘

> 数据盘默认没有格式化，需要手动操作。

```bash
# 1. 查看磁盘，确认数据盘设备名（通常是 /dev/vdb）
fdisk -l

# 你会看到类似输出：
# Disk /dev/vda: 40 GiB    ← 这是系统盘
# Disk /dev/vdb: 100 GiB   ← 这是数据盘
```

```bash
# 2. 格式化数据盘（⚠️ 只在首次执行，会清除数据）
mkfs.ext4 /dev/vdb
```

```bash
# 3. 创建挂载目录
mkdir -p /data
```

```bash
# 4. 挂载
mount /dev/vdb /data
```

```bash
# 5. 设置开机自动挂载
echo '/dev/vdb /data ext4 defaults 0 0' >> /etc/fstab
```

```bash
# 6. 验证挂载
df -h | grep /data
# 应显示：/dev/vdb  98G  ...  /data
```

### 4.5 创建项目目录

```bash
mkdir -p /data/projects
mkdir -p /data/backup/mysql
mkdir -p /data/logs
```

---

## 第五步：安装 Docker & Docker Compose

### 5.1 安装 Docker

```bash
# 使用阿里云镜像安装 Docker（国内速度快）
curl -fsSL https://get.docker.com | sh -s -- --mirror Aliyun
```

```bash
# 启动 Docker 并设为开机自启
systemctl start docker
systemctl enable docker
```

```bash
# 验证安装
docker --version
# 输出类似：Docker version 27.x.x
```

### 5.2 配置 Docker 镜像加速

> 国内拉取 Docker 镜像很慢，需要配置加速器。

```bash
mkdir -p /etc/docker

cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://docker.mirrors.ustc.edu.cn"
  ],
  "data-root": "/data/docker",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "50m",
    "max-file": "3"
  }
}
EOF
```

```bash
# 重启 Docker 使配置生效
systemctl daemon-reload
systemctl restart docker
```

> **说明**：`data-root` 设为 `/data/docker`，让 Docker 数据存储在数据盘上，避免系统盘空间不足。

### 5.3 安装 Docker Compose

```bash
# 下载最新版 Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
```

```bash
# 添加执行权限
chmod +x /usr/local/bin/docker-compose
```

```bash
# 验证安装
docker-compose --version
# 输出类似：Docker Compose version v2.x.x
```

> 如果 GitHub 下载慢，也可以用 `docker compose`（Docker 新版内置的 compose 插件），用法一样，只是把 `docker-compose` 替换为 `docker compose`。

---

## 第六步：上传项目代码

有两种方式，选择适合你的一种：

### 方式一：Git 拉取（推荐）

如果代码已经推送到 Git 仓库：

```bash
cd /data/projects

# 从 GitHub/Gitee 拉取
git clone https://github.com/你的用户名/你的仓库.git code

# 或从 Gitee（国内更快）
git clone https://gitee.com/你的用户名/你的仓库.git code
```

### 方式二：SCP 上传

从你的 Mac 本地上传：

```bash
# 在你的 Mac 终端执行（不是服务器）

# 密码方式
scp -r /Users/yili/Desktop/iflytek/test-demo/code root@你的服务器IP:/data/projects/

# 密钥方式
scp -i ~/Downloads/your-key.pem -r /Users/yili/Desktop/iflytek/test-demo/code root@你的服务器IP:/data/projects/
```

### 确认文件结构

```bash
# 在服务器上检查
ls /data/projects/code/

# 应该看到：
# admin-backend/  admin-frontend/  docker/  sql/  docs/  ...
```

---

## 第七步：本地构建项目

> 需要在服务器上构建后端 JAR 包和前端产物。

### 7.1 安装 JDK 17（后端构建）

```bash
yum install -y java-17-openjdk java-17-openjdk-devel
```

```bash
# 验证
java -version
# 输出：openjdk version "17.x.x"
```

### 7.2 安装 Maven（后端构建）

```bash
# 下载 Maven
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz

# 解压安装
tar -xzf apache-maven-3.9.9-bin.tar.gz -C /opt/
ln -s /opt/apache-maven-3.9.9 /opt/maven

# 配置环境变量
cat >> /etc/profile.d/maven.sh << 'EOF'
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
EOF

source /etc/profile.d/maven.sh
```

```bash
# 验证
mvn -version
```

### 7.3 配置 Maven 阿里云镜像（加速下载）

```bash
mkdir -p ~/.m2

cat > ~/.m2/settings.xml << 'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven Mirror</name>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
</settings>
EOF
```

### 7.4 构建后端

```bash
cd /data/projects/code/admin-backend

# 打包（跳过测试，服务器上没有数据库）
mvn clean package -DskipTests
```

```bash
# 验证 JAR 包生成
ls -lh target/*.jar
# 应看到：admin-backend-1.0.0.jar 或 admin-1.0.0.jar
```

> 首次构建需要下载依赖，可能需要 5-10 分钟。

### 7.5 安装 Node.js 20（前端构建）

```bash
# 使用 NodeSource 安装 Node.js 20 LTS
curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
yum install -y nodejs
```

```bash
# 验证
node -v   # v20.x.x
npm -v    # 10.x.x
```

### 7.6 配置 npm 淘宝镜像（加速下载）

```bash
npm config set registry https://registry.npmmirror.com
```

### 7.7 构建前端

```bash
cd /data/projects/code/admin-frontend

# 安装依赖
npm ci

# 构建生产版本
npm run build
```

```bash
# 验证构建产物
ls dist/
# 应看到：index.html  assets/  ...
```

---

## 第八步：配置环境变量

### 8.1 创建 .env 文件

```bash
cd /data/projects/code/docker
cp .env.example .env
```

### 8.2 生成安全密码

```bash
# 生成 MySQL 密码
echo "MySQL 密码: $(openssl rand -base64 24)"

# 生成 Redis 密码
echo "Redis 密码: $(openssl rand -base64 24)"

# 生成 JWT 密钥
echo "JWT 密钥: $(openssl rand -base64 48)"
```

记录输出的三个随机字符串。

### 8.3 编辑 .env 文件

```bash
vim .env
```

填入以下内容（将 `<xxx>` 替换为上一步生成的密码）：

```bash
# MySQL
MYSQL_ROOT_PASSWORD=<上面生成的MySQL密码>
MYSQL_DATABASE=admin_db

# Redis
REDIS_PASSWORD=<上面生成的Redis密码>

# Spring Boot
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<与MYSQL_ROOT_PASSWORD相同>

# JWT
JWT_SECRET=<上面生成的JWT密钥>
```

> **vim 基本操作**：按 `i` 进入编辑模式，编辑完按 `Esc`，输入 `:wq` 保存退出。

### 8.4 验证 .env 文件

```bash
cat .env
# 确认所有 <...> 占位符都已替换为实际值
# 确认没有空值
```

---

## 第九步：启动服务

### 9.1 启动 Docker Compose

```bash
cd /data/projects/code/docker

# 构建并启动所有服务（后台运行）
docker-compose up -d --build
```

> 首次启动需要拉取 MySQL、Redis、Nginx 镜像以及构建后端镜像，可能需要 5-15 分钟。

### 9.2 查看启动状态

```bash
docker-compose ps
```

预期输出（所有服务 State 应为 `Up` 或 `Up (healthy)`）：

```
NAME             IMAGE            STATUS                   PORTS
admin-mysql      mysql:8.0        Up (healthy)             127.0.0.1:3306->3306/tcp
admin-redis      redis:7-alpine   Up (healthy)             127.0.0.1:6379->6379/tcp
admin-backend    docker-backend   Up                       8080/tcp
admin-nginx      nginx:alpine     Up                       0.0.0.0:80->80/tcp
```

### 9.3 查看日志（如果有问题）

```bash
# 查看所有服务日志
docker-compose logs

# 只看后端日志（最常需要）
docker-compose logs -f backend

# 只看最近 100 行
docker-compose logs --tail 100 backend
```

### 9.4 常见启动问题排查

**问题 1：backend 一直 restarting**
```bash
# 查看后端日志
docker-compose logs backend
# 常见原因：MySQL 还没准备好，等 30 秒重试
# 或：JAR 包没正确构建，检查第七步
```

**问题 2：mysql 启动失败**
```bash
docker-compose logs mysql
# 常见原因：.env 中 MYSQL_ROOT_PASSWORD 为空
# 解决：检查 .env 文件
```

**问题 3：端口被占用**
```bash
# 检查 80 端口是否被占用
ss -tlnp | grep :80
# 如果被占用，停止占用进程或修改 docker-compose.yml 中的端口映射
```

### 9.5 快速验证

```bash
# 测试后端 API
curl http://localhost:8080/api/auth/info

# 测试 Nginx 代理（前端页面）
curl -I http://localhost

# 测试登录接口
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

此时可以在浏览器访问 `http://你的服务器公网IP` 查看前端页面。

> 如果域名还没备案，暂时只能通过 IP 访问。

---

## 第十步：配置域名解析

> 备案通过后才能进行此步骤。

### 10.1 添加 DNS 解析记录

1. 登录阿里云控制台
2. 搜索「云解析 DNS」→ 进入控制台
3. 找到你的域名 → 点击「解析设置」
4. 点击「添加记录」：

| 记录类型 | 主机记录 | 记录值 | TTL |
|---------|---------|--------|-----|
| A | @ | 你的服务器公网 IP | 10 分钟 |
| A | www | 你的服务器公网 IP | 10 分钟 |

> `@` 表示直接访问 `yourdomain.com`，`www` 表示访问 `www.yourdomain.com`。
> 如果只需要子域名如 `admin.yourdomain.com`，主机记录填 `admin`。

### 10.2 验证解析

```bash
# 在你的 Mac 或服务器上执行
ping yourdomain.com
# 应该显示你的服务器 IP
```

### 10.3 修改 Nginx 配置中的 server_name

```bash
vim /data/projects/code/docker/nginx/nginx.conf
```

找到 `server_name localhost;`，改为：

```nginx
server_name yourdomain.com www.yourdomain.com;
```

```bash
# 重启 Nginx
cd /data/projects/code/docker
docker-compose restart nginx
```

---

## 第十一步：配置 HTTPS

### 11.1 安装 Certbot

```bash
# Alibaba Cloud Linux / CentOS
yum install -y epel-release
yum install -y certbot
```

### 11.2 停止 Nginx（Certbot 需要 80 端口）

```bash
cd /data/projects/code/docker
docker-compose stop nginx
```

### 11.3 申请 SSL 证书

```bash
certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

按提示：
1. 输入你的邮箱
2. 同意协议，输入 `Y`
3. 是否分享邮箱，输入 `N`

成功后证书保存在：

```
证书：/etc/letsencrypt/live/yourdomain.com/fullchain.pem
私钥：/etc/letsencrypt/live/yourdomain.com/privkey.pem
```

### 11.4 修改 Nginx 配置支持 HTTPS

```bash
vim /data/projects/code/docker/nginx/nginx.conf
```

将整个 `server` 块替换为以下内容：

```nginx
    # HTTP → HTTPS 自动跳转
    server {
        listen 80;
        server_name yourdomain.com www.yourdomain.com;
        return 301 https://$host$request_uri;
    }

    # HTTPS 主配置
    server {
        listen 443 ssl http2;
        server_name yourdomain.com www.yourdomain.com;

        ssl_certificate     /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

        # 安全响应头
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header X-XSS-Protection "0" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;

        # 前端静态文件
        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }

        # API 反向代理
        location /api/ {
            limit_req zone=api_limit burst=40 nodelay;
            proxy_pass http://backend:8080/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            client_max_body_size 10m;
        }

        # 登录接口额外限流
        location /api/auth/login {
            limit_req zone=login_limit burst=3 nodelay;
            proxy_pass http://backend:8080/api/auth/login;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 禁止访问隐藏文件
        location ~ /\. {
            deny all;
            return 404;
        }
    }
```

### 11.5 修改 docker-compose.yml 增加 HTTPS 支持

```bash
vim /data/projects/code/docker/docker-compose.yml
```

修改 nginx 服务部分，增加 443 端口和 SSL 证书挂载：

```yaml
  nginx:
    image: nginx:alpine
    container_name: admin-nginx
    ports:
      - "80:80"
      - "443:443"                                          # 新增
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ../admin-frontend/dist:/usr/share/nginx/html
      - /etc/letsencrypt/live/yourdomain.com/fullchain.pem:/etc/nginx/ssl/fullchain.pem:ro   # 新增
      - /etc/letsencrypt/live/yourdomain.com/privkey.pem:/etc/nginx/ssl/privkey.pem:ro       # 新增
    depends_on:
      - backend
```

### 11.6 重启 Nginx

```bash
cd /data/projects/code/docker
docker-compose up -d nginx
```

### 11.7 配置自动续期

Let's Encrypt 证书有效期 90 天，需要自动续期：

```bash
# 创建续期脚本
cat > /data/scripts/renew-cert.sh << 'EOF'
#!/bin/bash
cd /data/projects/code/docker
docker-compose stop nginx
certbot renew --quiet
docker-compose start nginx
EOF

chmod +x /data/scripts/renew-cert.sh
```

```bash
# 每周一凌晨 3 点自动检查续期
mkdir -p /data/scripts
(crontab -l 2>/dev/null; echo "0 3 * * 1 /data/scripts/renew-cert.sh >> /data/logs/certbot.log 2>&1") | crontab -
```

---

## 第十二步：验证部署

### 12.1 服务状态检查

```bash
cd /data/projects/code/docker

# 所有容器状态
docker-compose ps

# 期望：所有容器 Up 且 healthy
```

### 12.2 逐项验证

```bash
# 1. MySQL 连接正常
docker exec admin-mysql mysqladmin ping -u root -p$MYSQL_ROOT_PASSWORD
# 期望输出：mysqld is alive

# 2. Redis 连接正常
docker exec admin-redis redis-cli -a $REDIS_PASSWORD ping
# 期望输出：PONG

# 3. 后端 API 正常
curl http://localhost:8080/api/auth/info
# 期望：返回 JSON

# 4. 前端页面正常
curl -I https://yourdomain.com
# 期望：HTTP/2 200

# 5. 登录功能正常
curl -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 期望：返回包含 accessToken 的 JSON
```

### 12.3 浏览器验证

1. 打开浏览器，访问 `https://yourdomain.com`
2. 看到登录页面
3. 使用 `admin` / `admin123` 登录
4. 确认功能正常（菜单、列表、操作等）
5. 检查浏览器地址栏是否显示安全锁图标（HTTPS）

---

## 第十三步：安全加固

### 13.1 立即修改默认密码

登录系统后，在「个人中心」修改 admin 和 user 账户的密码。

### 13.2 配置后端生产环境变量

在 `docker-compose.yml` 的 backend 环境变量中添加：

```yaml
    environment:
      # ... 已有变量 ...
      SWAGGER_ENABLED: "false"              # 关闭 API 文档
      LOG_LEVEL: "info"                     # 降低日志级别
      CORS_ALLOWED_ORIGINS: "https://yourdomain.com"   # 限制跨域
```

```bash
# 重启后端使配置生效
docker-compose up -d backend
```

### 13.3 禁止 root SSH 登录（可选但推荐）

```bash
# 1. 先创建普通用户
useradd -m -s /bin/bash deploy
passwd deploy    # 设置密码

# 2. 赋予 sudo 和 docker 权限
usermod -aG wheel deploy
usermod -aG docker deploy

# 3. 测试用新用户登录（新开一个终端窗口测试，不要关闭当前连接！）
ssh deploy@你的服务器IP

# 4. 确认新用户可以正常使用后，再禁止 root 登录
sudo vim /etc/ssh/sshd_config
# 找到 PermitRootLogin，改为：
# PermitRootLogin no

sudo systemctl restart sshd
```

### 13.4 安装 fail2ban（防暴力破解 SSH）

```bash
yum install -y epel-release
yum install -y fail2ban

# 启用并启动
systemctl enable fail2ban
systemctl start fail2ban
```

---

## 第十四步：配置自动备份

### 14.1 创建备份脚本

```bash
cat > /data/scripts/backup-mysql.sh << 'SCRIPT'
#!/bin/bash

# 配置
BACKUP_DIR="/data/backup/mysql"
KEEP_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/admin_db_${DATE}.sql.gz"

# 从 .env 读取密码
source /data/projects/code/docker/.env

# 执行备份
docker exec admin-mysql mysqldump \
  -u root \
  -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  admin_db | gzip > "${BACKUP_FILE}"

# 检查备份是否成功
if [ $? -eq 0 ] && [ -s "${BACKUP_FILE}" ]; then
    echo "[$(date)] 备份成功: ${BACKUP_FILE} ($(du -h ${BACKUP_FILE} | cut -f1))"
else
    echo "[$(date)] 备份失败!"
    exit 1
fi

# 清理过期备份
find ${BACKUP_DIR} -name "admin_db_*.sql.gz" -mtime +${KEEP_DAYS} -delete
echo "[$(date)] 已清理 ${KEEP_DAYS} 天前的备份"
SCRIPT

chmod +x /data/scripts/backup-mysql.sh
```

### 14.2 测试备份

```bash
/data/scripts/backup-mysql.sh

# 查看备份文件
ls -lh /data/backup/mysql/
```

### 14.3 设置定时任务

```bash
# 每天凌晨 2:00 自动备份
(crontab -l 2>/dev/null; echo "0 2 * * * /data/scripts/backup-mysql.sh >> /data/logs/backup.log 2>&1") | crontab -

# 查看定时任务
crontab -l
```

### 14.4 备份恢复方法

如果需要恢复备份：

```bash
# 解压备份文件
gunzip < /data/backup/mysql/admin_db_20260401_020000.sql.gz | \
  docker exec -i admin-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" admin_db
```

---

## 附录：日常运维命令

### A. 服务管理

```bash
cd /data/projects/code/docker

# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启某个服务
docker-compose restart backend
docker-compose restart nginx

# 查看服务状态
docker-compose ps

# 查看日志（实时跟踪）
docker-compose logs -f backend
docker-compose logs -f --tail 100 nginx
```

### B. 代码更新部署

```bash
# 1. 拉取最新代码
cd /data/projects/code
git pull origin main

# 2. 重新构建后端
cd admin-backend
mvn clean package -DskipTests

# 3. 重新构建前端
cd ../admin-frontend
npm ci
npm run build

# 4. 重启服务
cd ../docker
docker-compose up -d --build
```

### C. 磁盘空间检查

```bash
# 查看磁盘使用
df -h

# 查看 Docker 占用空间
docker system df

# 清理无用的 Docker 镜像和缓存
docker system prune -a --volumes
# ⚠️ 这会删除所有未使用的镜像和卷，谨慎操作
```

### D. 进入容器调试

```bash
# 进入后端容器
docker exec -it admin-backend sh

# 进入 MySQL 容器
docker exec -it admin-mysql mysql -u root -p

# 进入 Redis 容器
docker exec -it admin-redis redis-cli -a ${REDIS_PASSWORD}
```

### E. 服务器资源监控

```bash
# CPU 和内存使用
top

# 内存详情
free -h

# Docker 容器资源占用
docker stats
```

---

## 快速参考卡片

```
┌──────────────────────────────────────────────────┐
│              部署快速参考                          │
├──────────────────────────────────────────────────┤
│ 项目目录：/data/projects/code                      │
│ Docker 配置：/data/projects/code/docker             │
│ 环境变量：/data/projects/code/docker/.env            │
│ Nginx 配置：/data/projects/code/docker/nginx/       │
│ MySQL 数据：Docker volume (mysql-data)              │
│ 备份目录：/data/backup/mysql/                       │
│ 日志目录：/data/logs/                              │
│ SSL 证书：/etc/letsencrypt/live/yourdomain.com/    │
│                                                  │
│ 启动：docker-compose up -d                        │
│ 停止：docker-compose down                         │
│ 日志：docker-compose logs -f backend              │
│ 状态：docker-compose ps                           │
│ 备份：/data/scripts/backup-mysql.sh               │
└──────────────────────────────────────────────────┘
```
