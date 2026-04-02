# P1 体验基础 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后台管理系统新增 4 个前端体验功能：国际化 i18n、暗黑模式+主题换肤、多标签页导航、全局搜索。

**Architecture:** 全局状态驱动。所有功能共享 Pinia Store 体系，统一持久化到 localStorage。增强现有 `useAppStore` 而非新建 Store（主题相关），新建 `useTabStore`（标签页）。i18n 使用 `vue-i18n`，标签页拖拽使用 `vuedraggable`。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Pinia + vue-i18n + vuedraggable + @vueuse/core

**设计文档:** `docs/superpowers/specs/2026-04-01-p1-ux-foundation-design.md`

---

## 文件结构总览

### 新增文件

```
src/
├── locales/
│   ├── index.ts                          # i18n 入口，语言注册+懒加载
│   ├── README.md                         # 语言包规范说明
│   ├── zh-CN/
│   │   ├── common.json                   # 通用文本
│   │   ├── menu.json                     # 菜单名称
│   │   ├── login.json                    # 登录页
│   │   ├── dashboard.json                # 仪表盘
│   │   ├── system.json                   # 系统管理模块
│   │   └── profile.json                  # 个人中心
│   └── en-US/
│       └── (同结构)
├── stores/modules/tab.ts                 # 标签页 Store
├── stores/__tests__/tab.test.ts          # 标签页 Store 测试
├── stores/__tests__/app-theme.test.ts    # 主题增强测试
├── composables/useGlobalSearch.ts        # 全局搜索逻辑
├── composables/__tests__/useGlobalSearch.test.ts
├── components/Layout/TabBar.vue          # 标签栏组件
├── components/Layout/TabContextMenu.vue  # 标签右键菜单
├── components/SearchDialog/index.vue     # 搜索弹窗
├── components/SearchDialog/SearchResultItem.vue # 搜索结果项
└── assets/styles/dark.scss               # 暗黑模式样式
```

### 修改文件

```
src/
├── main.ts                               # 注册 i18n 插件
├── plugins/element-plus.ts               # 加入 locale 配置
├── stores/modules/app.ts                 # 增强主题：auto 模式 + 衍生色
├── components/Layout/index.vue           # 插入 TabBar + keep-alive
├── components/Layout/Header.vue          # 搜索图标 + 语言切换
├── components/Layout/SettingsDrawer.vue  # 取色器 + auto 模式
├── components/Layout/Sidebar.vue         # i18n 菜单文本
├── components/Layout/Breadcrumb.vue      # i18n 面包屑
├── router/guard.ts                       # 标签页联动
├── assets/styles/variables.scss          # 暗黑模式变量
├── assets/styles/global.scss             # 暗黑模式全局样式
└── views/**/*.vue                        # 所有页面 i18n 化
```

---

## Task 1: 安装依赖

**Files:**
- Modify: `admin-frontend/package.json`

- [ ] **Step 1: 安装 vue-i18n 和 vuedraggable**

```bash
cd admin-frontend
npm install vue-i18n@^9.14.0 vuedraggable@^4.1.0 sortablejs@^1.15.0
npm install -D @types/sortablejs
```

- [ ] **Step 2: 验证安装成功**

```bash
cd admin-frontend && npm ls vue-i18n vuedraggable
```

Expected: 显示已安装的版本号，无 UNMET PEER DEPENDENCY 错误。

- [ ] **Step 3: Commit**

```bash
git add admin-frontend/package.json admin-frontend/package-lock.json
git commit -m "chore: add vue-i18n and vuedraggable dependencies"
```

---

## Task 2: i18n 基础设施 — 中文语言包

**Files:**
- Create: `src/locales/zh-CN/common.json`
- Create: `src/locales/zh-CN/menu.json`
- Create: `src/locales/zh-CN/login.json`
- Create: `src/locales/zh-CN/dashboard.json`
- Create: `src/locales/zh-CN/system.json`
- Create: `src/locales/zh-CN/profile.json`

- [ ] **Step 1: 创建 zh-CN/common.json**

```json
{
  "action": {
    "confirm": "确定",
    "cancel": "取消",
    "add": "新增",
    "edit": "编辑",
    "delete": "删除",
    "search": "搜索",
    "reset": "重置",
    "export": "导出",
    "import": "导入",
    "refresh": "刷新",
    "save": "保存",
    "back": "返回",
    "close": "关闭",
    "submit": "提交",
    "upload": "上传",
    "download": "下载"
  },
  "label": {
    "status": "状态",
    "operation": "操作",
    "remark": "备注",
    "createTime": "创建时间",
    "updateTime": "更新时间",
    "createdBy": "创建人",
    "enabled": "启用",
    "disabled": "禁用",
    "yes": "是",
    "no": "否",
    "all": "全部",
    "total": "共 {total} 条"
  },
  "message": {
    "confirmDelete": "确定要删除吗？此操作不可恢复。",
    "deleteSuccess": "删除成功",
    "saveSuccess": "保存成功",
    "operationSuccess": "操作成功",
    "operationFailed": "操作失败",
    "loadingFailed": "数据加载失败",
    "networkError": "网络错误，请稍后重试",
    "tip": "提示",
    "warning": "警告"
  },
  "pagination": {
    "total": "共 {total} 条",
    "pageSize": "{size} 条/页"
  },
  "settings": {
    "title": "系统设置",
    "themeMode": "主题模式",
    "light": "浅色",
    "dark": "深色",
    "auto": "跟随系统",
    "themeColor": "主题色",
    "customColor": "自定义颜色",
    "layoutMode": "布局模式",
    "sidebarLayout": "侧边栏",
    "topLayout": "顶部",
    "display": "界面显示",
    "fixedHeader": "固定 Header",
    "showTagsView": "显示标签栏"
  },
  "tabs": {
    "close": "关闭",
    "closeOther": "关闭其他",
    "closeLeft": "关闭左侧",
    "closeRight": "关闭右侧",
    "closeAll": "关闭全部",
    "refresh": "刷新",
    "pin": "固定",
    "unpin": "取消固定"
  },
  "search": {
    "placeholder": "搜索菜单、用户、角色、配置...",
    "noResult": "未找到相关结果",
    "history": "搜索历史",
    "clearHistory": "清除历史",
    "category": {
      "menu": "菜单",
      "user": "用户",
      "role": "角色",
      "config": "配置",
      "dict": "字典"
    }
  },
  "header": {
    "logout": "退出登录",
    "profile": "个人中心",
    "logoutConfirm": "确定要退出登录吗？"
  }
}
```

- [ ] **Step 2: 创建 zh-CN/menu.json**

```json
{
  "dashboard": "仪表盘",
  "system": "系统管理",
  "user": "用户管理",
  "role": "角色管理",
  "menu": "菜单管理",
  "config": "系统配置",
  "log": "日志管理",
  "operationLog": "操作日志",
  "loginLog": "登录日志",
  "dept": "部门管理",
  "dict": "字典管理",
  "online": "在线用户",
  "file": "文件管理",
  "notice": "通知公告",
  "job": "定时任务",
  "monitor": "系统监控",
  "profile": "个人中心"
}
```

- [ ] **Step 3: 创建 zh-CN/login.json**

```json
{
  "title": "后台管理系统",
  "username": "用户名",
  "password": "密码",
  "login": "登录",
  "usernamePlaceholder": "请输入用户名",
  "passwordPlaceholder": "请输入密码",
  "usernameRequired": "请输入用户名",
  "passwordRequired": "请输入密码"
}
```

- [ ] **Step 4: 创建 zh-CN/dashboard.json**

```json
{
  "title": "首页",
  "userCount": "用户总数",
  "roleCount": "角色数量",
  "loginCount": "登录总数",
  "systemStatus": "系统状态",
  "running": "运行中",
  "loginTrend": "近7日登录趋势",
  "loginTimes": "登录次数",
  "systemOverview": "系统概览",
  "recentLogin": "最近登录记录",
  "column": {
    "username": "用户名",
    "ip": "IP地址",
    "location": "登录地点",
    "browser": "浏览器",
    "status": "状态",
    "loginTime": "登录时间",
    "success": "成功",
    "failed": "失败"
  }
}
```

- [ ] **Step 5: 创建 zh-CN/system.json**

```json
{
  "user": {
    "title": "用户管理",
    "username": "用户名",
    "nickname": "昵称",
    "email": "邮箱",
    "phone": "手机号",
    "gender": "性别",
    "dept": "部门",
    "role": "角色",
    "status": "状态",
    "createTime": "创建时间",
    "addUser": "新增用户",
    "editUser": "编辑用户",
    "resetPassword": "重置密码",
    "male": "男",
    "female": "女",
    "unknown": "未知"
  },
  "role": {
    "title": "角色管理",
    "roleName": "角色名称",
    "roleKey": "角色标识",
    "sort": "排序",
    "status": "状态",
    "description": "描述",
    "menuPermission": "菜单权限",
    "addRole": "新增角色",
    "editRole": "编辑角色"
  },
  "menu": {
    "title": "菜单管理",
    "menuName": "菜单名称",
    "icon": "图标",
    "sort": "排序",
    "permission": "权限标识",
    "component": "组件路径",
    "menuType": "菜单类型",
    "directory": "目录",
    "menuItem": "菜单",
    "button": "按钮",
    "addMenu": "新增菜单",
    "editMenu": "编辑菜单"
  },
  "dept": {
    "title": "部门管理",
    "deptName": "部门名称",
    "leader": "负责人",
    "phone": "联系电话",
    "email": "邮箱",
    "sort": "排序",
    "addDept": "新增部门",
    "editDept": "编辑部门"
  },
  "dict": {
    "title": "字典管理",
    "dictName": "字典名称",
    "dictType": "字典类型",
    "dictLabel": "字典标签",
    "dictValue": "字典值"
  },
  "config": {
    "title": "系统配置",
    "configKey": "配置键",
    "configName": "配置名称",
    "configValue": "配置值"
  },
  "log": {
    "operation": "操作日志",
    "login": "登录日志"
  },
  "online": {
    "title": "在线用户",
    "userId": "用户ID",
    "username": "用户名",
    "nickname": "昵称",
    "loginTime": "登录时间",
    "sessionId": "会话标识",
    "forceLogout": "强制踢出"
  },
  "file": {
    "title": "文件管理",
    "fileName": "文件名",
    "fileSize": "文件大小",
    "fileType": "文件类型",
    "uploadTime": "上传时间"
  },
  "notice": {
    "title": "通知公告",
    "noticeTitle": "标题",
    "noticeType": "类型",
    "content": "内容"
  },
  "job": {
    "title": "定时任务",
    "jobName": "任务名称",
    "cronExpression": "Cron 表达式",
    "status": "状态",
    "lastExecTime": "上次执行"
  },
  "monitor": {
    "title": "系统监控"
  }
}
```

