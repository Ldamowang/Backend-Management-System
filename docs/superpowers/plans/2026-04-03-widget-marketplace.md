# Widget Marketplace 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为仪表盘添加卡片市场 Drawer + 自定义创建向导弹窗，支持浏览预置卡片、自定义创建图表/统计/便签/链接卡片、编辑和删除。

**Architecture:** Drawer（480px）展示预置卡片列表和已添加卡片管理；点击"自定义创建"打开 600px Dialog 分步向导（选类型→配置→预览）。自定义卡片配置存 localStorage，通过动态注册融入现有 widget 体系。

**Tech Stack:** Vue 3 + TypeScript + Element Plus + Pinia + ECharts (vue-echarts) + marked (Markdown)

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `src/types/widget.d.ts` | Create | CustomWidget, ChartConfig, StatConfig, NoteConfig, LinkConfig, PresetApi 类型 |
| `src/views/dashboard/components/widgets/presetApis.ts` | Create | 预置 API 端点注册表 |
| `src/stores/modules/dashboard.ts` | Modify | 添加 customWidgets CRUD + 持久化 + 动态 registry 集成 |
| `src/views/dashboard/components/widgets/registry.ts` | Modify | 导出 getWidgetSpan() 支持自定义卡片 span 查询 |
| `src/views/dashboard/components/widgets/CustomChart.vue` | Create | 自定义图表渲染 |
| `src/views/dashboard/components/widgets/CustomStat.vue` | Create | 自定义统计数字渲染 |
| `src/views/dashboard/components/widgets/CustomNote.vue` | Create | 自定义 Markdown 便签渲染 |
| `src/views/dashboard/components/widgets/CustomLink.vue` | Create | 自定义快捷链接渲染 |
| `src/views/dashboard/components/wizard/StepSelectType.vue` | Create | 向导 Step 1 类型选择 |
| `src/views/dashboard/components/wizard/StepConfigure.vue` | Create | 向导 Step 2 配置表单 |
| `src/views/dashboard/components/wizard/StepPreview.vue` | Create | 向导 Step 3 预览确认 |
| `src/views/dashboard/components/CreateWidgetWizard.vue` | Create | 向导弹窗主组件 |
| `src/views/dashboard/components/WidgetMarketDrawer.vue` | Create | 卡片市场 Drawer |
| `src/views/dashboard/components/DashboardGrid.vue` | Modify | 替换 AddWidgetDialog 为 Drawer |
| `src/views/dashboard/components/WidgetWrapper.vue` | Modify | 自定义卡片编辑按钮 + 动态组件 |

---

## Task 1: 类型定义 + 预置 API 注册表

**Files:**
- Create: `src/types/widget.d.ts`
- Create: `src/views/dashboard/components/widgets/presetApis.ts`

- [ ] **Step 1: 创建 widget 类型定义**

```typescript
// src/types/widget.d.ts

export interface ChartConfig {
  chartType: 'line' | 'bar' | 'pie' | 'area'
  dataSource: 'api' | 'static'
  apiEndpoint?: string
  staticData?: {
    labels: string[]
    values: number[]
  }
}

export interface StatConfig {
  dataSource: 'api' | 'static'
  apiEndpoint?: string
  apiField?: string
  staticValue?: number
  icon: string
  color: string
}

export interface NoteConfig {
  content: string
}

export interface LinkConfig {
  links: Array<{
    name: string
    path: string
    icon: string
    color: string
  }>
}

export type CustomWidgetType = 'chart' | 'stat' | 'note' | 'link'

export interface CustomWidget {
  id: string
  name: string
  type: CustomWidgetType
  span: number
  config: ChartConfig | StatConfig | NoteConfig | LinkConfig
  createdAt: number
  updatedAt: number
}

export interface PresetApi {
  key: string
  name: string
  description: string
  fetcher: () => Promise<Record<string, unknown>>
  fields: Array<{
    key: string
    label: string
    type: 'number' | 'string'
  }>
}
```

- [ ] **Step 2: 创建预置 API 注册表**

```typescript
// src/views/dashboard/components/widgets/presetApis.ts

import type { PresetApi } from '@/types/widget'
import { getStats } from '@/api/modules/dashboard'
import { getLoginLogs } from '@/api/modules/log'

export const presetApis: PresetApi[] = [
  {
    key: 'dashboard-stats',
    name: '仪表盘统计',
    description: '用户数、角色数、菜单数、登录统计',
    fetcher: async () => {
      const res = await getStats()
      return res.data as unknown as Record<string, unknown>
    },
    fields: [
      { key: 'userCount', label: '用户数', type: 'number' },
      { key: 'roleCount', label: '角色数', type: 'number' },
      { key: 'menuCount', label: '菜单数', type: 'number' },
      { key: 'todayLoginCount', label: '今日登录', type: 'number' },
      { key: 'totalLoginCount', label: '总登录次数', type: 'number' }
    ]
  },
  {
    key: 'login-logs',
    name: '登录日志',
    description: '最近登录记录统计',
    fetcher: async () => {
      const res = await getLoginLogs({ page: 1, size: 50 })
      return { total: res.data.total, list: res.data.list } as unknown as Record<string, unknown>
    },
    fields: [
      { key: 'total', label: '日志总数', type: 'number' }
    ]
  }
]

export function getPresetApi(key: string): PresetApi | undefined {
  return presetApis.find(a => a.key === key)
}
```

- [ ] **Step 3: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npx vue-tsc --noEmit 2>&1 | head -20`

Expected: 无类型错误（新文件暂未被引用，不会报错）

- [ ] **Step 4: Commit**

```bash
git add src/types/widget.d.ts src/views/dashboard/components/widgets/presetApis.ts
git commit -m "feat: add CustomWidget types and preset API registry"
```

---

## Task 2: Dashboard Store 扩展

**Files:**
- Modify: `src/stores/modules/dashboard.ts`
- Modify: `src/views/dashboard/components/widgets/registry.ts`

- [ ] **Step 1: 修改 registry.ts 添加 getWidgetSpan 函数**

在 `registry.ts` 文件末尾追加：

```typescript
// 在现有 import 上方添加
import type { CustomWidget } from '@/types/widget'

