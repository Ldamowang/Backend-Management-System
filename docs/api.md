# API 文档

> 完整的交互式 API 文档请访问 Swagger UI: `http://localhost:8080/swagger-ui.html`

## 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `/api` |
| 认证方式 | Bearer Token (JWT) |
| Content-Type | `application/json` |
| 字符编码 | UTF-8 |

## 统一响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "参数错误: 用户名不能为空",
  "data": null
}
```

## 错误码

| Code | 含义 | 说明 |
|------|------|------|
| 200 | 成功 | 请求处理成功 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未认证 | Token 缺失或已过期 |
| 403 | 无权限 | 当前用户无操作权限 |
| 404 | 未找到 | 请求的资源不存在 |
| 500 | 服务器错误 | 服务器内部异常 |

---

## 1. 认证模块 (Auth)

### 1.1 用户登录

```
POST /api/auth/login
```

**请求体：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**成功响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

### 1.2 刷新 Token

```
POST /api/auth/refresh
```

**请求体：**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**成功响应：** 同登录响应格式。

### 1.3 用户登出

```
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

**成功响应：**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

### 1.4 获取当前用户信息

```
GET /api/auth/info
Authorization: Bearer <accessToken>
```

**成功响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@iflytek.com",
    "phone": "13800000000",
    "avatar": null,
    "roles": ["admin"],
    "permissions": ["*:*:*"]
  }
}
```

---

## 2. 用户管理 (User)

> 所有接口需要 Bearer Token 认证

### 2.1 用户列表 (分页)

```
GET /api/system/users?page=1&size=10&username=admin&status=1
Authorization: Bearer <accessToken>
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页条数，默认 10 |
| username | string | 否 | 用户名 (模糊查询) |
| status | number | 否 | 状态 (1 启用 / 0 禁用) |

**权限：** `sys:user:list`

### 2.2 用户详情

```
GET /api/system/users/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:user:list`

### 2.3 创建用户

```
POST /api/system/users
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "newuser@iflytek.com",
  "phone": "13800000001",
  "status": 1,
  "roleIds": [2]
}
```

**权限：** `sys:user:add`

### 2.4 更新用户

```
PUT /api/system/users/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:user:edit`

### 2.5 删除用户

```
DELETE /api/system/users/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:user:delete`

### 2.6 重置密码

```
PATCH /api/system/users/{id}/password
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "newPassword": "newpassword123"
}
```

**权限：** `sys:user:edit`

---

## 3. 角色管理 (Role)

### 3.1 角色列表

```
GET /api/system/roles?page=1&size=10&roleName=管理
Authorization: Bearer <accessToken>
```

**权限：** `sys:role:list`

### 3.2 角色详情

```
GET /api/system/roles/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:role:list`

### 3.3 创建角色

```
POST /api/system/roles
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "roleName": "审计员",
  "roleKey": "auditor",
  "sort": 3,
  "status": 1,
  "remark": "审计角色",
  "menuIds": [1, 2, 3]
}
```

**权限：** `sys:role:add`

### 3.4 更新角色

```
PUT /api/system/roles/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:role:edit`

### 3.5 删除角色

```
DELETE /api/system/roles/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:role:delete`

---

## 4. 菜单管理 (Menu)

### 4.1 菜单树

```
GET /api/system/menus/tree
Authorization: Bearer <accessToken>
```

**权限：** `sys:menu:list`

**响应示例：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "menuName": "系统管理",
      "menuType": 1,
      "path": "/system",
      "icon": "Setting",
      "sort": 1,
      "children": [
        {
          "id": 2,
          "parentId": 1,
          "menuName": "用户管理",
          "menuType": 2,
          "path": "/system/user",
          "component": "system/user/index",
          "permission": "sys:user:list",
          "children": [ ... ]
        }
      ]
    }
  ]
}
```

### 4.2 创建菜单

```
POST /api/system/menus
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "parentId": 1,
  "menuName": "新页面",
  "menuType": 2,
  "path": "/system/new-page",
  "component": "system/new-page/index",
  "permission": "sys:newpage:list",
  "icon": "Document",
  "sort": 10,
  "visible": 1,
  "status": 1
}
```

**菜单类型 (menuType)：** 1=目录, 2=菜单, 3=按钮

**权限：** `sys:menu:add`

### 4.3 更新菜单

```
PUT /api/system/menus/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:menu:edit`

### 4.4 删除菜单

```
DELETE /api/system/menus/{id}
Authorization: Bearer <accessToken>
```

**权限：** `sys:menu:delete`

---

## 5. 系统配置 (Config)

### 5.1 配置列表

```
GET /api/system/configs?page=1&size=10&configKey=site
Authorization: Bearer <accessToken>
```

**权限：** `sys:config:list`

### 5.2 更新配置

```
PUT /api/system/configs/{id}
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "configValue": "新值",
  "remark": "修改说明"
}
```

**权限：** `sys:config:edit`

---

## 6. 日志管理 (Log)

### 6.1 操作日志列表

```
GET /api/system/logs/operation?page=1&size=10&module=用户管理
Authorization: Bearer <accessToken>
```

**权限：** `sys:log:list`

### 6.2 登录日志列表

```
GET /api/system/logs/login?page=1&size=10&username=admin
Authorization: Bearer <accessToken>
```

**权限：** `sys:log:list`

---

## 7. 个人中心 (Profile)

### 7.1 获取个人信息

```
GET /api/profile
Authorization: Bearer <accessToken>
```

### 7.2 更新个人信息

```
PUT /api/profile
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "nickname": "新昵称",
  "email": "new@iflytek.com",
  "phone": "13900000000"
}
```

### 7.3 修改密码

```
PUT /api/profile/password
Authorization: Bearer <accessToken>
```

**请求体：**
```json
{
  "oldPassword": "admin123",
  "newPassword": "newpassword123"
}
```

---

## 8. 仪表盘 (Dashboard)

### 8.1 获取统计数据

```
GET /api/dashboard/stats
Authorization: Bearer <accessToken>
```

**响应示例：**
```json
{
  "code": 200,
  "data": {
    "userCount": 10,
    "roleCount": 3,
    "menuCount": 25,
    "todayLoginCount": 5
  }
}
```

---

## 权限标识速查表

| 权限标识 | 说明 |
|----------|------|
| `sys:user:list` | 查看用户列表 |
| `sys:user:add` | 新增用户 |
| `sys:user:edit` | 编辑用户 |
| `sys:user:delete` | 删除用户 |
| `sys:role:list` | 查看角色列表 |
| `sys:role:add` | 新增角色 |
| `sys:role:edit` | 编辑角色 |
| `sys:role:delete` | 删除角色 |
| `sys:menu:list` | 查看菜单列表 |
| `sys:menu:add` | 新增菜单 |
| `sys:menu:edit` | 编辑菜单 |
| `sys:menu:delete` | 删除菜单 |
| `sys:config:list` | 查看系统配置 |
| `sys:config:edit` | 编辑配置 |
| `sys:log:list` | 查看日志 |