- [ ] **Step 6: 创建 zh-CN/profile.json**

```json
{
  "title": "个人中心",
  "basicInfo": "基本信息",
  "changePassword": "修改密码",
  "oldPassword": "原密码",
  "newPassword": "新密码",
  "confirmPassword": "确认密码",
  "avatar": "头像"
}
```

- [ ] **Step 7: Commit**

```bash
git add admin-frontend/src/locales/zh-CN/
git commit -m "feat(i18n): add zh-CN language pack"
```

---

## Task 3: i18n 基础设施 — 英文语言包

**Files:**
- Create: `src/locales/en-US/common.json`
- Create: `src/locales/en-US/menu.json`
- Create: `src/locales/en-US/login.json`
- Create: `src/locales/en-US/dashboard.json`
- Create: `src/locales/en-US/system.json`
- Create: `src/locales/en-US/profile.json`

- [ ] **Step 1: 创建 en-US/common.json**

```json
{
  "action": {
    "confirm": "Confirm",
    "cancel": "Cancel",
    "add": "Add",
    "edit": "Edit",
    "delete": "Delete",
    "search": "Search",
    "reset": "Reset",
    "export": "Export",
    "import": "Import",
    "refresh": "Refresh",
    "save": "Save",
    "back": "Back",
    "close": "Close",
    "submit": "Submit",
    "upload": "Upload",
    "download": "Download"
  },
  "label": {
    "status": "Status",
    "operation": "Operation",
    "remark": "Remark",
    "createTime": "Created At",
    "updateTime": "Updated At",
    "createdBy": "Created By",
    "enabled": "Enabled",
    "disabled": "Disabled",
    "yes": "Yes",
    "no": "No",
    "all": "All",
    "total": "{total} records"
  },
  "message": {
    "confirmDelete": "Are you sure you want to delete? This action cannot be undone.",
    "deleteSuccess": "Deleted successfully",
    "saveSuccess": "Saved successfully",
    "operationSuccess": "Operation successful",
    "operationFailed": "Operation failed",
    "loadingFailed": "Failed to load data",
    "networkError": "Network error, please try again later",
    "tip": "Tip",
    "warning": "Warning"
  },
  "pagination": {
    "total": "{total} records",
    "pageSize": "{size} / page"
  },
  "settings": {
    "title": "Settings",
    "themeMode": "Theme Mode",
    "light": "Light",
    "dark": "Dark",
    "auto": "System",
    "themeColor": "Theme Color",
    "customColor": "Custom Color",
    "layoutMode": "Layout Mode",
    "sidebarLayout": "Sidebar",
    "topLayout": "Top",
    "display": "Display",
    "fixedHeader": "Fixed Header",
    "showTagsView": "Show Tabs"
  },
  "tabs": {
    "close": "Close",
    "closeOther": "Close Others",
    "closeLeft": "Close Left",
    "closeRight": "Close Right",
    "closeAll": "Close All",
    "refresh": "Refresh",
    "pin": "Pin",
    "unpin": "Unpin"
  },
  "search": {
    "placeholder": "Search menus, users, roles, configs...",
    "noResult": "No results found",
    "history": "Search History",
    "clearHistory": "Clear History",
    "category": {
      "menu": "Menus",
      "user": "Users",
      "role": "Roles",
      "config": "Configs",
      "dict": "Dictionaries"
    }
  },
  "header": {
    "logout": "Logout",
    "profile": "Profile",
    "logoutConfirm": "Are you sure you want to logout?"
  }
}
```

- [ ] **Step 2: 创建 en-US/menu.json**

```json
{
  "dashboard": "Dashboard",
  "system": "System",
  "user": "Users",
  "role": "Roles",
  "menu": "Menus",
  "config": "Config",
  "log": "Logs",
  "operationLog": "Operation Logs",
  "loginLog": "Login Logs",
  "dept": "Departments",
  "dict": "Dictionaries",
  "online": "Online Users",
  "file": "Files",
  "notice": "Notices",
  "job": "Jobs",
  "monitor": "Monitor",
  "profile": "Profile"
}
```

- [ ] **Step 3: 创建 en-US/login.json**

```json
{
  "title": "Admin System",
  "username": "Username",
  "password": "Password",
  "login": "Login",
  "usernamePlaceholder": "Enter username",
  "passwordPlaceholder": "Enter password",
  "usernameRequired": "Please enter username",
  "passwordRequired": "Please enter password"
}
```

- [ ] **Step 4: 创建 en-US/dashboard.json**

```json
{
  "title": "Home",
  "userCount": "Users",
  "roleCount": "Roles",
  "loginCount": "Logins",
  "systemStatus": "System Status",
  "running": "Running",
  "loginTrend": "Login Trend (7 Days)",
  "loginTimes": "Login Count",
  "systemOverview": "System Overview",
  "recentLogin": "Recent Logins",
  "column": {
    "username": "Username",
    "ip": "IP Address",
    "location": "Location",
    "browser": "Browser",
    "status": "Status",
    "loginTime": "Login Time",
    "success": "Success",
    "failed": "Failed"
  }
}
```

- [ ] **Step 5: 创建 en-US/system.json**

```json
{
  "user": {
    "title": "User Management",
    "username": "Username",
    "nickname": "Nickname",
    "email": "Email",
    "phone": "Phone",
    "gender": "Gender",
    "dept": "Department",
    "role": "Role",
    "status": "Status",
    "createTime": "Created At",
    "addUser": "Add User",
    "editUser": "Edit User",
    "resetPassword": "Reset Password",
    "male": "Male",
    "female": "Female",
    "unknown": "Unknown"
  },
  "role": {
    "title": "Role Management",
    "roleName": "Role Name",
    "roleKey": "Role Key",
    "sort": "Sort",
    "status": "Status",
    "description": "Description",
    "menuPermission": "Menu Permissions",
    "addRole": "Add Role",
    "editRole": "Edit Role"
  },
  "menu": {
    "title": "Menu Management",
    "menuName": "Menu Name",
    "icon": "Icon",
    "sort": "Sort",
    "permission": "Permission",
    "component": "Component",
    "menuType": "Type",
    "directory": "Directory",
    "menuItem": "Menu",
    "button": "Button",
    "addMenu": "Add Menu",
    "editMenu": "Edit Menu"
  },
  "dept": {
    "title": "Department Management",
    "deptName": "Department Name",
    "leader": "Leader",
    "phone": "Phone",
    "email": "Email",
    "sort": "Sort",
    "addDept": "Add Department",
    "editDept": "Edit Department"
  },
  "dict": {
    "title": "Dictionary Management",
    "dictName": "Dictionary Name",
    "dictType": "Dictionary Type",
    "dictLabel": "Label",
    "dictValue": "Value"
  },
  "config": {
    "title": "System Config",
    "configKey": "Config Key",
    "configName": "Config Name",
    "configValue": "Config Value"
  },
  "log": {
    "operation": "Operation Logs",
    "login": "Login Logs"
  },
  "online": {
    "title": "Online Users",
    "userId": "User ID",
    "username": "Username",
    "nickname": "Nickname",
    "loginTime": "Login Time",
    "sessionId": "Session ID",
    "forceLogout": "Force Logout"
  },
  "file": {
    "title": "File Management",
    "fileName": "File Name",
    "fileSize": "Size",
    "fileType": "Type",
    "uploadTime": "Upload Time"
  },
  "notice": {
    "title": "Notices",
    "noticeTitle": "Title",
    "noticeType": "Type",
    "content": "Content"
  },
  "job": {
    "title": "Scheduled Jobs",
    "jobName": "Job Name",
    "cronExpression": "Cron Expression",
    "status": "Status",
    "lastExecTime": "Last Execution"
  },
  "monitor": {
    "title": "System Monitor"
  }
}
```

- [ ] **Step 6: 创建 en-US/profile.json**

```json
{
  "title": "Profile",
  "basicInfo": "Basic Info",
  "changePassword": "Change Password",
  "oldPassword": "Current Password",
  "newPassword": "New Password",
  "confirmPassword": "Confirm Password",
  "avatar": "Avatar"
}
```

- [ ] **Step 7: Commit**

```bash
git add admin-frontend/src/locales/en-US/
git commit -m "feat(i18n): add en-US language pack"
```

---

## Task 4: i18n 入口和 README

**Files:**
- Create: `src/locales/index.ts`
- Create: `src/locales/README.md`

- [ ] **Step 1: 创建 src/locales/index.ts**