// 在文件末尾追加
const customWidgetComponents: Record<string, () => Promise<any>> = {
  chart: () => import('./CustomChart.vue'),
  stat: () => import('./CustomStat.vue'),
  note: () => import('./CustomNote.vue'),
  link: () => import('./CustomLink.vue')
}

export function getCustomWidgetComponent(type: string): (() => Promise<any>) | undefined {
  return customWidgetComponents[type]
}

export function resolveWidgetMeta(widgetId: string, customWidgets: CustomWidget[]): WidgetMeta | undefined {
  // 先查预置
  const preset = getWidgetMeta(widgetId)
  if (preset) return preset
  // 再查自定义
  const custom = customWidgets.find(w => w.id === widgetId)
  if (!custom) return undefined
  const comp = getCustomWidgetComponent(custom.type)
  if (!comp) return undefined
  return {
    id: custom.id,
    name: custom.name,
    type: custom.type === 'note' || custom.type === 'link' ? 'shortcut' : custom.type,
    component: comp,
    defaultSpan: custom.span
  }
}
```

- [ ] **Step 2: 扩展 dashboard store**

将 `src/stores/modules/dashboard.ts` 替换为：

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { widgetRegistry, DEFAULT_LAYOUT, getWidgetMeta, resolveWidgetMeta, type WidgetMeta } from '@/views/dashboard/components/widgets/registry'
import type { CustomWidget } from '@/types/widget'

const STORAGE_KEY = 'dashboard-layout'
const CUSTOM_WIDGETS_KEY = 'custom-widgets'

export const useDashboardStore = defineStore('dashboard', () => {
  // ===== Layout =====
  function loadLayout(): string[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed
      }
    } catch { /* ignore */ }
    return [...DEFAULT_LAYOUT]
  }

  function saveLayout() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(activeWidgets.value))
  }

  const activeWidgets = ref<string[]>(loadLayout())
  const editMode = ref(false)

  // ===== Custom Widgets =====
  function loadCustomWidgets(): CustomWidget[] {
    try {
      const raw = localStorage.getItem(CUSTOM_WIDGETS_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed)) return parsed
      }
    } catch { /* ignore */ }
    return []
  }

  function saveCustomWidgets() {
    localStorage.setItem(CUSTOM_WIDGETS_KEY, JSON.stringify(customWidgets.value))
  }

  const customWidgets = ref<CustomWidget[]>(loadCustomWidgets())

  function addCustomWidget(widget: CustomWidget) {
    customWidgets.value = [...customWidgets.value, widget]
    saveCustomWidgets()
    addWidget(widget.id)
  }

  function updateCustomWidget(widgetId: string, updates: Partial<Omit<CustomWidget, 'id'>>) {
    customWidgets.value = customWidgets.value.map(w =>
      w.id === widgetId ? { ...w, ...updates, updatedAt: Date.now() } : w
    )
    saveCustomWidgets()
  }

  function removeCustomWidget(widgetId: string) {
    customWidgets.value = customWidgets.value.filter(w => w.id !== widgetId)
    saveCustomWidgets()
    removeWidget(widgetId)
  }

  function getCustomWidget(widgetId: string): CustomWidget | undefined {
    return customWidgets.value.find(w => w.id === widgetId)
  }

  // ===== Preset Widgets =====
  const availableWidgets = computed<WidgetMeta[]>(() =>
    widgetRegistry.filter(w => !activeWidgets.value.includes(w.id))
  )

  const activeWidgetMetas = computed<WidgetMeta[]>(() =>
    activeWidgets.value
      .map(id => resolveWidgetMeta(id, customWidgets.value))
      .filter((m): m is WidgetMeta => m !== undefined)
  )

  function addWidget(widgetId: string) {
    if (!activeWidgets.value.includes(widgetId)) {
      activeWidgets.value = [...activeWidgets.value, widgetId]
      saveLayout()
    }
  }

  function removeWidget(widgetId: string) {
    activeWidgets.value = activeWidgets.value.filter(id => id !== widgetId)
    saveLayout()
    // 如果是自定义卡片，也从 customWidgets 中删除
    if (widgetId.startsWith('custom-')) {
      customWidgets.value = customWidgets.value.filter(w => w.id !== widgetId)
      saveCustomWidgets()
    }
  }

  function reorderWidgets(newOrder: string[]) {
    activeWidgets.value = [...newOrder]
    saveLayout()
  }

  function resetLayout() {
    activeWidgets.value = [...DEFAULT_LAYOUT]
    localStorage.removeItem(STORAGE_KEY)
  }

  function toggleEditMode() {
    editMode.value = !editMode.value
  }

  return {
    activeWidgets,
    editMode,
    customWidgets,
    availableWidgets,
    activeWidgetMetas,
    addWidget,
    removeWidget,
    reorderWidgets,
    resetLayout,
    toggleEditMode,
    addCustomWidget,
    updateCustomWidget,
    removeCustomWidget,
    getCustomWidget
  }
})
```

- [ ] **Step 3: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -5`

Expected: `✓ built in` (构建成功)

- [ ] **Step 4: Commit**

```bash
git add src/stores/modules/dashboard.ts src/views/dashboard/components/widgets/registry.ts
git commit -m "feat: extend dashboard store with custom widget CRUD and dynamic registry"
```

---

## Task 3: 自定义卡片渲染组件（4 个）

**Files:**
- Create: `src/views/dashboard/components/widgets/CustomChart.vue`
- Create: `src/views/dashboard/components/widgets/CustomStat.vue`
- Create: `src/views/dashboard/components/widgets/CustomNote.vue`
- Create: `src/views/dashboard/components/widgets/CustomLink.vue`

- [ ] **Step 1: 创建 CustomChart.vue**

```vue
<template>
  <div class="chart-container">
    <v-chart v-if="chartOption" :option="chartOption" autoresize />
    <div v-else class="empty-chart">暂无数据</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getPresetApi } from './presetApis'
import type { ChartConfig } from '@/types/widget'

use([CanvasRenderer, LineChart, BarChart, PieChart, TooltipComponent, GridComponent, LegendComponent])

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()
const apiData = ref<Record<string, unknown>>({})

const config = computed<ChartConfig | null>(() => {
  const w = store.getCustomWidget(props.widgetId)
  return w ? w.config as ChartConfig : null
})

