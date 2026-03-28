# RBAC 权限矩阵

> 基于 `sys_menu` 表定义，权限标识格式：`module:entity:action`

## 角色定义

| 角色 ID | 角色名称 | 角色标识 | 说明 |
|---------|---------|---------|------|
| 1 | 超级管理员 | admin | 拥有系统所有权限 |
| 2 | 普通用户 | user | 基础查看权限 |

## 菜单权限对照表

### 一级菜单

| 菜单 ID | 菜单名称 | 路径 | 类型 | admin | user |
|---------|---------|------|------|:-----:|:----:|
| 1 | 仪表盘 | /dashboard | 页面 | ✅ | ✅ |
| 2 | 系统管理 | /system | 目录 | ✅ | ✅ |

### 二级菜单

| 菜单 ID | 菜单名称 | 路径 | 权限标识 | admin | user |
|---------|---------|------|---------|:-----:|:----:|
| 201 | 用户管理 | /system/user | sys:user:list | ✅ | ✅ |
| 202 | 角色管理 | /system/role | sys:role:list | ✅ | ❌ |
| 203 | 菜单管理 | /system/menu | sys:menu:list | ✅ | ❌ |
| 204 | 系统配置 | /system/config | sys:config:list | ✅ | ✅ |
| 205 | 日志管理 | /system/log | — | ✅ | ❌ |

### 三级菜单（日志子菜单）

| 菜单 ID | 菜单名称 | 路径 | 权限标识 | admin | user |
|---------|---------|------|---------|:-----:|:----:|
| 2051 | 操作日志 | /system/log/operation | sys:log:list | ✅ | ❌ |
| 2052 | 登录日志 | /system/log/login | sys:log:list | ✅ | ❌ |

### 按钮权限

| 菜单 ID | 按钮名称 | 所属模块 | 权限标识 | admin | user |
|---------|---------|---------|---------|:-----:|:----:|
| 2011 | 新增用户 | 用户管理 | sys:user:add | ✅ | ❌ |
| 2012 | 编辑用户 | 用户管理 | sys:user:edit | ✅ | ❌ |
| 2013 | 删除用户 | 用户管理 | sys:user:delete | ✅ | ❌ |
| 2021 | 新增角色 | 角色管理 | sys:role:add | ✅ | ❌ |
| 2022 | 编辑角色 | 角色管理 | sys:role:edit | ✅ | ❌ |
| 2023 | 删除角色 | 角色管理 | sys:role:delete | ✅ | ❌ |
| 2031 | 新增菜单 | 菜单管理 | sys:menu:add | ✅ | ❌ |
| 2032 | 编辑菜单 | 菜单管理 | sys:menu:edit | ✅ | ❌ |
| 2033 | 删除菜单 | 菜单管理 | sys:menu:delete | ✅ | ❌ |
| 2041 | 编辑配置 | 系统配置 | sys:config:edit | ✅ | ❌ |

## 权限标识完整清单

| 权限标识 | 说明 | 对应后端接口 |
|---------|------|-------------|
| sys:user:list | 查看用户列表 | GET /api/users |
| sys:user:add | 新增用户 | POST /api/users |
| sys:user:edit | 编辑用户 | PUT /api/users/{id} |
| sys:user:delete | 删除用户 | DELETE /api/users/{id} |
| sys:role:list | 查看角色列表 | GET /api/roles |
| sys:role:add | 新增角色 | POST /api/roles |
| sys:role:edit | 编辑角色 | PUT /api/roles/{id} |
| sys:role:delete | 删除角色 | DELETE /api/roles/{id} |
| sys:menu:list | 查看菜单列表 | GET /api/menus |
| sys:menu:add | 新增菜单 | POST /api/menus |
| sys:menu:edit | 编辑菜单 | PUT /api/menus/{id} |
| sys:menu:delete | 删除菜单 | DELETE /api/menus/{id} |
| sys:config:list | 查看系统配置 | GET /api/configs |
| sys:config:edit | 编辑系统配置 | PUT /api/configs/{id} |
| sys:log:list | 查看日志 | GET /api/logs/* |

## 新增角色指南

添加新角色时需要：
1. 在 `sys_role` 表新增角色记录
2. 在 `sys_role_menu` 表配置该角色的菜单和按钮权限
3. 后端无需改动（`@PreAuthorize` 基于权限标识自动校验）
4. 前端无需改动（`v-permission` 指令基于权限标识自动控制）
