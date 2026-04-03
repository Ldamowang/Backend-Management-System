# Widget Marketplace（卡片市场）设计文档

## 概述

为仪表盘添加卡片市场功能，用户可以浏览预置卡片、自定义创建新卡片（图表/统计/便签/链接）、编辑和删除已有卡片。

## 交互架构

采用 **Drawer + 分步向导弹窗** 两层交互模式：

- **Layer 1 — Drawer（480px 右侧抽屉）**：浏览预置卡片、管理已添加卡片
- **Layer 2 — Dialog（600px 向导弹窗）**：自定义创建/编辑卡片的分步表单

## Drawer 结构

### 入口

替换当前编辑模式下的"+ 添加卡片"按钮，改为打开 Drawer。

### Tab 1：预置卡片

- 顶部：「自定义创建卡片」入口按钮（点击打开向导弹窗）
- 搜索栏：按名称搜索
- 分类标签筛选：全部 / 统计 / 图表 / 表格 / 其他
- 卡片列表：每项包含图标、名称、描述、类型标签
  - 未添加：显示「+ 添加」按钮，点击直接添加到仪表盘
  - 已添加：半透明 + 显示「已添加 ✓」

### Tab 2：已添加

- 列出当前仪表盘上所有卡片
- 预置卡片：仅显示「移除」按钮
- 自定义卡片：显示「自定义」标签 + 「编辑」和「移除」按钮
- 点击「编辑」打开向导弹窗，预填当前配置

## 向导弹窗（3 步）

### Step 1：选择卡片类型

4 种类型，2x2 网格卡片选择：

| 类型 | 说明 | 配置项 |
|------|------|--------|
| 图表 | 折线图 / 柱状图 / 饼图 / 面积图 | 标题、图表类型、数据来源、宽度 |
| 统计数字 | 数值指标 | 标题、数据来源、图标、颜色、宽度 |
| 富文本便签 | Markdown 内容 | 标题、Markdown 内容、宽度 |
| 快捷链接 | 自定义导航入口 | 标题、链接列表（名称+路径+图标）、宽度 |

### Step 2：配置内容

根据 Step 1 选择的类型动态渲染不同表单：

**图表类型表单：**
- 卡片标题（必填）
- 图表类型选择：折线图 / 柱状图 / 饼图 / 面积图
- 数据来源：
  - 预置 API：下拉选择已注册的 API 端点
  - 静态数据：手动输入 JSON 格式的 labels + values
- 宽度选择：25%（span-6）/ 50%（span-12）/ 100%（span-24）

**统计数字表单：**
- 卡片标题（必填）
- 数据来源：预置 API 字段选择 / 静态数值
- 图标选择（Element Plus 图标列表）
- 主题颜色选择

**富文本便签表单：**
- 卡片标题（必填）
- Markdown 编辑区（textarea，支持预览）
- 宽度选择

**快捷链接表单：**
- 卡片标题（必填）
- 链接列表（动态增删行）：
  - 每行：名称、路由路径、图标选择、颜色
- 宽度选择

### Step 3：预览确认

- 渲染卡片的实际效果预览
- 显示配置摘要
- 「创建并添加」/ 「保存修改」按钮

## 数据模型

### CustomWidget 接口

```typescript
interface CustomWidget {
  id: string              // 唯一 ID，格式 'custom-{timestamp}'
  name: string            // 卡片标题
  type: 'chart' | 'stat' | 'note' | 'link'
  span: number            // 栅格宽度 6/12/24
  config: ChartConfig | StatConfig | NoteConfig | LinkConfig
  createdAt: number       // 创建时间戳
  updatedAt: number       // 更新时间戳
}

interface ChartConfig {
  chartType: 'line' | 'bar' | 'pie' | 'area'
  dataSource: 'api' | 'static'
  apiEndpoint?: string    // 预置 API 端点 key
  staticData?: {
    labels: string[]
    values: number[]
  }
}

interface StatConfig {
  dataSource: 'api' | 'static'
  apiEndpoint?: string
  apiField?: string
  staticValue?: number
  icon: string
  color: string
}

interface NoteConfig {
  content: string         // Markdown 内容
}

interface LinkConfig {
  links: Array<{
    name: string
    path: string
    icon: string
    color: string
  }>
}
```

### 预置 API 端点注册

```typescript
interface PresetApi {
  key: string             // 唯一标识
  name: string            // 显示名称
  description: string     // 描述
  endpoint: string        // API 路径
  fields: Array<{         // 可用字段
    key: string
    label: string
    type: 'number' | 'string'
  }>
}
```

初始预置 API 列表：
- `dashboard-stats`：仪表盘统计（userCount, roleCount, menuCount, todayLoginCount, totalLoginCount）
- `login-logs`：登录日志列表

## 持久化

- 自定义卡片配置存储在 `localStorage`，key：`custom-widgets`
- 仪表盘布局（activeWidgets）已有持久化机制，自定义卡片 ID 直接加入该数组
- 预置卡片的元数据继续从 `registry.ts` 静态读取

## 需要新增/修改的文件

### 新增文件

| 文件 | 说明 |
|------|------|
| `views/dashboard/components/WidgetMarketDrawer.vue` | 卡片市场 Drawer 主组件 |
| `views/dashboard/components/CreateWidgetWizard.vue` | 自定义创建向导弹窗 |
| `views/dashboard/components/wizard/StepSelectType.vue` | Step 1 类型选择 |
| `views/dashboard/components/wizard/StepConfigure.vue` | Step 2 配置表单 |
| `views/dashboard/components/wizard/StepPreview.vue` | Step 3 预览确认 |
| `views/dashboard/components/widgets/CustomChart.vue` | 自定义图表渲染组件 |
| `views/dashboard/components/widgets/CustomStat.vue` | 自定义统计数字渲染组件 |
| `views/dashboard/components/widgets/CustomNote.vue` | 自定义富文本便签渲染组件 |
| `views/dashboard/components/widgets/CustomLink.vue` | 自定义快捷链接渲染组件 |
| `types/widget.ts` | CustomWidget 等类型定义 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `stores/modules/dashboard.ts` | 添加 customWidgets 管理（CRUD + 持久化） |
| `views/dashboard/components/widgets/registry.ts` | 支持动态注册自定义 widget |
| `views/dashboard/components/DashboardGrid.vue` | 替换"+ 添加卡片"按钮为打开 Drawer |
| `views/dashboard/components/WidgetWrapper.vue` | 支持自定义卡片的编辑按钮 |

## 视觉风格

- Drawer 背景：深色（`#0F172A`），与侧边栏风格一致
- 向导弹窗背景：`#1E293B`
- 主色调：靛蓝 `#6366F1`
- 自定义卡片标签：紫色 `#8B5CF6` 区分预置卡片
- 所有组件遵循现有 Midnight Sapphire 设计系统

## 不在范围内

- 卡片市场的后端 API（纯前端 localStorage 持久化）
- 卡片导入/导出功能
- 卡片分享功能
- 自定义 API URL 输入（仅支持预置 API + 静态数据）