const chartOption = computed(() => {
  const c = config.value
  if (!c) return null

  let labels: string[] = []
  let values: number[] = []

  if (c.dataSource === 'static' && c.staticData) {
    labels = c.staticData.labels
    values = c.staticData.values
  } else if (c.dataSource === 'api' && c.apiEndpoint) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      labels = api.fields.filter(f => f.type === 'number').map(f => f.label)
      values = api.fields.filter(f => f.type === 'number').map(f => Number(apiData.value[f.key] ?? 0))
    }
  }

  if (labels.length === 0) return null

  if (c.chartType === 'pie') {
    return {
      tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(99,102,241,0.2)', textStyle: { color: '#E2E8F0' }, borderRadius: 8 },
      legend: { bottom: 0, textStyle: { color: '#64748B', fontSize: 12 } },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: labels.map((l, i) => ({ name: l, value: values[i] }))
      }]
    }
  }

  const seriesType = c.chartType === 'area' ? 'line' : c.chartType
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(99,102,241,0.2)', textStyle: { color: '#E2E8F0' }, borderRadius: 8 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#64748B', fontSize: 12 }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } }, axisLabel: { color: '#94A3B8', fontSize: 12 } },
    series: [{
      type: seriesType, data: values, smooth: true, barWidth: '40%',
      ...(c.chartType === 'area' ? { areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.2)' }, { offset: 1, color: 'rgba(99,102,241,0.01)' }] } } } : {}),
      itemStyle: { borderRadius: c.chartType === 'bar' ? [6, 6, 0, 0] : 0, color: '#6366F1' }
    }]
  }
})

onMounted(async () => {
  const c = config.value
  if (c?.dataSource === 'api' && c.apiEndpoint) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      try { apiData.value = await api.fetcher() } catch { /* ignore */ }
    }
  }
})
</script>

<style scoped>
.chart-container { height: 300px; }
.empty-chart { height: 300px; display: flex; align-items: center; justify-content: center; color: #94A3B8; font-size: 14px; }
</style>
```

- [ ] **Step 2: 创建 CustomStat.vue**

```vue
<template>
  <div class="stat-item">
    <div class="stat-icon" :style="{ backgroundColor: bgColor, color: widget?.config.color || '#6366F1' }">
      <el-icon :size="24"><component :is="widget?.config.icon || 'DataAnalysis'" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ displayValue }}</div>
      <div class="stat-label">{{ widget?.name || '' }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getPresetApi } from './presetApis'
import type { StatConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()
const apiValue = ref<number>(0)

const widget = computed(() => store.getCustomWidget(props.widgetId))
const config = computed<StatConfig | null>(() => widget.value ? widget.value.config as StatConfig : null)

const bgColor = computed(() => {
  const color = config.value?.color || '#6366F1'
  return color + '1A' // 10% opacity hex
})

const displayValue = computed(() => {
  const c = config.value
  if (!c) return '—'
  if (c.dataSource === 'static') return (c.staticValue ?? 0).toLocaleString()
  return apiValue.value.toLocaleString()
})

onMounted(async () => {
  const c = config.value
  if (c?.dataSource === 'api' && c.apiEndpoint && c.apiField) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      try {
        const data = await api.fetcher()
        apiValue.value = Number(data[c.apiField] ?? 0)
      } catch { /* ignore */ }
    }
  }
})
</script>