```typescript
import { createI18n } from 'vue-i18n'

// 中文：默认语言，同步加载
import zhCommon from './zh-CN/common.json'
import zhMenu from './zh-CN/menu.json'
import zhLogin from './zh-CN/login.json'
import zhDashboard from './zh-CN/dashboard.json'
import zhSystem from './zh-CN/system.json'
import zhProfile from './zh-CN/profile.json'

const LOCALE_KEY = 'app-locale'

function getStoredLocale(): string {
  try {
    return localStorage.getItem(LOCALE_KEY) || 'zh-CN'
  } catch {
    return 'zh-CN'
  }
}

export function setStoredLocale(locale: string) {
  localStorage.setItem(LOCALE_KEY, locale)
}

const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': {
      common: zhCommon,
      menu: zhMenu,
      login: zhLogin,
      dashboard: zhDashboard,
      system: zhSystem,
      profile: zhProfile
    }
  }
})

// 英文：懒加载
const loadedLanguages = new Set(['zh-CN'])

export async function loadLanguage(locale: string) {
  if (loadedLanguages.has(locale)) {
    i18n.global.locale.value = locale
    setStoredLocale(locale)
    return
  }

  if (locale === 'en-US') {
    const [common, menu, login, dashboard, system, profile] = await Promise.all([
      import('./en-US/common.json'),
      import('./en-US/menu.json'),
      import('./en-US/login.json'),
      import('./en-US/dashboard.json'),
      import('./en-US/system.json'),
      import('./en-US/profile.json')
    ])
    i18n.global.setLocaleMessage('en-US', {
      common: common.default,
      menu: menu.default,
      login: login.default,
      dashboard: dashboard.default,
      system: system.default,
      profile: profile.default
    })
  }

  loadedLanguages.add(locale)
  i18n.global.locale.value = locale
  setStoredLocale(locale)
}

export const availableLocales = [
  { code: 'zh-CN', name: '中文' },
  { code: 'en-US', name: 'English' }
]

export default i18n
```

- [ ] **Step 2: 创建 src/locales/README.md**

```markdown
# 语言包规范

## 新增语种

1. 在 `src/locales/` 下创建语言目录（如 `ja-JP/`）
2. 按照 `zh-CN/` 的结构创建同名 JSON 文件，key 保持一致
3. 在 `src/locales/index.ts` 的 `loadLanguage` 函数中添加新语种的懒加载逻辑
4. 在 `availableLocales` 数组中注册新语种

## 文件结构

| 文件 | 内容 |
|------|------|
| common.json | 通用文本：按钮、标签、消息、分页、设置、标签页、搜索 |
| menu.json | 侧边栏菜单名称 |
| login.json | 登录页文本 |
| dashboard.json | 仪表盘页文本 |
| system.json | 系统管理所有子模块文本 |
| profile.json | 个人中心文本 |

## 使用方式

模板中：`{{ $t('common.action.confirm') }}`
脚本中：`const { t } = useI18n(); t('common.action.confirm')`

## 注意事项

- 所有 key 必须在 zh-CN 中有对应值（fallback 语言）
- 支持插值：`"共 {total} 条"` → `$t('common.label.total', { total: 100 })`
- 后端返回的动态数据不做翻译
```

- [ ] **Step 3: Commit**

```bash
git add admin-frontend/src/locales/index.ts admin-frontend/src/locales/README.md
git commit -m "feat(i18n): add i18n setup with lazy loading"
```

---

## Task 5: 注册 i18n 插件 + Element Plus locale

**Files:**
- Modify: `src/main.ts`
- Modify: `src/plugins/element-plus.ts`
- Modify: `src/App.vue`

- [ ] **Step 1: 修改 main.ts — 注册 i18n**

在 `app.use(pinia)` 之后、`app.use(elementPlus)` 之前添加 i18n：

```typescript
import { createApp } from 'vue'
import App from './App.vue'
import pinia from './stores'
import router from './router'
import i18n from './locales'
import elementPlus from './plugins/element-plus'
import { setupPermissionDirective } from './directives/permission'
import './assets/styles/global.scss'

const app = createApp(App)

app.use(pinia)
app.use(i18n)
app.use(router)
app.use(elementPlus)
setupPermissionDirective(app)

app.mount('#app')
```

- [ ] **Step 2: 修改 App.vue — 包裹 el-config-provider**

```vue
<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

const { locale } = useI18n()

const elementLocale = computed(() => {
  return locale.value === 'en-US' ? en : zhCn
})
</script>
```

- [ ] **Step 3: 验证编译无报错**

```bash
cd admin-frontend && npm run type-check
```

Expected: 无 TypeScript 错误。

- [ ] **Step 4: Commit**

```bash
git add admin-frontend/src/main.ts admin-frontend/src/App.vue
git commit -m "feat(i18n): register i18n plugin and element-plus locale"
```

---

## Task 6: i18n 化 — Layout 组件

**Files:**
- Modify: `src/components/Layout/Header.vue`
- Modify: `src/components/Layout/Sidebar.vue`
- Modify: `src/components/Layout/SettingsDrawer.vue`

- [ ] **Step 1: 修改 Header.vue — 添加语言切换 + i18n 文本**

在 `<div class="header-right">` 中，Setting 图标之前添加语言切换下拉：

```vue
<!-- 在 Setting 图标之前添加 -->
<el-dropdown @command="handleLocaleChange" class="locale-switcher">
  <span class="action-icon locale-text">{{ currentLocaleName }}</span>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item
        v-for="loc in availableLocales"
        :key="loc.code"
        :command="loc.code"
        :disabled="locale === loc.code"
      >
        {{ loc.name }}
      </el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>
```

在 `<script setup>` 中添加：

```typescript
import { useI18n } from 'vue-i18n'
import { loadLanguage, availableLocales } from '@/locales'

const { t, locale } = useI18n()

const currentLocaleName = computed(() => {
  return availableLocales.find(l => l.code === locale.value)?.name || '中文'
})

async function handleLocaleChange(code: string) {
  await loadLanguage(code)
}
```

将硬编码中文替换为 `$t()` 调用：
- `'个人中心'` → `$t('common.header.profile')`
- `'退出登录'` → `$t('common.header.logout')`
- `'确定要退出登录吗？'` → `$t('common.header.logoutConfirm')`
- `'提示'` → `$t('common.message.tip')`
- `'确定'` → `$t('common.action.confirm')`
- `'取消'` → `$t('common.action.cancel')`
- `'用户'`（fallback 昵称）→ `$t('system.user.username')`

添加样式：

```scss
.locale-text {
  font-size: 13px;
  cursor: pointer;
  color: #606266;
  padding: 6px 8px;
  border-radius: 4px;
  &:hover { background-color: #f5f7fa; color: var(--el-color-primary); }
}
```

- [ ] **Step 2: 修改 Sidebar.vue — 静态菜单 i18n**

将硬编码的"仪表盘"替换为 `$t()`：

```vue
<el-menu-item index="/dashboard">
  <el-icon><Monitor /></el-icon>
  <template #title>{{ $t('menu.dashboard') }}</template>
</el-menu-item>
```

- [ ] **Step 3: 修改 SettingsDrawer.vue — 设置面板 i18n**

将所有中文替换为 `$t()` 调用：

```vue
<el-drawer v-model="visible" :title="$t('common.settings.title')" :size="300" direction="rtl">
  <div class="settings-section">
    <h4>{{ $t('common.settings.themeMode') }}</h4>
    <el-radio-group ...>
      <el-radio-button value="light">{{ $t('common.settings.light') }}</el-radio-button>
      <el-radio-button value="dark">{{ $t('common.settings.dark') }}</el-radio-button>
    </el-radio-group>
  </div>

  <div class="settings-section">
    <h4>{{ $t('common.settings.themeColor') }}</h4>
    <!-- color list unchanged -->
  </div>

  <div class="settings-section">
    <h4>{{ $t('common.settings.layoutMode') }}</h4>
    <el-radio-group ...>
      <el-radio-button value="sidebar">{{ $t('common.settings.sidebarLayout') }}</el-radio-button>
      <el-radio-button value="top">{{ $t('common.settings.topLayout') }}</el-radio-button>
    </el-radio-group>
  </div>

  <div class="settings-section">
    <h4>{{ $t('common.settings.display') }}</h4>
    <div class="settings-item">
      <span>{{ $t('common.settings.fixedHeader') }}</span>
      <el-switch v-model="appStore.fixedHeader" />
    </div>
    <div class="settings-item">
      <span>{{ $t('common.settings.showTagsView') }}</span>
      <el-switch v-model="appStore.showTagsView" />
    </div>
  </div>
</el-drawer>
```

- [ ] **Step 4: 验证编译和页面显示**

```bash
cd admin-frontend && npm run type-check
```

手动验证：刷新页面，Layout 中的中文仍正确显示，切换到英文后对应文本变为英文。

- [ ] **Step 5: Commit**

```bash
git add admin-frontend/src/components/Layout/Header.vue admin-frontend/src/components/Layout/Sidebar.vue admin-frontend/src/components/Layout/SettingsDrawer.vue
git commit -m "feat(i18n): internationalize layout components"
```

---

## Task 7: i18n 化 — 页面视图（批量替换）

**Files:**
- Modify: `src/views/login/index.vue`
- Modify: `src/views/dashboard/index.vue`
- Modify: 其他 views 页面

- [ ] **Step 1: i18n 化登录页**

将登录页中的所有中文字符串替换为 `$t('login.xxx')` 调用。在 `<script setup>` 中不需要额外导入，`$t` 在模板中全局可用。

关键替换：
- `'后台管理系统'` → `$t('login.title')`
- `'请输入用户名'` → `$t('login.usernamePlaceholder')`
- `'请输入密码'` → `$t('login.passwordPlaceholder')`
- `'登录'` → `$t('login.login')`
- 表单校验 message → 使用 `t('login.usernameRequired')` （script 中需 `const { t } = useI18n()`）

