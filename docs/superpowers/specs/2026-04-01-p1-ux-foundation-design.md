# P1 体验基础 — 设计文档

> 日期：2026-04-01
> 阶段：P1（共 4 个阶段）
> 风格参考：Ant Design Pro

## 概述

为后台管理系统新增 4 个前端体验功能，采用全局状态驱动架构（方案一），所有功能共享 Pinia Store 体系，统一持久化到 localStorage。

功能列表：

| 编号 | 功能 | 复杂度 |
|------|------|--------|
| M1 | 暗黑模式 + 主题换肤 | 中 |
| M2 | 多标签页导航 | 高 |
| M3 | 全局搜索 | 中 |
| M4 | 国际化 i18n | 高（改动面广） |

## 技术决策

- **状态管理**：Pinia Store，与现有 `useAppStore`、`useUserStore` 保持一致
- **持久化**：所有 UI 状态持久化到 localStorage，页面加载时恢复
- **后端改动**：无。P1 全部为纯前端改造
- **新增依赖**：`vue-i18n`、`vuedraggable`

---

## M1：暗黑模式 + 主题换肤

### 功能描述

支持明/暗/跟随系统三种模式切换，6 个预设主题色 + 取色器自定义任意颜色。

### 实现方案

- **模式切换**：在 `<html>` 标签上添加 `class="dark"` 切换明暗模式，配合 Element Plus 暗黑模式 CSS 变量覆盖
- **主题色**：6 个预设色（蓝 #409EFF / 绿 #67C23A / 紫 #722ED1 / 红 #F56C6C / 橙 #E6A23C / 青 #13C2C2）+ 取色器自定义。通过动态修改 CSS 变量 `--el-color-primary` 及其衍生色（light-3/5/7/9、dark-2）实现
- **auto 模式**：通过 `window.matchMedia('(prefers-color-scheme: dark)')` 监听系统主题变化，实时跟随
- **设置入口**：复用已有的 `SettingsDrawer.vue` 组件，Ant Design Pro 风格右侧抽屉面板
- **持久化**：`useThemeStore` 将 `mode`（light/dark/auto）和 `primaryColor` 存入 localStorage

### Store 设计

```typescript
// stores/modules/theme.ts
interface ThemeState {
  mode: 'light' | 'dark' | 'auto'   // 主题模式
  primaryColor: string                // 主题色 HEX 值
}
```

### 涉及文件

| 类型 | 文件 |
|------|------|
| 新增 | `stores/modules/theme.ts` |
| 新增 | `assets/styles/dark.scss` |
| 修改 | `components/Layout/SettingsDrawer.vue` |
| 修改 | `components/Layout/index.vue` |
| 修改 | `main.ts`（初始化主题） |

### 不做

- 不改变组件结构，仅通过 CSS 变量覆盖
- 不做每个页面的独立主题配置

---

## M2：多标签页导航

### 功能描述

Header 下方的水平标签栏，支持右键菜单操作、固定标签、拖拽排序、刷新浏览器后恢复状态。

### 实现方案

- **标签栏位置**：Header 下方，与内容区之间有 1px 分隔线
- **路由联动**：Router afterEach 钩子自动添加标签，点击标签切换路由。仪表盘为默认固定标签，不可关闭
- **右键菜单**：关闭当前 / 关闭其他 / 关闭左侧 / 关闭右侧 / 关闭全部（保留固定标签）/ 刷新当前页 / 固定或取消固定
- **拖拽排序**：使用 `vuedraggable`（基于 SortableJS）
- **滚动**：标签超出容器宽度时，左右箭头按钮 + 鼠标滚轮水平滚动
- **刷新当前页**：通过 `router.replace` + `keep-alive` 的 `include/exclude` 控制，不做整页刷新
- **缓存策略**：`<keep-alive>` 缓存已打开标签页的组件状态，关闭标签时移除缓存
- **持久化**：localStorage 存储标签列表，`beforeunload` 事件保存当前状态

### Store 设计

```typescript
// stores/modules/tab.ts
interface Tab {
  path: string          // 路由路径（唯一标识）
  title: string         // 标签标题
  icon?: string         // 图标
  pinned: boolean       // 是否固定
  closable: boolean     // 是否可关闭
}

interface TabState {
  tabs: Tab[]
  activeTab: string     // 当前激活的 path
  cachedViews: string[] // keep-alive 缓存的组件 name 列表
}
```

### 涉及文件

| 类型 | 文件 |
|------|------|
| 新增 | `stores/modules/tab.ts` |
| 新增 | `components/Layout/TabBar.vue` |
| 新增 | `components/Layout/TabContextMenu.vue` |
| 修改 | `components/Layout/index.vue`（插入 TabBar + keep-alive） |
| 新增依赖 | `vuedraggable` |

### 不做

- 不支持标签页拖拽到新窗口
- 不支持标签页分组

---

## M3：全局搜索

### 功能描述