<style scoped lang="scss">
.stat-item { display: flex; align-items: center; padding: 4px 0; }
.stat-icon {
  width: 52px; height: 52px; border-radius: $border-radius-base;
  display: flex; align-items: center; justify-content: center; margin-right: 18px;
}
.stat-value {
  font-family: $font-family-heading; font-size: 28px; font-weight: 700;
  color: $text-primary; line-height: 1.2; letter-spacing: -0.02em;
}
.stat-label { font-size: $font-size-sm; color: $text-secondary; margin-top: 4px; font-weight: 500; }
</style>
```

- [ ] **Step 3: 创建 CustomNote.vue**

```vue
<template>
  <div class="note-content" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import type { NoteConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()

const config = computed<NoteConfig | null>(() => {
  const w = store.getCustomWidget(props.widgetId)
  return w ? w.config as NoteConfig : null
})

const renderedContent = computed(() => {
  const content = config.value?.content || ''
  // 简单 Markdown 渲染：标题、加粗、斜体、列表、换行
  return content
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    .replace(/\n/g, '<br>')
})
</script>

<style scoped lang="scss">
.note-content {
  font-size: $font-size-base;
  line-height: 1.7;
  color: $text-regular;
  min-height: 60px;

  :deep(h2) { font-size: $font-size-xl; font-weight: 700; margin-bottom: 8px; color: $text-primary; }
  :deep(h3) { font-size: $font-size-lg; font-weight: 600; margin-bottom: 6px; color: $text-primary; }
  :deep(h4) { font-size: $font-size-md; font-weight: 600; margin-bottom: 4px; color: $text-primary; }
  :deep(strong) { font-weight: 600; color: $text-primary; }
  :deep(ul) { padding-left: 20px; margin: 8px 0; }
  :deep(li) { margin: 4px 0; }
}
</style>
```

- [ ] **Step 4: 创建 CustomLink.vue**

```vue
<template>
  <div class="link-grid">
    <button
      v-for="link in links"
      :key="link.path"
      class="link-item"
      @click="router.push(link.path)"
    >
      <div class="link-icon" :style="{ background: link.color || '#6366F1' }">
        <el-icon :size="20" color="#fff"><component :is="link.icon || 'Link'" /></el-icon>
      </div>
      <span class="link-label">{{ link.name }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/modules/dashboard'
import type { LinkConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const router = useRouter()
const store = useDashboardStore()

const links = computed(() => {
  const w = store.getCustomWidget(props.widgetId)
  if (!w) return []
  return (w.config as LinkConfig).links || []
})
</script>

<style scoped lang="scss">
.link-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.link-item {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 20px 12px; border-radius: $border-radius-base; cursor: pointer;
  border: 1px solid transparent; background: transparent; transition: all $transition-base;
  &:hover {
    background: $border-lighter; border-color: $border-light; transform: translateY(-2px);
    .link-icon { transform: scale(1.1); }
  }
}
.link-icon {
  width: 44px; height: 44px; border-radius: $border-radius-base;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 3px 8px rgba(0,0,0,0.08); transition: all $transition-base;
}
.link-label { font-size: $font-size-sm; font-weight: 500; color: $text-regular; }
</style>
```

- [ ] **Step 5: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -5`

Expected: `✓ built in`

- [ ] **Step 6: Commit**

```bash
git add src/views/dashboard/components/widgets/Custom*.vue
git commit -m "feat: add custom widget renderers (chart, stat, note, link)"
```

---

## Task 4: 向导弹窗（3 步）

**Files:**
- Create: `src/views/dashboard/components/wizard/StepSelectType.vue`
- Create: `src/views/dashboard/components/wizard/StepConfigure.vue`
- Create: `src/views/dashboard/components/wizard/StepPreview.vue`
- Create: `src/views/dashboard/components/CreateWidgetWizard.vue`

- [ ] **Step 1: 创建 StepSelectType.vue**

```vue
<template>
  <div>
    <div class="step-title">选择卡片类型</div>
    <div class="step-desc">选择你想创建的卡片类型</div>
    <div class="type-grid">
      <div
        v-for="t in types"
        :key="t.value"
        class="type-card"
        :class="{ active: modelValue === t.value }"
        @click="$emit('update:modelValue', t.value)"
      >
        <div class="type-emoji">{{ t.emoji }}</div>
        <div class="type-name">{{ t.label }}</div>
        <div class="type-desc">{{ t.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CustomWidgetType } from '@/types/widget'

defineProps<{ modelValue: CustomWidgetType | '' }>()
defineEmits<{ 'update:modelValue': [value: CustomWidgetType] }>()

const types = [
  { value: 'chart' as const, emoji: '📈', label: '图表', desc: '折线图 / 柱状图 / 饼图 / 面积图' },
  { value: 'stat' as const, emoji: '🔢', label: '统计数字', desc: '数值指标 + 趋势' },
  { value: 'note' as const, emoji: '📝', label: '富文本便签', desc: 'Markdown / 公告 / 备注' },
  { value: 'link' as const, emoji: '🔗', label: '快捷链接', desc: '自定义导航入口' }
]
</script>

<style scoped lang="scss">
.step-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; color: $text-primary; }
.step-desc { font-size: $font-size-sm; color: $text-secondary; margin: 4px 0 20px; }
.type-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.type-card {
  padding: 20px; border-radius: $border-radius-lg; border: 1px solid $border-light;
  text-align: center; cursor: pointer; transition: all $transition-base;
  &:hover { border-color: $primary-color; background: rgba(99, 102, 241, 0.04); }
  &.active { border-color: $primary-color; background: rgba(99, 102, 241, 0.08); box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); }
}
.type-emoji { font-size: 32px; margin-bottom: 8px; }
.type-name { font-family: $font-family-heading; font-size: $font-size-md; font-weight: 600; }
.type-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 4px; }
</style>
```

- [ ] **Step 2: 创建 StepConfigure.vue**

```vue
<template>
  <div>
    <!-- 公共字段：标题 + 宽度 -->
    <el-form label-position="top" :model="form">
      <el-form-item label="卡片标题" required>
        <el-input v-model="form.name" placeholder="输入卡片名称" />
      </el-form-item>

      <el-form-item label="宽度">
        <el-radio-group v-model="form.span">
          <el-radio-button :value="6">25%</el-radio-button>
          <el-radio-button :value="12">50%</el-radio-button>
          <el-radio-button :value="24">100%</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 图表配置 -->
      <template v-if="widgetType === 'chart'">
        <el-form-item label="图表类型">
          <el-radio-group v-model="(form.config as ChartConfig).chartType">
            <el-radio-button value="line">折线图</el-radio-button>
            <el-radio-button value="bar">柱状图</el-radio-button>
            <el-radio-button value="pie">饼图</el-radio-button>
            <el-radio-button value="area">面积图</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-radio-group v-model="(form.config as ChartConfig).dataSource">
            <el-radio-button value="api">预置 API</el-radio-button>
            <el-radio-button value="static">静态数据</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="(form.config as ChartConfig).dataSource === 'api'" label="API 端点">
          <el-select v-model="(form.config as ChartConfig).apiEndpoint" placeholder="选择数据源">
            <el-option v-for="api in presetApis" :key="api.key" :label="api.name" :value="api.key">
              <div>{{ api.name }}</div>
              <div style="font-size:12px;color:#94A3B8;">{{ api.description }}</div>
            </el-option>
          </el-select>
        </el-form-item>
        <template v-if="(form.config as ChartConfig).dataSource === 'static'">
          <el-form-item label="标签（逗号分隔）">
            <el-input v-model="staticLabelsStr" placeholder="如：一月,二月,三月" />
          </el-form-item>
          <el-form-item label="数值（逗号分隔）">
            <el-input v-model="staticValuesStr" placeholder="如：100,200,300" />
          </el-form-item>
        </template>
      </template>

      <!-- 统计配置 -->
      <template v-if="widgetType === 'stat'">
        <el-form-item label="数据来源">
          <el-radio-group v-model="(form.config as StatConfig).dataSource">
            <el-radio-button value="api">预置 API</el-radio-button>
            <el-radio-button value="static">静态数值</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'api'" label="API 端点">
          <el-select v-model="(form.config as StatConfig).apiEndpoint" placeholder="选择数据源">
            <el-option v-for="api in presetApis" :key="api.key" :label="api.name" :value="api.key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'api' && (form.config as StatConfig).apiEndpoint" label="字段">
          <el-select v-model="(form.config as StatConfig).apiField" placeholder="选择字段">
            <el-option v-for="f in selectedApiFields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'static'" label="静态数值">
          <el-input-number v-model="(form.config as StatConfig).staticValue" :min="0" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select v-model="(form.config as StatConfig).icon" placeholder="选择图标">
            <el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="(form.config as StatConfig).color" />
        </el-form-item>
      </template>

      <!-- 便签配置 -->
      <template v-if="widgetType === 'note'">
        <el-form-item label="内容（支持 Markdown）">
          <el-input v-model="(form.config as NoteConfig).content" type="textarea" :rows="8" placeholder="输入 Markdown 内容..." />
        </el-form-item>
      </template>

      <!-- 链接配置 -->
      <template v-if="widgetType === 'link'">
        <div class="link-header">
          <span class="step-desc">链接列表</span>
          <el-button size="small" @click="addLinkRow">+ 添加链接</el-button>
        </div>
        <div v-for="(link, idx) in (form.config as LinkConfig).links" :key="idx" class="link-row">
          <el-input v-model="link.name" placeholder="名称" style="width:25%" />
          <el-input v-model="link.path" placeholder="路由路径" style="width:30%" />
          <el-select v-model="link.icon" placeholder="图标" style="width:20%">
            <el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon" />
          </el-select>
          <el-color-picker v-model="link.color" size="small" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="removeLinkRow(idx)" />
        </div>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { presetApis, getPresetApi } from '../widgets/presetApis'
import type { CustomWidgetType, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  widgetType: CustomWidgetType
  form: { name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }
}>()

const iconOptions = ['User', 'UserFilled', 'Monitor', 'DataAnalysis', 'TrendCharts', 'PieChart', 'Bell', 'Star', 'Setting', 'Link', 'Document', 'Folder', 'CircleCheck', 'Menu']

const selectedApiFields = computed(() => {
  const c = props.form.config as StatConfig
  if (c.apiEndpoint) {
    return getPresetApi(c.apiEndpoint)?.fields.filter(f => f.type === 'number') || []
  }
  return []
})

// 静态数据双向绑定辅助
const staticLabelsStr = computed({
  get: () => (props.form.config as ChartConfig).staticData?.labels.join(',') || '',
  set: (v: string) => {
    const c = props.form.config as ChartConfig
    if (!c.staticData) c.staticData = { labels: [], values: [] }
    c.staticData.labels = v.split(',').map(s => s.trim()).filter(Boolean)
  }
})

const staticValuesStr = computed({
  get: () => (props.form.config as ChartConfig).staticData?.values.join(',') || '',
  set: (v: string) => {
    const c = props.form.config as ChartConfig
    if (!c.staticData) c.staticData = { labels: [], values: [] }
    c.staticData.values = v.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  }
})

function addLinkRow() {
  const c = props.form.config as LinkConfig
  c.links.push({ name: '', path: '', icon: 'Link', color: '#6366F1' })
}

function removeLinkRow(idx: number) {
  const c = props.form.config as LinkConfig
  c.links.splice(idx, 1)
}
</script>

<style scoped lang="scss">
.step-desc { font-size: $font-size-sm; color: $text-secondary; }
.link-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.link-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
</style>
```

- [ ] **Step 3: 创建 StepPreview.vue**

```vue
<template>
  <div>
    <div class="step-title">预览确认</div>
    <div class="step-desc">确认卡片配置并预览效果</div>

    <div class="preview-summary">
      <div class="summary-row"><span class="label">名称</span><span>{{ form.name }}</span></div>
      <div class="summary-row"><span class="label">类型</span><span>{{ typeLabel }}</span></div>
      <div class="summary-row"><span class="label">宽度</span><span>{{ spanLabel }}</span></div>
    </div>

    <div class="preview-card">
      <el-card shadow="never">
        <template #header><span style="font-weight:600;">{{ form.name }}</span></template>
        <div class="preview-placeholder">
          <span style="font-size:40px;">{{ typeEmoji }}</span>
          <p>卡片将在添加后渲染实际内容</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CustomWidgetType, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  widgetType: CustomWidgetType
  form: { name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }
}>()

const typeMap: Record<string, { label: string; emoji: string }> = {
  chart: { label: '图表', emoji: '📈' },
  stat: { label: '统计数字', emoji: '🔢' },
  note: { label: '富文本便签', emoji: '📝' },
  link: { label: '快捷链接', emoji: '🔗' }
}

const typeLabel = computed(() => typeMap[props.widgetType]?.label || '')
const typeEmoji = computed(() => typeMap[props.widgetType]?.emoji || '')
const spanLabel = computed(() => ({ 6: '25%', 12: '50%', 24: '100%' }[props.form.span] || '50%'))
</script>

<style scoped lang="scss">
.step-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; color: $text-primary; }
.step-desc { font-size: $font-size-sm; color: $text-secondary; margin: 4px 0 20px; }
.preview-summary { margin-bottom: 20px; }
.summary-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid $border-light; font-size: $font-size-base; }
.label { color: $text-secondary; font-weight: 500; }
.preview-card { margin-top: 16px; }
.preview-placeholder { text-align: center; padding: 24px; color: $text-secondary; font-size: $font-size-sm; }
</style>
```

- [ ] **Step 4: 创建 CreateWidgetWizard.vue**

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="isEdit ? '编辑自定义卡片' : '创建自定义卡片'"
    width="600px"
    :close-on-click-modal="false"
  >
    <!-- Steps 指示器 -->
    <div class="wizard-steps">
      <div v-for="s in steps" :key="s.step" class="wizard-step" :class="{ active: step === s.step, done: step > s.step }">
        <div class="step-circle">{{ step > s.step ? '✓' : s.step }}</div>
        <span class="step-label">{{ s.label }}</span>
      </div>
    </div>

    <!-- Step 内容 -->
    <StepSelectType v-if="step === 1" v-model="widgetType" />
    <StepConfigure v-if="step === 2" :widget-type="widgetType" :form="form" />
    <StepPreview v-if="step === 3" :widget-type="widgetType" :form="form" />

    <template #footer>
      <el-button v-if="step > 1" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" :disabled="!canNext" @click="step++">下一步</el-button>
      <el-button v-if="step === 3" type="primary" @click="handleSubmit">
        {{ isEdit ? '保存修改' : '创建并添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import StepSelectType from './wizard/StepSelectType.vue'
import StepConfigure from './wizard/StepConfigure.vue'
import StepPreview from './wizard/StepPreview.vue'
import type { CustomWidgetType, CustomWidget, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  modelValue: boolean
  editWidget?: CustomWidget | null
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: []
  updated: []
}>()

const store = useDashboardStore()
const step = ref(1)

const isEdit = computed(() => !!props.editWidget)

const widgetType = ref<CustomWidgetType | ''>('')

const defaultConfigs: Record<CustomWidgetType, () => ChartConfig | StatConfig | NoteConfig | LinkConfig> = {
  chart: () => ({ chartType: 'line', dataSource: 'api', apiEndpoint: '', staticData: { labels: [], values: [] } }),
  stat: () => ({ dataSource: 'api', apiEndpoint: '', apiField: '', staticValue: 0, icon: 'DataAnalysis', color: '#6366F1' }),
  note: () => ({ content: '' }),
  link: () => ({ links: [{ name: '', path: '', icon: 'Link', color: '#6366F1' }] })
}

const form = ref<{ name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }>({
  name: '',
  span: 12,
  config: { chartType: 'line', dataSource: 'api' } as ChartConfig
})

// 类型切换时重置 config
watch(widgetType, (t) => {
  if (t && !isEdit.value) {
    form.value.config = defaultConfigs[t]()
  }
})

// 编辑模式：预填数据
watch(() => props.modelValue, (visible) => {
  if (visible && props.editWidget) {
    widgetType.value = props.editWidget.type
    form.value = {
      name: props.editWidget.name,
      span: props.editWidget.span,
      config: JSON.parse(JSON.stringify(props.editWidget.config))
    }
    step.value = 2 // 编辑时跳过类型选择
  } else if (visible) {
    step.value = 1
    widgetType.value = ''
    form.value = { name: '', span: 12, config: { chartType: 'line', dataSource: 'api' } as ChartConfig }
  }
})

const canNext = computed(() => {
  if (step.value === 1) return widgetType.value !== ''
  if (step.value === 2) return form.value.name.trim() !== ''
  return true
})

const steps = [
  { step: 1, label: '选择类型' },
  { step: 2, label: '配置内容' },
  { step: 3, label: '预览确认' }
]

function handleSubmit() {
  if (isEdit.value && props.editWidget) {
    store.updateCustomWidget(props.editWidget.id, {
      name: form.value.name,
      type: widgetType.value as CustomWidgetType,
      span: form.value.span,
      config: JSON.parse(JSON.stringify(form.value.config))
    })
    emit('updated')
  } else {
    const widget: CustomWidget = {
      id: `custom-${Date.now()}`,
      name: form.value.name,
      type: widgetType.value as CustomWidgetType,
      span: form.value.span,
      config: JSON.parse(JSON.stringify(form.value.config)),
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
    store.addCustomWidget(widget)
    emit('created')
  }
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.wizard-steps {
  display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 28px;
}
.wizard-step {
  display: flex; align-items: center; gap: 8px;
  &:not(:last-child)::after { content: ''; width: 40px; height: 1px; background: $border-color; margin: 0 12px; }
}
.step-circle {
  width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: $font-size-sm; font-weight: 700; background: $border-light; color: $text-secondary; transition: all $transition-base;
  .active & { background: $primary-color; color: #fff; }
  .done & { background: $success-color; color: #fff; }
}
.step-label {
  font-size: $font-size-sm; color: $text-secondary; font-weight: 500;
  .active & { color: $primary-color; font-weight: 600; }
}
</style>
```

- [ ] **Step 5: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -5`

Expected: `✓ built in`

- [ ] **Step 6: Commit**

```bash
git add src/views/dashboard/components/wizard/ src/views/dashboard/components/CreateWidgetWizard.vue
git commit -m "feat: add create widget wizard with 3-step flow"
```

---

## Task 5: 卡片市场 Drawer

**Files:**
- Create: `src/views/dashboard/components/WidgetMarketDrawer.vue`

- [ ] **Step 1: 创建 WidgetMarketDrawer.vue**

```vue
<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title=""
    size="480px"
    :with-header="false"
  >
    <div class="drawer-content">
      <!-- Header -->
      <div class="drawer-header">
        <div>
          <h3 class="drawer-title">卡片市场</h3>
          <p class="drawer-desc">浏览和管理仪表盘卡片</p>
        </div>
        <el-button :icon="Close" circle size="small" @click="$emit('update:modelValue', false)" />
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="drawer-tabs">
        <el-tab-pane label="预置卡片" name="preset">
          <!-- 自定义创建入口 -->
          <div class="create-entry" @click="wizardVisible = true">
            <div class="create-icon">✨</div>
            <div class="create-text">
              <div class="create-title">自定义创建卡片</div>
              <div class="create-desc">图表 · 统计 · 便签 · 链接</div>
            </div>
          </div>

          <!-- 搜索 -->
          <el-input v-model="searchQuery" placeholder="搜索卡片..." prefix-icon="Search" clearable class="search-input" />

          <!-- 分类筛选 -->
          <div class="filter-tags">
            <el-check-tag v-for="tag in filterTags" :key="tag.value" :checked="activeFilter === tag.value" @change="activeFilter = tag.value">
              {{ tag.label }}
            </el-check-tag>
          </div>

          <!-- 卡片列表 -->
          <div class="widget-list">
            <div v-for="widget in filteredWidgets" :key="widget.id" class="widget-item" :class="{ added: isAdded(widget.id) }">
              <div class="widget-icon" :style="{ background: typeGradient(widget.type) }">
                {{ typeEmoji(widget.type) }}
              </div>
              <div class="widget-info">
                <div class="widget-name">{{ widget.name }}</div>
                <div class="widget-type-tag">
                  <el-tag size="small" :type="typeTagColor(widget.type)" effect="plain">{{ typeLabel(widget.type) }}</el-tag>
                </div>
              </div>
              <el-button v-if="!isAdded(widget.id)" type="success" size="small" plain @click="store.addWidget(widget.id)">+ 添加</el-button>
              <span v-else class="added-label">已添加 ✓</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="added">
          <template #label>
            已添加 <el-badge :value="store.activeWidgets.length" :max="99" class="tab-badge" />
          </template>

          <div class="widget-list">
            <div v-for="widgetId in store.activeWidgets" :key="widgetId" class="widget-item">
              <div class="widget-icon" :style="{ background: resolveGradient(widgetId) }">
                {{ resolveEmoji(widgetId) }}
              </div>
              <div class="widget-info">
                <div class="widget-name">
                  {{ resolveName(widgetId) }}
                  <el-tag v-if="widgetId.startsWith('custom-')" size="small" type="warning" effect="plain" class="custom-tag">自定义</el-tag>
                </div>
                <div class="widget-type-tag">
                  <el-tag size="small" effect="plain">{{ resolveTypeLabel(widgetId) }}</el-tag>
                </div>
              </div>
              <div class="action-btns">
                <el-button v-if="widgetId.startsWith('custom-')" size="small" type="primary" plain @click="editWidget(widgetId)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="store.removeWidget(widgetId)">移除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 向导弹窗 -->
    <CreateWidgetWizard
      v-model="wizardVisible"
      :edit-widget="editingWidget"
      @created="onWizardDone"
      @updated="onWizardDone"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { widgetRegistry, getWidgetMeta, resolveWidgetMeta } from '../widgets/registry'
import CreateWidgetWizard from './CreateWidgetWizard.vue'
import type { CustomWidget } from '@/types/widget'

defineProps<{ modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()

const store = useDashboardStore()
const activeTab = ref('preset')
const searchQuery = ref('')
const activeFilter = ref('all')
const wizardVisible = ref(false)
const editingWidget = ref<CustomWidget | null>(null)

const filterTags = [
  { label: '全部', value: 'all' },
  { label: '统计', value: 'stat' },
  { label: '图表', value: 'chart' },
  { label: '表格', value: 'table' },
  { label: '其他', value: 'shortcut' }
]

const filteredWidgets = computed(() => {
  let list = [...widgetRegistry]
  if (activeFilter.value !== 'all') {
    list = list.filter(w => w.type === activeFilter.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(w => w.name.toLowerCase().includes(q))
  }
  return list
})

function isAdded(id: string): boolean {
  return store.activeWidgets.includes(id)
}

function typeEmoji(type: string): string {
  return { stat: '📊', chart: '📈', table: '📋', shortcut: '🔗' }[type] || '📦'
}

function typeGradient(type: string): string {
  const map: Record<string, string> = {
    stat: 'linear-gradient(135deg, #6366F1, #818CF8)',
    chart: 'linear-gradient(135deg, #10B981, #34D399)',
    table: 'linear-gradient(135deg, #F59E0B, #FBBF24)',
    shortcut: 'linear-gradient(135deg, #EF4444, #F87171)'
  }
  return map[type] || map.stat
}

function typeLabel(type: string): string {
  return { stat: '统计', chart: '图表', table: '表格', shortcut: '快捷' }[type] || type
}

function typeTagColor(type: string): '' | 'success' | 'warning' | 'danger' {
  return { stat: '' as const, chart: 'success' as const, table: 'warning' as const, shortcut: 'danger' as const }[type] || ''
}

function resolveName(id: string): string {
  const meta = resolveWidgetMeta(id, store.customWidgets)
  return meta?.name || id
}

function resolveTypeLabel(id: string): string {
  if (id.startsWith('custom-')) {
    const w = store.getCustomWidget(id)
    return { chart: '图表', stat: '统计', note: '便签', link: '链接' }[w?.type || ''] || ''
  }
  const meta = getWidgetMeta(id)
  return meta ? typeLabel(meta.type) : ''
}

function resolveEmoji(id: string): string {
  if (id.startsWith('custom-')) {
    const w = store.getCustomWidget(id)
    return { chart: '📈', stat: '🔢', note: '📝', link: '🔗' }[w?.type || ''] || '📦'
  }
  const meta = getWidgetMeta(id)
  return meta ? typeEmoji(meta.type) : '📦'
}

function resolveGradient(id: string): string {
  if (id.startsWith('custom-')) return 'linear-gradient(135deg, #8B5CF6, #A78BFA)'
  const meta = getWidgetMeta(id)
  return meta ? typeGradient(meta.type) : typeGradient('stat')
}

function editWidget(id: string) {
  editingWidget.value = store.getCustomWidget(id) || null
  wizardVisible.value = true
}

function onWizardDone() {
  editingWidget.value = null
}
</script>

<style scoped lang="scss">
.drawer-content { padding: 0; }
.drawer-header {
  display: flex; justify-content: space-between; align-items: flex-start; padding: 24px 24px 0;
}
.drawer-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; margin: 0; }
.drawer-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 2px; }
.drawer-tabs { padding: 0 24px; }

.create-entry {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  border: 1px dashed $primary-color; border-radius: $border-radius-base;
  background: rgba(99, 102, 241, 0.04); cursor: pointer; margin-bottom: 16px;
  transition: all $transition-base;
  &:hover { background: rgba(99, 102, 241, 0.08); }
}
.create-icon { font-size: 24px; }
.create-title { font-family: $font-family-heading; font-weight: 600; color: $primary-color; }
.create-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 2px; }

.search-input { margin-bottom: 12px; }

.filter-tags { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }

.widget-list { display: flex; flex-direction: column; gap: 8px; }
.widget-item {
  display: flex; align-items: center; gap: 14px; padding: 12px 16px;
  border: 1px solid $border-light; border-radius: $border-radius-base; transition: all $transition-base;
  &:hover { border-color: $border-color; }
  &.added { opacity: 0.45; }
}
.widget-icon {
  width: 40px; height: 40px; border-radius: $border-radius-sm; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
}
.widget-info { flex: 1; min-width: 0; }
.widget-name { font-weight: 600; font-size: $font-size-sm; display: flex; align-items: center; gap: 6px; }
.widget-type-tag { margin-top: 4px; }
.added-label { font-size: $font-size-xs; color: $text-secondary; white-space: nowrap; }
.custom-tag { margin-left: 4px; }
.action-btns { display: flex; gap: 6px; flex-shrink: 0; }
.tab-badge { margin-left: 6px; }
</style>
```

- [ ] **Step 2: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -5`

Expected: `✓ built in`

- [ ] **Step 3: Commit**

```bash
git add src/views/dashboard/components/WidgetMarketDrawer.vue
git commit -m "feat: add widget marketplace drawer with search, filter, and management"
```

---

## Task 6: 集成到 DashboardGrid + WidgetWrapper

**Files:**
- Modify: `src/views/dashboard/components/DashboardGrid.vue`
- Modify: `src/views/dashboard/components/WidgetWrapper.vue`

- [ ] **Step 1: 修改 DashboardGrid.vue — 替换 AddWidgetDialog 为 Drawer**

将 `DashboardGrid.vue` 完整替换为：

```vue
<template>
  <div>
    <div class="grid-toolbar">
      <el-button v-if="!store.editMode" type="primary" plain size="small" class="edit-btn" @click="store.toggleEditMode()">
        编辑布局
      </el-button>
      <template v-else>
        <el-button type="success" size="small" @click="store.toggleEditMode()">完成编辑</el-button>
        <el-button size="small" @click="drawerVisible = true">卡片市场</el-button>
        <el-button size="small" type="warning" @click="store.resetLayout()">恢复默认</el-button>
      </template>
    </div>

    <draggable
      v-model="store.activeWidgets"
      :item-key="(id: string) => id"
      :disabled="!store.editMode"
      :animation="200"
      ghost-class="widget-ghost"
      drag-class="widget-drag"
      class="dashboard-grid"
      @end="onDragEnd"
    >
      <template #item="{ element: widgetId }">
        <div
          class="grid-item animate-fade-in-up"
          :class="[`stagger-${store.activeWidgets.indexOf(widgetId) + 1}`, spanClass(widgetId)]"
        >
          <WidgetWrapper
            :widget-id="widgetId"
            :edit-mode="store.editMode"
            @remove="store.removeWidget(widgetId)"
            @edit="openEditWizard(widgetId)"
          />
        </div>
      </template>
    </draggable>

    <WidgetMarketDrawer v-model="drawerVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import draggable from 'vuedraggable'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { resolveWidgetMeta } from './widgets/registry'
import WidgetWrapper from './WidgetWrapper.vue'
import WidgetMarketDrawer from './WidgetMarketDrawer.vue'

const store = useDashboardStore()
const drawerVisible = ref(false)

function spanClass(widgetId: string): string {
  const meta = resolveWidgetMeta(widgetId, store.customWidgets)
  const span = meta?.defaultSpan ?? 12
  return `span-${span}`
}

function onDragEnd() {
  store.reorderWidgets([...store.activeWidgets])
}

function openEditWizard(widgetId: string) {
  // Drawer 内部处理编辑
  drawerVisible.value = true
}
</script>

<style scoped lang="scss">
.grid-toolbar { display: flex; gap: 8px; margin-bottom: 20px; }
.edit-btn { border-radius: $border-radius-sm; }
.dashboard-grid { display: flex; flex-wrap: wrap; gap: 20px; }
.grid-item {
  min-height: 0;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  &.span-6  { width: calc(25% - 15px); }
  &.span-8  { width: calc(33.333% - 13.333px); }
  &.span-12 { width: calc(50% - 10px); }
  &.span-16 { width: calc(66.666% - 6.666px); }
  &.span-24 { width: 100%; }
}
.widget-ghost { opacity: 0.4; border-radius: $border-radius-lg; :deep(.el-card) { border: 2px dashed $primary-color; background: rgba(99, 102, 241, 0.04); } }
.widget-drag { transform: rotate(1.5deg) scale(1.02); box-shadow: $box-shadow-hover; z-index: 100; }
</style>
```

- [ ] **Step 2: 修改 WidgetWrapper.vue — 支持自定义卡片编辑按钮**

将 `WidgetWrapper.vue` 完整替换为：

```vue
<template>
  <el-card class="widget-wrapper" :class="{ 'edit-mode': editMode }" shadow="never">
    <template #header v-if="meta">
      <div class="widget-header">
        <span v-if="editMode" class="drag-handle">&#x2807;</span>
        <span class="widget-title">{{ meta.name }}</span>
        <div v-if="editMode" class="header-actions">
          <el-button
            v-if="isCustom"
            type="primary"
            :icon="EditPen"
            circle
            size="small"
            @click="$emit('edit')"
          />
          <el-button
            type="danger"
            :icon="Close"
            circle
            size="small"
            @click="$emit('remove')"
          />
        </div>
      </div>
    </template>
    <component :is="asyncComponent" :widget-id="widgetId" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { Close, EditPen } from '@element-plus/icons-vue'
import { resolveWidgetMeta } from './widgets/registry'
import { useDashboardStore } from '@/stores/modules/dashboard'

const props = defineProps<{
  widgetId: string
  editMode: boolean
}>()

defineEmits<{ remove: []; edit: [] }>()

const store = useDashboardStore()

const isCustom = computed(() => props.widgetId.startsWith('custom-'))

const meta = computed(() => resolveWidgetMeta(props.widgetId, store.customWidgets))

const asyncComponent = computed(() => {
  const m = meta.value
  if (!m) return null
  return defineAsyncComponent(m.component)
})
</script>

<style scoped lang="scss">
.widget-wrapper {
  height: 100%; border: 1px solid $border-light; box-shadow: $box-shadow-sm;
  transition: box-shadow $transition-base, transform $transition-base, border-color $transition-base;
  &:hover { box-shadow: $box-shadow-base; border-color: $border-color; }
  &.edit-mode { border: 2px dashed $primary-color; background: rgba(99, 102, 241, 0.02); cursor: grab; &:active { cursor: grabbing; } }
}
.widget-header { display: flex; align-items: center; gap: 8px; }
.widget-title { font-family: $font-family-heading; font-weight: 600; font-size: $font-size-base; color: $text-primary; }
.drag-handle { cursor: grab; font-size: 18px; color: $text-secondary; user-select: none; &:hover { color: $primary-color; } }
.header-actions { margin-left: auto; display: flex; gap: 4px; }
</style>
```

- [ ] **Step 3: 删除旧的 AddWidgetDialog.vue**

Run: `rm /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend/src/views/dashboard/components/AddWidgetDialog.vue`

- [ ] **Step 4: 验证构建**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -10`

Expected: `✓ built in`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: integrate widget marketplace drawer and update widget wrapper with edit support"
```

---

## Task 7: 最终验证

- [ ] **Step 1: 完整构建检查**

Run: `cd /Users/yili/Desktop/iflytek/test-demo/code/admin-frontend && npm run build 2>&1 | tail -10`

Expected: `✓ built in`

- [ ] **Step 2: 功能验证清单**

手动验证：
1. 编辑模式下，"+ 添加卡片"替换为"卡片市场"按钮
2. 点击打开 Drawer，显示预置卡片 Tab
3. 搜索和分类筛选正常工作
4. 点击"+ 添加"将预置卡片添加到仪表盘
5. 切换到"已添加" Tab，列出所有已添加卡片
6. 点击"自定义创建卡片"打开向导弹窗
7. 3 步向导正常流转：选类型 → 配置 → 预览 → 创建
8. 自定义卡片出现在仪表盘上并正确渲染
9. 在"已添加" Tab 点击"编辑"可修改自定义卡片
10. 点击"移除"可删除卡片
11. 刷新页面后自定义卡片和布局保持不变