- [ ] **Step 2: i18n 化仪表盘页**

关键替换：
- `'用户总数'` → `$t('dashboard.userCount')`
- `'角色数量'` → `$t('dashboard.roleCount')`
- `'登录总数'` → `$t('dashboard.loginCount')`
- `'运行中'` → `$t('dashboard.running')`
- `'近7日登录趋势'` → `$t('dashboard.loginTrend')`
- 表头列同理

- [ ] **Step 3: i18n 化系统管理页面**

逐个替换 `src/views/system/` 下所有页面的中文字符串。每个页面的 key 前缀为 `system.{module}`：
- `views/system/user/index.vue` → `$t('system.user.xxx')`
- `views/system/role/index.vue` → `$t('system.role.xxx')`
- `views/system/menu/index.vue` → `$t('system.menu.xxx')`
- 其他页面同理

对于按钮文本使用 common key：
- `'新增'` → `$t('common.action.add')`
- `'编辑'` → `$t('common.action.edit')`
- `'删除'` → `$t('common.action.delete')`
- `'搜索'` → `$t('common.action.search')`
- `'重置'` → `$t('common.action.reset')`

- [ ] **Step 4: i18n 化个人中心**

- `'基本信息'` → `$t('profile.basicInfo')`
- `'修改密码'` → `$t('profile.changePassword')`

- [ ] **Step 5: 验证编译**

```bash
cd admin-frontend && npm run type-check
```

- [ ] **Step 6: Commit**

```bash
git add admin-frontend/src/views/
git commit -m "feat(i18n): internationalize all page views"
```

---

## Task 8: 暗黑模式增强 — useAppStore 升级

**Files:**
- Modify: `src/stores/modules/app.ts`
- Create: `src/assets/styles/dark.scss`
- Test: `src/stores/__tests__/app-theme.test.ts`

- [ ] **Step 1: 编写主题增强测试**

创建 `src/stores/__tests__/app-theme.test.ts`：

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '../modules/app'

describe('app store - theme enhancement', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    document.documentElement.classList.remove('dark')
    document.documentElement.style.removeProperty('--el-color-primary')
  })

  it('setThemeMode("auto") 应根据系统偏好设置暗黑模式', () => {
    const store = useAppStore()
    // 模拟系统偏好为暗色
    const matchMedia = vi.fn().mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })
    vi.stubGlobal('matchMedia', matchMedia)

    store.setThemeMode('auto')
    expect(store.themeMode).toBe('auto')
    expect(document.documentElement.classList.contains('dark')).toBe(true)

    vi.unstubAllGlobals()
  })

  it('setThemeColor 应设置主色和衍生色', () => {
    const store = useAppStore()
    store.setThemeColor('#722ED1')
    expect(store.themeColor).toBe('#722ED1')
    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#722ED1')
    // 验证衍生色
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-3')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-5')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-7')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-9')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-dark-2')).toBeTruthy()
  })

  it('themeMode 应持久化到 localStorage', () => {
    const store = useAppStore()
    store.setThemeMode('dark')
    const saved = JSON.parse(localStorage.getItem('app-settings') || '{}')
    expect(saved.themeMode).toBe('dark')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd admin-frontend && npx vitest run src/stores/__tests__/app-theme.test.ts
```

Expected: FAIL — `setThemeMode('auto')` 不存在、衍生色未设置。

- [ ] **Step 3: 修改 useAppStore — 增强主题功能**

修改 `src/stores/modules/app.ts`：

```typescript
import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'
export type LayoutMode = 'sidebar' | 'top'
export type ThemeColor = string

const SETTINGS_KEY = 'app-settings'

function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

/** 将 HEX 色值与白色混合，生成 Element Plus light-N 衍生色 */
function mixWhite(hex: string, percentage: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const mix = (c: number) => Math.round(c + (255 - c) * (percentage / 10))
  return `rgb(${mix(r)}, ${mix(g)}, ${mix(b)})`
}

/** 将 HEX 色值与黑色混合，生成 Element Plus dark-N 衍生色 */
function mixBlack(hex: string, percentage: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const mix = (c: number) => Math.round(c * (1 - percentage / 10))
  return `rgb(${mix(r)}, ${mix(g)}, ${mix(b)})`
}

/** 设置主色及所有衍生色到 CSS 变量 */
function applyPrimaryColor(color: string) {
  const el = document.documentElement
  el.style.setProperty('--el-color-primary', color)
  for (const level of [3, 5, 7, 9]) {
    el.style.setProperty(`--el-color-primary-light-${level}`, mixWhite(color, level))
  }
  el.style.setProperty('--el-color-primary-dark-2', mixBlack(color, 2))
}

/** 判断系统是否偏好暗色模式 */
function prefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

/** 根据模式应用 dark class */
function applyDarkClass(mode: ThemeMode) {
  const isDark = mode === 'dark' || (mode === 'auto' && prefersDark())
  document.documentElement.classList.toggle('dark', isDark)
}

export const useAppStore = defineStore('app', () => {
  const saved = loadSettings()

  const sidebarCollapsed = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')
  const themeMode = ref<ThemeMode>(saved.themeMode || 'light')
  const themeColor = ref<ThemeColor>(saved.themeColor || '#409eff')
  const layoutMode = ref<LayoutMode>(saved.layoutMode || 'sidebar')
  const showTagsView = ref<boolean>(saved.showTagsView !== false)
  const fixedHeader = ref<boolean>(saved.fixedHeader !== false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setDevice(val: 'desktop' | 'mobile') {
    device.value = val
  }

  function setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    applyDarkClass(mode)
  }

  function setThemeColor(color: ThemeColor) {
    themeColor.value = color
    applyPrimaryColor(color)
  }

  function setLayoutMode(mode: LayoutMode) {
    layoutMode.value = mode
  }

  function persistSettings() {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify({
      themeMode: themeMode.value,
      themeColor: themeColor.value,
      layoutMode: layoutMode.value,
      showTagsView: showTagsView.value,
      fixedHeader: fixedHeader.value
    }))
  }

  // 自动持久化
  watch([themeMode, themeColor, layoutMode, showTagsView, fixedHeader], persistSettings)

  // 初始化主题
  applyDarkClass(themeMode.value)
  if (themeColor.value !== '#409eff') {
    applyPrimaryColor(themeColor.value)
  }

  // auto 模式：监听系统主题变化
  if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (themeMode.value === 'auto') {
        applyDarkClass('auto')
      }
    })
  }

  return {
    sidebarCollapsed, device, themeMode, themeColor, layoutMode, showTagsView, fixedHeader,
    toggleSidebar, setDevice, setThemeMode, setThemeColor, setLayoutMode
  }
})
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd admin-frontend && npx vitest run src/stores/__tests__/app-theme.test.ts
```

Expected: PASS

- [ ] **Step 5: 创建 dark.scss**

创建 `src/assets/styles/dark.scss`：

```scss
// Element Plus 暗黑模式 CSS 变量
@use 'element-plus/theme-chalk/dark/css-vars.css';

html.dark {
  // 侧边栏
  --sidebar-bg: #1d1e1f;
  --sidebar-header-bg: #141414;

  // 头部
  --header-bg: #1d1e1f;
  --header-border: #363637;

  // 内容区
  --content-bg: #141414;

  // 文本
  --text-primary: #e5eaf3;
  --text-regular: #cfd3dc;
  --text-secondary: #a3a6ad;

  // 边框
  --border-color: #363637;
  --border-light: #414243;

  // 滚动条
  --scrollbar-thumb: #4c4d4f;
}
```

- [ ] **Step 6: 在 global.scss 中导入 dark.scss + 适配暗黑模式**

在 `global.scss` 顶部添加：

```scss
@use './dark.scss';
```

在 `global.scss` 末尾添加暗黑模式覆盖：

```scss
// ==================== 暗黑模式适配 ====================