Ctrl+K（Mac Cmd+K）唤起搜索弹窗，支持搜索菜单、用户、角色、配置、字典，按分类分组展示结果。

### 实现方案

- **触发方式**：Ctrl+K / Cmd+K 快捷键，Header 右侧搜索图标点击
- **弹窗样式**：Ant Design Pro 风格居中模态框，顶部搜索输入框 + 下方分类结果列表
- **键盘交互**：上下箭头选中、Enter 确认跳转、Esc 关闭
- **搜索范围与数据源**：
  - **菜单**：从 `usePermissionStore.dynamicRoutes` + 静态路由提取，本地匹配
  - **用户**：`GET /api/users?keyword=xxx`
  - **角色**：`GET /api/roles?keyword=xxx`
  - **配置**：`GET /api/configs?keyword=xxx`
  - **字典**：`GET /api/dicts/types?keyword=xxx`
- **搜索策略**：菜单本地匹配；其余 API 搜索，300ms 防抖
- **结果展示**：按分类分组（菜单/用户/角色/配置/字典），每组最多 5 条，高亮匹配关键词
- **搜索历史**：localStorage 记录最近 10 条，无输入时展示

### 涉及文件

| 类型 | 文件 |
|------|------|
| 新增 | `components/SearchDialog/index.vue` |
| 新增 | `components/SearchDialog/SearchResultItem.vue` |
| 新增 | `composables/useGlobalSearch.ts` |
| 修改 | `components/Layout/Header.vue`（添加搜索图标） |

### 不做

- 不做全文搜索引擎（Elasticsearch），直接用 API 的 LIKE 查询
- 不做搜索结果缓存

---

## M4：国际化 i18n

### 功能描述

中英双语 + 可扩展语言包机制。使用 `vue-i18n` 框架，按模块拆分语言包，懒加载非默认语言。

### 实现方案

- **框架**：`vue-i18n`，createI18n 注册到 Vue 实例
- **默认语言**：zh-CN（中文），内置 en-US（英文）
- **语言包结构**：

```
src/locales/
├── zh-CN/
│   ├── common.json      # 通用：确定、取消、搜索、操作…
│   ├── menu.json         # 菜单名称
│   ├── login.json        # 登录页
│   ├── dashboard.json    # 仪表盘
│   ├── system.json       # 系统管理所有子模块
│   └── profile.json      # 个人中心
├── en-US/
│   └── (同结构)
├── index.ts              # 语言注册 + 懒加载逻辑
└── README.md             # 语言包规范说明
```

- **切换入口**：Header 右侧下拉菜单，显示当前语言标识（中/EN），切换后 localStorage 持久化
- **覆盖范围**：
  - 前端静态文本：菜单、按钮、表单标签、表头、提示消息、校验错误
  - Element Plus 组件内置文本：通过 `el-config-provider` 的 `locale` 属性切换
  - 后端动态数据（菜单名、字典标签等）：不翻译，保持后端返回原始值
- **扩展机制**：新增语种只需添加对应目录 + 在 `index.ts` 注册，无需改业务代码
- **组件用法**：模板 `$t('common.confirm')`，script `const { t } = useI18n()`

### 涉及文件

| 类型 | 文件 |
|------|------|
| 新增 | `src/locales/` 目录及所有语言包文件 |
| 新增 | `src/locales/index.ts` |
| 新增 | `src/locales/README.md` |
| 新增依赖 | `vue-i18n` |
| 修改 | `main.ts`（注册 i18n 插件） |
| 修改 | 所有现有页面和组件（硬编码中文替换为 `$t()` 调用） |
| 修改 | `components/Layout/Header.vue`（添加语言切换下拉） |
| 修改 | `App.vue` 或 `Layout/index.vue`（包裹 `el-config-provider`） |

### 不做

- 不翻译后端返回的动态数据
- 不做 RTL（从右到左）布局支持
- 不做自动翻译工具集成

---

## 模块间依赖关系

```
M1（主题） ← M2（标签页样式需适配明暗主题）
M1（主题） ← M3（搜索弹窗样式需适配明暗主题）
M4（i18n） ← M1/M2/M3（所有新增组件的文本都需 i18n 化）
```

建议实现顺序：M4（i18n）→ M1（主题）→ M2（标签页）→ M3（全局搜索）

理由：i18n 改动面最广且是基础设施，先完成后其余模块直接用 `$t()` 写文本，避免返工。

---

## 新增依赖汇总

| 包名 | 用途 | 模块 |
|------|------|------|
| `vue-i18n` | 国际化框架 | M4 |
| `vuedraggable` | 标签页拖拽排序 | M2 |

---

## 测试策略

- **M1 主题**：单测 `useThemeStore` 的状态切换逻辑和 CSS 变量计算
- **M2 标签页**：单测 `useTabStore` 的增删改查、持久化恢复逻辑
- **M3 搜索**：单测 `useGlobalSearch` 的匹配算法、防抖逻辑
- **M4 i18n**：单测语言切换、fallback 机制；验证所有 key 在中英文语言包中都存在