html.dark {
  ::-webkit-scrollbar-thumb {
    background: var(--scrollbar-thumb, #4c4d4f);
  }
}
```

- [ ] **Step 7: 修改 Layout 组件使用 CSS 变量**

修改 `Layout/index.vue` 样式，让侧边栏和头部颜色支持暗黑模式：

```scss
.layout-aside {
  background-color: var(--sidebar-bg, $sidebar-bg);
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout-header {
  display: flex;
  align-items: center;
  padding: 0;
  background-color: var(--header-bg, $header-bg);
  border-bottom: 1px solid var(--header-border, #ebeef5);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.layout-content {
  background-color: var(--content-bg, $content-bg);
  padding: 20px;
  overflow-y: auto;
}
```

- [ ] **Step 8: Commit**

```bash
git add admin-frontend/src/stores/modules/app.ts admin-frontend/src/stores/__tests__/app-theme.test.ts admin-frontend/src/assets/styles/dark.scss admin-frontend/src/assets/styles/global.scss admin-frontend/src/components/Layout/index.vue
git commit -m "feat(theme): enhance dark mode with auto system preference and derived colors"
```

---

## Task 9: 暗黑模式 — SettingsDrawer 增强

**Files:**
- Modify: `src/components/Layout/SettingsDrawer.vue`

- [ ] **Step 1: 添加 auto 模式和取色器**

修改 `SettingsDrawer.vue`：

```vue
<template>
  <el-drawer v-model="visible" :title="$t('common.settings.title')" :size="300" direction="rtl">
    <div class="settings-section">
      <h4>{{ $t('common.settings.themeMode') }}</h4>
      <el-radio-group :model-value="appStore.themeMode" @change="(val: string) => appStore.setThemeMode(val as 'light' | 'dark' | 'auto')">
        <el-radio-button value="light">{{ $t('common.settings.light') }}</el-radio-button>
        <el-radio-button value="dark">{{ $t('common.settings.dark') }}</el-radio-button>
        <el-radio-button value="auto">{{ $t('common.settings.auto') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.themeColor') }}</h4>
      <div class="color-list">
        <div
          v-for="color in colorPresets"
          :key="color"
          class="color-item"
          :style="{ backgroundColor: color }"
          :class="{ active: appStore.themeColor === color }"
          @click="appStore.setThemeColor(color)"
        />
        <el-color-picker
          :model-value="appStore.themeColor"
          @change="(val: string | null) => val && appStore.setThemeColor(val)"
          size="small"
        />
      </div>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.layoutMode') }}</h4>
      <el-radio-group :model-value="appStore.layoutMode" @change="(val: string) => appStore.setLayoutMode(val as 'sidebar' | 'top')">
        <el-radio-button value="sidebar">{{ $t('common.settings.sidebarLayout') }}</el-radio-button>
        <el-radio-button value="top">{{ $t('common.settings.topLayout') }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="settings-section">
      <h4>{{ $t('common.settings.display') }}</h4>
      <div class="settings-item">
        <span>{{ $t('common.settings.fixedHeader') }}</span>
        <el-switch v-model="appStore.fixedHeader" />
      </div>
      <div class="settings-item">
        <span>{{ $t('common.settings.showTagsView') }}</span>
        <el-switch v-model="appStore.showTagsView" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/modules/app'

const visible = defineModel<boolean>({ default: false })
const appStore = useAppStore()

const colorPresets = [
  '#409eff', '#67C23A', '#722ED1',
  '#F56C6C', '#E6A23C', '#13c2c2'
]
</script>

<style scoped lang="scss">
.settings-section {
  margin-bottom: 24px;

  h4 {
    margin: 0 0 12px;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

.color-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.color-item {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;

  &.active {
    border-color: var(--el-text-color-primary);
  }
}

.settings-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}
</style>
```

- [ ] **Step 2: 验证**

```bash
cd admin-frontend && npm run type-check
```

手动验证：打开设置抽屉，检查 auto 模式、预设色和取色器均可用。

- [ ] **Step 3: Commit**

```bash
git add admin-frontend/src/components/Layout/SettingsDrawer.vue
git commit -m "feat(theme): add auto mode, color picker, and preset colors to settings"
```

---

## Task 10: 多标签页 — useTabStore

**Files:**
- Create: `src/stores/modules/tab.ts`
- Test: `src/stores/__tests__/tab.test.ts`

- [ ] **Step 1: 编写 useTabStore 测试**

创建 `src/stores/__tests__/tab.test.ts`：

```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTabStore } from '../modules/tab'

describe('tab store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始状态包含仪表盘固定标签', () => {
    const store = useTabStore()
    expect(store.tabs).toHaveLength(1)
    expect(store.tabs[0].path).toBe('/dashboard')
    expect(store.tabs[0].pinned).toBe(true)
    expect(store.activeTab).toBe('/dashboard')
  })

  it('addTab 添加新标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理', icon: 'User' })
    expect(store.tabs).toHaveLength(2)
    expect(store.activeTab).toBe('/system/user')
  })

  it('addTab 不重复添加已存在的标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/user', title: '用户管理' })
    expect(store.tabs).toHaveLength(2)
  })

  it('closeTab 关闭标签并激活相邻标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    store.setActiveTab('/system/user')

    const nextPath = store.closeTab('/system/user')
    expect(store.tabs).toHaveLength(2) // dashboard + role
    expect(nextPath).toBe('/system/role')
  })

  it('closeTab 不能关闭固定标签', () => {
    const store = useTabStore()
    store.closeTab('/dashboard')
    expect(store.tabs).toHaveLength(1) // dashboard still there
  })

  it('closeOtherTabs 关闭其他非固定标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    store.closeOtherTabs('/system/user')
    expect(store.tabs).toHaveLength(2) // dashboard (pinned) + user (current)
  })

  it('closeLeftTabs 关闭左侧非固定标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    store.addTab({ path: '/system/menu', title: '菜单管理' })
    store.closeLeftTabs('/system/menu')
    // dashboard (pinned) + menu (current)
    expect(store.tabs.map(t => t.path)).toEqual(['/dashboard', '/system/menu'])
  })

  it('closeRightTabs 关闭右侧非固定标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    store.addTab({ path: '/system/menu', title: '菜单管理' })
    store.closeRightTabs('/system/user')
    expect(store.tabs.map(t => t.path)).toEqual(['/dashboard', '/system/user'])
  })

  it('pinTab / unpinTab 固定和取消固定标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.pinTab('/system/user')
    expect(store.tabs.find(t => t.path === '/system/user')?.pinned).toBe(true)

    store.unpinTab('/system/user')
    expect(store.tabs.find(t => t.path === '/system/user')?.pinned).toBe(false)
  })

  it('closeAllTabs 关闭所有非固定标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    store.closeAllTabs()
    expect(store.tabs).toHaveLength(1) // only dashboard
    expect(store.activeTab).toBe('/dashboard')
  })

  it('cachedViews 返回已打开标签的组件名', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理', name: 'system-user' })
    store.addTab({ path: '/system/role', title: '角色管理', name: 'system-role' })
    expect(store.cachedViews).toContain('system-user')
    expect(store.cachedViews).toContain('system-role')
  })

  it('reorderTabs 重新排序标签', () => {
    const store = useTabStore()
    store.addTab({ path: '/system/user', title: '用户管理' })
    store.addTab({ path: '/system/role', title: '角色管理' })
    // 将 role 移到 user 前面
    store.reorderTabs(2, 1)
    expect(store.tabs[1].path).toBe('/system/role')
    expect(store.tabs[2].path).toBe('/system/user')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd admin-frontend && npx vitest run src/stores/__tests__/tab.test.ts
```

Expected: FAIL — `useTabStore` 不存在。

- [ ] **Step 3: 实现 useTabStore**

创建 `src/stores/modules/tab.ts`：

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Tab {
  path: string
  title: string
  icon?: string
  name?: string   // 路由 name，用于 keep-alive
  pinned: boolean
  closable: boolean
}

const TABS_KEY = 'app-tabs'

function loadTabs(): Tab[] {
  try {
    const raw = localStorage.getItem(TABS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const DEFAULT_TAB: Tab = {
  path: '/dashboard',
  title: '仪表盘',
  icon: 'Monitor',
  name: 'Dashboard',
  pinned: true,
  closable: false
}

export const useTabStore = defineStore('tab', () => {
  const saved = loadTabs()
  const tabs = ref<Tab[]>(saved.length > 0 ? saved : [{ ...DEFAULT_TAB }])
  const activeTab = ref(tabs.value[tabs.value.length - 1]?.path || '/dashboard')

  // 确保仪表盘始终存在
  if (!tabs.value.find(t => t.path === '/dashboard')) {
    tabs.value.unshift({ ...DEFAULT_TAB })
  }

  const cachedViews = computed(() =>
    tabs.value.filter(t => t.name).map(t => t.name!)
  )

  function persistTabs() {
    localStorage.setItem(TABS_KEY, JSON.stringify(tabs.value))
  }

  function addTab(tab: { path: string; title: string; icon?: string; name?: string }) {
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push({
        path: tab.path,
        title: tab.title,
        icon: tab.icon,
        name: tab.name,
        pinned: false,
        closable: true
      })
    }
    activeTab.value = tab.path
    persistTabs()
  }

  function setActiveTab(path: string) {
    activeTab.value = path
  }

  function closeTab(path: string): string {
    const tab = tabs.value.find(t => t.path === path)
    if (!tab || tab.pinned) return activeTab.value

    const index = tabs.value.indexOf(tab)
    tabs.value.splice(index, 1)

    // 如果关闭的是当前标签，激活相邻标签
    if (activeTab.value === path) {
      const next = tabs.value[index] || tabs.value[index - 1]
      activeTab.value = next?.path || '/dashboard'
    }

    persistTabs()
    return activeTab.value
  }

  function closeOtherTabs(path: string) {
    tabs.value = tabs.value.filter(t => t.pinned || t.path === path)
    activeTab.value = path
    persistTabs()
  }

  function closeLeftTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index <= 0) return
    tabs.value = tabs.value.filter((t, i) => t.pinned || i >= index)
    activeTab.value = path
    persistTabs()
  }

  function closeRightTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index < 0) return
    tabs.value = tabs.value.filter((t, i) => t.pinned || i <= index)
    activeTab.value = path
    persistTabs()
  }

  function closeAllTabs() {
    tabs.value = tabs.value.filter(t => t.pinned)
    activeTab.value = tabs.value[0]?.path || '/dashboard'
    persistTabs()
  }

  function pinTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab) {
      tab.pinned = true
      tab.closable = false
      persistTabs()
    }
  }

  function unpinTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab && tab.path !== '/dashboard') {
      tab.pinned = false
      tab.closable = true
      persistTabs()
    }
  }

  function reorderTabs(fromIndex: number, toIndex: number) {
    const [moved] = tabs.value.splice(fromIndex, 1)
    tabs.value.splice(toIndex, 0, moved)
    persistTabs()
  }

  function refreshTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab?.name) {
      // 暂时移出缓存列表，触发组件重新挂载
      const name = tab.name
      tab.name = undefined
      setTimeout(() => {
        tab.name = name
      }, 100)
    }
  }

  function resetTabs() {
    tabs.value = [{ ...DEFAULT_TAB }]
    activeTab.value = '/dashboard'
    persistTabs()
  }

  return {
    tabs,
    activeTab,
    cachedViews,
    addTab,
    setActiveTab,
    closeTab,
    closeOtherTabs,
    closeLeftTabs,
    closeRightTabs,
    closeAllTabs,
    pinTab,
    unpinTab,
    reorderTabs,
    refreshTab,
    resetTabs
  }
})
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd admin-frontend && npx vitest run src/stores/__tests__/tab.test.ts
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add admin-frontend/src/stores/modules/tab.ts admin-frontend/src/stores/__tests__/tab.test.ts
git commit -m "feat(tabs): implement useTabStore with full tab management"
```

---

## Task 11: 多标签页 — TabBar 组件

**Files:**
- Create: `src/components/Layout/TabContextMenu.vue`
- Create: `src/components/Layout/TabBar.vue`

- [ ] **Step 1: 创建 TabContextMenu.vue**

```vue
<template>
  <div
    v-show="visible"
    class="tab-context-menu"
    :style="{ left: x + 'px', top: y + 'px' }"
  >
    <ul>
      <li @click="emit('refresh')">
        <el-icon><Refresh /></el-icon>{{ $t('common.tabs.refresh') }}
      </li>
      <li v-if="!isPinned" @click="emit('pin')">
        <el-icon><Lock /></el-icon>{{ $t('common.tabs.pin') }}
      </li>
      <li v-if="isPinned && closable" @click="emit('unpin')">
        <el-icon><Unlock /></el-icon>{{ $t('common.tabs.unpin') }}
      </li>
      <li v-if="closable" @click="emit('close')" class="divider">
        <el-icon><Close /></el-icon>{{ $t('common.tabs.close') }}
      </li>
      <li @click="emit('closeOther')">
        <el-icon><FolderDelete /></el-icon>{{ $t('common.tabs.closeOther') }}
      </li>
      <li @click="emit('closeLeft')">
        <el-icon><DArrowLeft /></el-icon>{{ $t('common.tabs.closeLeft') }}
      </li>
      <li @click="emit('closeRight')">
        <el-icon><DArrowRight /></el-icon>{{ $t('common.tabs.closeRight') }}
      </li>
      <li @click="emit('closeAll')">
        <el-icon><CircleClose /></el-icon>{{ $t('common.tabs.closeAll') }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  x: number
  y: number
  isPinned: boolean
  closable: boolean
}>()

const emit = defineEmits<{
  refresh: []
  pin: []
  unpin: []
  close: []
  closeOther: []
  closeLeft: []
  closeRight: []
  closeAll: []
}>()
</script>

<style scoped lang="scss">
.tab-context-menu {
  position: fixed;
  z-index: 3001;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    font-size: 13px;
    cursor: pointer;
    color: var(--el-text-color-regular);
    white-space: nowrap;

    &:hover {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }

    &.divider {
      border-bottom: 1px solid var(--el-border-color-lighter);
      margin-bottom: 4px;
      padding-bottom: 12px;
    }
  }
}
</style>
```

- [ ] **Step 2: 创建 TabBar.vue**

```vue
<template>
  <div class="tab-bar">
    <el-icon class="tab-scroll-btn" @click="scrollLeft">
      <ArrowLeft />
    </el-icon>

    <div ref="scrollContainer" class="tab-scroll" @wheel.prevent="handleWheel">
      <draggable
        v-model="tabStore.tabs"
        item-key="path"
        class="tab-list"
        ghost-class="tab-ghost"
        :animation="200"
        @end="onDragEnd"
      >
        <template #item="{ element: tab, index }">
          <div
            class="tab-item"
            :class="{ active: tabStore.activeTab === tab.path, pinned: tab.pinned }"
            @click="handleClick(tab)"
            @contextmenu.prevent="openContextMenu($event, tab, index)"
          >
            <el-icon v-if="tab.pinned" class="pin-icon"><Lock /></el-icon>
            <span class="tab-title">{{ tab.title }}</span>
            <el-icon
              v-if="tab.closable"
              class="tab-close"
              @click.stop="handleClose(tab.path)"
            >
              <Close />
            </el-icon>
          </div>
        </template>
      </draggable>
    </div>

    <el-icon class="tab-scroll-btn" @click="scrollRight">
      <ArrowRight />
    </el-icon>

    <TabContextMenu
      :visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :is-pinned="contextMenu.isPinned"
      :closable="contextMenu.closable"
      @refresh="handleRefresh"
      @pin="handlePin"
      @unpin="handleUnpin"
      @close="handleContextClose"
      @close-other="handleCloseOther"
      @close-left="handleCloseLeft"
      @close-right="handleCloseRight"
      @close-all="handleCloseAll"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { useTabStore } from '@/stores/modules/tab'
import TabContextMenu from './TabContextMenu.vue'
import type { Tab } from '@/stores/modules/tab'

const router = useRouter()
const tabStore = useTabStore()
const scrollContainer = ref<HTMLElement>()

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  tabPath: '',
  isPinned: false,
  closable: false
})

function handleClick(tab: Tab) {
  tabStore.setActiveTab(tab.path)
  router.push(tab.path)
}

function handleClose(path: string) {
  const nextPath = tabStore.closeTab(path)
  if (nextPath !== router.currentRoute.value.path) {
    router.push(nextPath)
  }
}

function openContextMenu(e: MouseEvent, tab: Tab, _index: number) {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.tabPath = tab.path
  contextMenu.isPinned = tab.pinned
  contextMenu.closable = tab.closable
}

function closeContextMenu() {
  contextMenu.visible = false
}

function handleRefresh() {
  tabStore.refreshTab(contextMenu.tabPath)
  closeContextMenu()
}

function handlePin() {
  tabStore.pinTab(contextMenu.tabPath)
  closeContextMenu()
}

function handleUnpin() {
  tabStore.unpinTab(contextMenu.tabPath)
  closeContextMenu()
}

function handleContextClose() {
  const nextPath = tabStore.closeTab(contextMenu.tabPath)
  if (nextPath !== router.currentRoute.value.path) {
    router.push(nextPath)
  }
  closeContextMenu()
}

function handleCloseOther() {
  tabStore.closeOtherTabs(contextMenu.tabPath)
  router.push(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseLeft() {
  tabStore.closeLeftTabs(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseRight() {
  tabStore.closeRightTabs(contextMenu.tabPath)
  closeContextMenu()
}

function handleCloseAll() {
  tabStore.closeAllTabs()
  router.push('/dashboard')
  closeContextMenu()
}

function onDragEnd(e: { oldIndex: number; newIndex: number }) {
  if (e.oldIndex !== e.newIndex) {
    tabStore.reorderTabs(e.oldIndex, e.newIndex)
  }
}

function scrollLeft() {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft -= 200
  }
}

function scrollRight() {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft += 200
  }
}

function handleWheel(e: WheelEvent) {
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft += e.deltaY
  }
}

function handleClickOutside() {
  closeContextMenu()
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.tab-bar {
  display: flex;
  align-items: center;
  background: var(--header-bg, #fff);
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 4px;
  height: 38px;
}

.tab-scroll-btn {
  flex-shrink: 0;
  cursor: pointer;
  padding: 4px;
  color: var(--el-text-color-secondary);
  &:hover { color: var(--el-color-primary); }
}

.tab-scroll {
  flex: 1;
  overflow-x: hidden;
  overflow-y: hidden;
  scroll-behavior: smooth;
}

.tab-list {
  display: flex;
  gap: 4px;
  padding: 4px 0;
  white-space: nowrap;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  border: 1px solid transparent;
  transition: all 0.2s;
  user-select: none;

  &:hover {
    color: var(--el-color-primary);
  }

  &.active {
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
  }

  &.pinned .tab-title {
    font-weight: 500;
  }
}

.pin-icon {
  font-size: 10px;
}

.tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
  &:hover {
    background: var(--el-color-danger-light-7);
    color: var(--el-color-danger);
  }
}

.tab-ghost {
  opacity: 0.5;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add admin-frontend/src/components/Layout/TabBar.vue admin-frontend/src/components/Layout/TabContextMenu.vue
git commit -m "feat(tabs): create TabBar and TabContextMenu components"
```

---

## Task 12: 多标签页 — 集成到 Layout 和路由

**Files:**
- Modify: `src/components/Layout/index.vue`
- Modify: `src/router/guard.ts`

- [ ] **Step 1: 修改 Layout/index.vue — 插入 TabBar + keep-alive**

```vue
<template>
  <el-container class="layout-container">
    <el-aside class="layout-aside" :width="appStore.sidebarCollapsed ? '64px' : '200px'">
      <Sidebar />
    </el-aside>
    <el-container class="layout-main">
      <el-header class="layout-header" height="60px">
        <Header />
      </el-header>
      <TabBar v-if="appStore.showTagsView" />
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="tabStore.cachedViews">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import TabBar from './TabBar.vue'
import { useAppStore } from '@/stores/modules/app'
import { useTabStore } from '@/stores/modules/tab'

const appStore = useAppStore()
const tabStore = useTabStore()
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: var(--sidebar-bg, $sidebar-bg);
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout-main {
  display: flex;
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: center;
  padding: 0;
  background-color: var(--header-bg, $header-bg);
  border-bottom: 1px solid var(--header-border, #ebeef5);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.layout-content {
  background-color: var(--content-bg, $content-bg);
  padding: 20px;
  overflow-y: auto;
}
</style>
```

- [ ] **Step 2: 修改 router/guard.ts — 路由切换时自动添加标签**

在 `afterEach` 钩子中添加标签逻辑：

```typescript
import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/modules/user'
import { usePermissionStore } from '@/stores/modules/permission'
import { useTabStore } from '@/stores/modules/tab'
import { notFoundRoute } from './static-routes'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/403', '/404']

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()
    document.title = `${to.meta.title || ''} - 后台管理系统`

    const token = getToken()

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' })
        return
      }

      const userStore = useUserStore()
      if (userStore.userInfo) {
        next()
        return
      }

      // 已登录但没有用户信息，获取信息并生成动态路由
      try {
        const data = await userStore.fetchUserInfo()
        const permissionStore = usePermissionStore()
        const routes = permissionStore.generateRoutes(data.menus)

        // 添加动态路由
        routes.forEach(route => {
          router.addRoute('Layout', route)
        })
        // 添加 404 兜底
        router.addRoute(notFoundRoute)

        // 重新导航到目标页面
        next({ ...to, replace: true })
      } catch {
        userStore.resetState()
        next(`/login?redirect=${to.path}`)
      }
    } else {
      if (whiteList.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })

  router.afterEach((to) => {
    NProgress.done()

    // 自动添加标签页（排除白名单和隐藏路由）
    if (!whiteList.includes(to.path) && !to.meta.hidden) {
      const tabStore = useTabStore()
      tabStore.addTab({
        path: to.path,
        title: (to.meta.title as string) || '',
        icon: (to.meta.icon as string) || '',
        name: (to.name as string) || ''
      })
    }
  })
}
```

- [ ] **Step 3: 验证**

```bash
cd admin-frontend && npm run type-check
```

- [ ] **Step 4: Commit**

```bash
git add admin-frontend/src/components/Layout/index.vue admin-frontend/src/router/guard.ts
git commit -m "feat(tabs): integrate TabBar into layout with router and keep-alive"
```

---

## Task 13: 全局搜索 — useGlobalSearch

**Files:**
- Create: `src/composables/useGlobalSearch.ts`
- Test: `src/composables/__tests__/useGlobalSearch.test.ts`

- [ ] **Step 1: 编写搜索 composable 测试**

创建 `src/composables/__tests__/useGlobalSearch.test.ts`：

```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock API modules
vi.mock('@/api/modules/user', () => ({
  getUserList: vi.fn().mockResolvedValue({ data: { list: [] } })
}))
vi.mock('@/api/modules/role', () => ({
  getRoleList: vi.fn().mockResolvedValue({ data: { list: [] } })
}))
vi.mock('@/api/modules/config', () => ({
  getConfigList: vi.fn().mockResolvedValue({ data: { list: [] } })
}))
vi.mock('@/api/modules/dict', () => ({
  getDictTypeList: vi.fn().mockResolvedValue({ data: { list: [] } })
}))

// Mock permission store
vi.mock('@/stores/modules/permission', () => ({
  usePermissionStore: vi.fn(() => ({
    dynamicRoutes: [
      { path: '/system/user', meta: { title: '用户管理', icon: 'User' }, children: [] },
      { path: '/system/role', meta: { title: '角色管理', icon: 'UserFilled' }, children: [] }
    ]
  }))
}))

import { filterMenus } from '../useGlobalSearch'

describe('useGlobalSearch', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('filterMenus 匹配菜单标题', () => {
    const routes = [
      { path: '/system/user', meta: { title: '用户管理', icon: 'User' }, children: [] },
      { path: '/system/role', meta: { title: '角色管理' }, children: [] }
    ]
    const results = filterMenus(routes as any, '用户')
    expect(results).toHaveLength(1)
    expect(results[0].title).toBe('用户管理')
  })

  it('filterMenus 空关键词返回空数组', () => {
    const results = filterMenus([], '')
    expect(results).toHaveLength(0)
  })

  it('filterMenus 递归搜索子菜单', () => {
    const routes = [
      {
        path: '/system',
        meta: { title: '系统管理' },
        children: [
          { path: '/system/user', meta: { title: '用户管理' }, children: [] }
        ]
      }
    ]
    const results = filterMenus(routes as any, '用户')
    expect(results).toHaveLength(1)
    expect(results[0].path).toBe('/system/user')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd admin-frontend && npx vitest run src/composables/__tests__/useGlobalSearch.test.ts
```

Expected: FAIL — `filterMenus` 不存在。

- [ ] **Step 3: 实现 useGlobalSearch**

创建 `src/composables/useGlobalSearch.ts`：

```typescript
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import type { RouteRecordRaw } from 'vue-router'
import { usePermissionStore } from '@/stores/modules/permission'
import { getUserList } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'
import { getConfigList } from '@/api/modules/config'
import { getDictTypeList } from '@/api/modules/dict'

export interface SearchResult {
  category: 'menu' | 'user' | 'role' | 'config' | 'dict'
  title: string
  path: string
  icon?: string
}

const HISTORY_KEY = 'search-history'
const MAX_HISTORY = 10
const MAX_PER_CATEGORY = 5

/** 从路由树中模糊匹配菜单 */
export function filterMenus(routes: RouteRecordRaw[], keyword: string): SearchResult[] {
  if (!keyword) return []
  const results: SearchResult[] = []
  const lowerKeyword = keyword.toLowerCase()

  function walk(items: RouteRecordRaw[]) {
    for (const route of items) {
      const title = (route.meta?.title as string) || ''
      if (title.toLowerCase().includes(lowerKeyword)) {
        results.push({
          category: 'menu',
          title,
          path: route.path,
          icon: route.meta?.icon as string
        })
      }
      if (route.children?.length) {
        walk(route.children)
      }
    }
  }

  walk(items)
  return results.slice(0, MAX_PER_CATEGORY)
}

export function useGlobalSearch() {
  const router = useRouter()
  const permissionStore = usePermissionStore()

  const visible = ref(false)
  const keyword = ref('')
  const results = ref<SearchResult[]>([])
  const loading = ref(false)
  const selectedIndex = ref(0)
  const history = ref<string[]>(loadHistory())

  function loadHistory(): string[] {
    try {
      return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    } catch {
      return []
    }
  }

  function saveHistory(query: string) {
    if (!query.trim()) return
    history.value = [query, ...history.value.filter(h => h !== query)].slice(0, MAX_HISTORY)
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value))
  }

  function clearHistory() {
    history.value = []
    localStorage.removeItem(HISTORY_KEY)
  }

  async function search(query: string) {
    if (!query.trim()) {
      results.value = []
      return
    }

    loading.value = true
    selectedIndex.value = 0

    try {
      // 本地搜索：菜单
      const staticRoutes = [{ path: '/dashboard', meta: { title: '仪表盘', icon: 'Monitor' }, children: [] }]
      const menuResults = filterMenus(
        [...staticRoutes, ...permissionStore.dynamicRoutes] as RouteRecordRaw[],
        query
      )

      // API 搜索：并行请求
      const [userRes, roleRes, configRes, dictRes] = await Promise.allSettled([
        getUserList({ keyword: query, page: 1, size: MAX_PER_CATEGORY }),
        getRoleList({ keyword: query, page: 1, size: MAX_PER_CATEGORY }),
        getConfigList({ keyword: query, page: 1, size: MAX_PER_CATEGORY }),
        getDictTypeList({ keyword: query, page: 1, size: MAX_PER_CATEGORY })
      ])

      const apiResults: SearchResult[] = []

      if (userRes.status === 'fulfilled' && userRes.value?.data?.list) {
        userRes.value.data.list.forEach((u: any) => {
          apiResults.push({ category: 'user', title: `${u.username} (${u.nickname || ''})`, path: '/system/user', icon: 'User' })
        })
      }

      if (roleRes.status === 'fulfilled' && roleRes.value?.data?.list) {
        roleRes.value.data.list.forEach((r: any) => {
          apiResults.push({ category: 'role', title: r.roleName, path: '/system/role', icon: 'UserFilled' })
        })
      }

      if (configRes.status === 'fulfilled' && configRes.value?.data?.list) {
        configRes.value.data.list.forEach((c: any) => {
          apiResults.push({ category: 'config', title: `${c.configName}: ${c.configValue}`, path: '/system/config', icon: 'Tools' })
        })
      }

      if (dictRes.status === 'fulfilled' && dictRes.value?.data?.list) {
        dictRes.value.data.list.forEach((d: any) => {
          apiResults.push({ category: 'dict', title: d.dictName, path: '/system/dict', icon: 'Collection' })
        })
      }

      results.value = [...menuResults, ...apiResults]
    } catch {
      results.value = []
    } finally {
      loading.value = false
    }
  }

  const debouncedSearch = useDebounceFn(search, 300)

  watch(keyword, (val) => {
    debouncedSearch(val)
  })

  function open() {
    visible.value = true
    keyword.value = ''
    results.value = []
    selectedIndex.value = 0
  }

  function close() {
    visible.value = false
  }

  function selectResult(result: SearchResult) {
    saveHistory(keyword.value)
    router.push(result.path)
    close()
  }

  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, results.value.length - 1)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
    } else if (e.key === 'Enter' && results.value[selectedIndex.value]) {
      selectResult(results.value[selectedIndex.value])
    }
  }

  // 全局快捷键 Ctrl+K / Cmd+K
  function setupShortcut() {
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        open()
      }
    })
  }

  return {
    visible,
    keyword,
    results,
    loading,
    selectedIndex,
    history,
    open,
    close,
    selectResult,
    handleKeydown,
    clearHistory,
    setupShortcut
  }
}
```

注意：`filterMenus` 函数中有一个 bug — `walk(items)` 应该是 `walk(routes)`。修正：

```typescript
export function filterMenus(routes: RouteRecordRaw[], keyword: string): SearchResult[] {
  if (!keyword) return []
  const results: SearchResult[] = []
  const lowerKeyword = keyword.toLowerCase()

  function walk(items: RouteRecordRaw[]) {
    for (const route of items) {
      const title = (route.meta?.title as string) || ''
      if (title.toLowerCase().includes(lowerKeyword)) {
        results.push({
          category: 'menu',
          title,
          path: route.path,
          icon: route.meta?.icon as string
        })
      }
      if (route.children?.length) {
        walk(route.children)
      }
    }
  }

  walk(routes)
  return results.slice(0, MAX_PER_CATEGORY)
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd admin-frontend && npx vitest run src/composables/__tests__/useGlobalSearch.test.ts
```

Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add admin-frontend/src/composables/useGlobalSearch.ts admin-frontend/src/composables/__tests__/useGlobalSearch.test.ts
git commit -m "feat(search): implement useGlobalSearch composable with debounced multi-source search"
```

---

## Task 14: 全局搜索 — SearchDialog 组件

**Files:**
- Create: `src/components/SearchDialog/index.vue`
- Create: `src/components/SearchDialog/SearchResultItem.vue`
- Modify: `src/components/Layout/Header.vue`

- [ ] **Step 1: 创建 SearchResultItem.vue**

```vue
<template>
  <div class="search-result-item" :class="{ selected }" @click="emit('select')">
    <el-icon class="result-icon"><component :is="icon || 'Search'" /></el-icon>
    <div class="result-content">
      <span class="result-title" v-html="highlightedTitle" />
      <span class="result-category">{{ categoryLabel }}</span>
    </div>
    <el-icon v-if="selected" class="enter-icon"><Right /></el-icon>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SearchResult } from '@/composables/useGlobalSearch'

const props = defineProps<{
  result: SearchResult
  keyword: string
  selected: boolean
}>()

const emit = defineEmits<{ select: [] }>()
const { t } = useI18n()

const icon = computed(() => props.result.icon)

const categoryLabel = computed(() =>
  t(`common.search.category.${props.result.category}`)
)

const highlightedTitle = computed(() => {
  if (!props.keyword) return props.result.title
  const regex = new RegExp(`(${props.keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return props.result.title.replace(regex, '<mark>$1</mark>')
})
</script>

<style scoped lang="scss">
.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 4px;

  &:hover, &.selected {
    background: var(--el-fill-color-light);
  }
}

.result-icon {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  :deep(mark) {
    background: var(--el-color-primary-light-7);
    color: var(--el-color-primary);
    padding: 0 2px;
    border-radius: 2px;
  }
}

.result-category {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  margin-left: 12px;
}

.enter-icon {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
</style>
```

- [ ] **Step 2: 创建 SearchDialog/index.vue**

```vue
<template>
  <el-dialog
    v-model="search.visible.value"
    :show-close="false"
    :close-on-click-modal="true"
    width="600px"
    top="15vh"
    class="search-dialog"
    @opened="inputRef?.focus()"
  >
    <div class="search-header">
      <el-icon class="search-icon"><Search /></el-icon>
      <input
        ref="inputRef"
        v-model="search.keyword.value"
        class="search-input"
        :placeholder="$t('common.search.placeholder')"
        @keydown="search.handleKeydown"
      />
      <kbd class="search-kbd">ESC</kbd>
    </div>

    <el-scrollbar max-height="400px" class="search-body">
      <!-- 搜索历史 -->
      <div v-if="!search.keyword.value && search.history.value.length" class="search-section">
        <div class="section-header">
          <span>{{ $t('common.search.history') }}</span>
          <el-button link size="small" @click="search.clearHistory()">
            {{ $t('common.search.clearHistory') }}
          </el-button>
        </div>
        <div
          v-for="(item, i) in search.history.value"
          :key="i"
          class="history-item"
          @click="search.keyword.value = item"
        >
          <el-icon><Clock /></el-icon>
          <span>{{ item }}</span>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="search.loading.value" class="search-empty">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>

      <!-- 无结果 -->
      <div v-else-if="search.keyword.value && !search.results.value.length" class="search-empty">
        {{ $t('common.search.noResult') }}
      </div>

      <!-- 搜索结果 -->
      <template v-else>
        <template v-for="category in groupedResults" :key="category.name">
          <div class="search-section">
            <div class="section-header">
              <span>{{ $t(`common.search.category.${category.name}`) }}</span>
              <span class="section-count">{{ category.items.length }}</span>
            </div>
            <SearchResultItem
              v-for="(result, i) in category.items"
              :key="result.path + i"
              :result="result"
              :keyword="search.keyword.value"
              :selected="flatIndex(category.name, i) === search.selectedIndex.value"
              @select="search.selectResult(result)"
            />
          </div>
        </template>
      </template>
    </el-scrollbar>

    <div class="search-footer">
      <span><kbd>↑</kbd><kbd>↓</kbd> {{ $t('common.search.placeholder').includes('搜索') ? '导航' : 'Navigate' }}</span>
      <span><kbd>↵</kbd> {{ $t('common.action.confirm') }}</span>
      <span><kbd>ESC</kbd> {{ $t('common.action.close') }}</span>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGlobalSearch } from '@/composables/useGlobalSearch'
import SearchResultItem from './SearchResultItem.vue'
import type { SearchResult } from '@/composables/useGlobalSearch'

const search = useGlobalSearch()
const inputRef = ref<HTMLInputElement>()

interface GroupedCategory {
  name: string
  items: SearchResult[]
}

const groupedResults = computed<GroupedCategory[]>(() => {
  const groups = new Map<string, SearchResult[]>()
  for (const result of search.results.value) {
    const list = groups.get(result.category) || []
    list.push(result)
    groups.set(result.category, list)
  }
  return Array.from(groups.entries()).map(([name, items]) => ({ name, items }))
})

function flatIndex(category: string, indexInCategory: number): number {
  let offset = 0
  for (const group of groupedResults.value) {
    if (group.name === category) return offset + indexInCategory
    offset += group.items.length
  }
  return -1
}

defineExpose({ search })
</script>

<style lang="scss">
.search-dialog {
  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 0; }
}
</style>

<style scoped lang="scss">
.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.search-icon {
  font-size: 20px;
  color: var(--el-text-color-secondary);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--el-text-color-primary);

  &::placeholder { color: var(--el-text-color-placeholder); }
}

.search-kbd {
  font-size: 12px;
  padding: 2px 6px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
}

.search-body {
  padding: 8px;
}

.search-section {
  margin-bottom: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  text-transform: uppercase;
}

.section-count {
  font-size: 11px;
  background: var(--el-fill-color);
  padding: 1px 6px;
  border-radius: 10px;
}

.search-empty {
  padding: 40px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  color: var(--el-text-color-regular);

  &:hover { background: var(--el-fill-color-light); }
}

.search-footer {
  display: flex;
  gap: 16px;
  padding: 8px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);

  kbd {
    font-size: 11px;
    padding: 1px 4px;
    border: 1px solid var(--el-border-color);
    border-radius: 3px;
    margin-right: 2px;
    background: var(--el-fill-color-lighter);
  }
}
</style>
```

- [ ] **Step 3: 集成到 Header.vue — 添加搜索图标和快捷键**

在 Header.vue 的 `<div class="header-right">` 中，通知图标之前添加：

```vue
<el-tooltip content="Ctrl+K" placement="bottom">
  <el-icon class="action-icon" @click="searchDialog?.search.open()"><Search /></el-icon>
</el-tooltip>
```

在模板末尾（`</div>` 之前）添加：

```vue
<SearchDialog ref="searchDialog" />
```

在 `<script setup>` 中添加：

```typescript
import SearchDialog from '@/components/SearchDialog/index.vue'

const searchDialog = ref<InstanceType<typeof SearchDialog>>()

onMounted(() => {
  if (userStore.token) fetchUnreadCount()
  searchDialog.value?.search.setupShortcut()
})
```

- [ ] **Step 4: 验证**

```bash
cd admin-frontend && npm run type-check
```

- [ ] **Step 5: Commit**

```bash
git add admin-frontend/src/components/SearchDialog/ admin-frontend/src/components/Layout/Header.vue
git commit -m "feat(search): create SearchDialog with keyboard navigation and history"
```

---

## Task 15: 最终验证和运行测试

**Files:** 无新增修改

- [ ] **Step 1: 运行全部单元测试**

```bash
cd admin-frontend && npm run test
```

Expected: ALL PASS

- [ ] **Step 2: TypeScript 类型检查**

```bash
cd admin-frontend && npm run type-check
```

Expected: 无错误

- [ ] **Step 3: ESLint 检查**

```bash
cd admin-frontend && npm run lint
```

Expected: 无错误或自动修复

- [ ] **Step 4: 构建验证**

```bash
cd admin-frontend && npm run build
```

Expected: 构建成功

- [ ] **Step 5: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete P1 UX foundation - i18n, dark mode, tabs, global search"
```

---

## 实现顺序总结

| 顺序 | Task | 模块 | 预估步骤 |
|------|------|------|---------|
| 1 | Task 1 | 安装依赖 | 3 |
| 2 | Task 2-3 | M4: 语言包 | 14 |
| 3 | Task 4 | M4: i18n 入口 | 3 |
| 4 | Task 5 | M4: 插件注册 | 4 |
| 5 | Task 6 | M4: Layout i18n | 5 |
| 6 | Task 7 | M4: 页面 i18n | 6 |
| 7 | Task 8 | M1: 主题增强 | 8 |
| 8 | Task 9 | M1: 设置面板 | 3 |
| 9 | Task 10 | M2: TabStore | 5 |
| 10 | Task 11 | M2: TabBar UI | 3 |
| 11 | Task 12 | M2: 集成路由 | 4 |
| 12 | Task 13 | M3: 搜索逻辑 | 5 |
| 13 | Task 14 | M3: 搜索 UI | 5 |
| 14 | Task 15 | 最终验证 | 5 |
