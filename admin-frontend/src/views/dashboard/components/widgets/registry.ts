import type { CustomWidget } from '@/types/widget'

export interface WidgetMeta {
  id: string
  name: string
  type: 'stat' | 'chart' | 'table' | 'shortcut'
  component: () => Promise<any>
  defaultSpan: number
}

export const widgetRegistry: WidgetMeta[] = [
  {
    id: 'stat-users',
    name: '用户统计',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-roles',
    name: '角色统计',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-today-login',
    name: '今日登录',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'stat-system',
    name: '系统状态',
    type: 'stat',
    component: () => import('./StatCard.vue'),
    defaultSpan: 6
  },
  {
    id: 'chart-login-trend',
    name: '登录趋势（7天）',
    type: 'chart',
    component: () => import('./ChartLoginTrend.vue'),
    defaultSpan: 12
  },
  {
    id: 'chart-overview',
    name: '系统概览',
    type: 'chart',
    component: () => import('./ChartOverview.vue'),
    defaultSpan: 12
  },
  {
    id: 'table-recent-login',
    name: '最近登录记录',
    type: 'table',
    component: () => import('./TableRecentLogin.vue'),
    defaultSpan: 24
  },
  {
    id: 'shortcut-links',
    name: '快捷入口',
    type: 'shortcut',
    component: () => import('./ShortcutLinks.vue'),
    defaultSpan: 24
  }
]

export const DEFAULT_LAYOUT = [
  'stat-users', 'stat-roles', 'stat-today-login', 'stat-system',
  'chart-login-trend', 'chart-overview',
  'table-recent-login'
]

export function getWidgetMeta(id: string): WidgetMeta | undefined {
  return widgetRegistry.find(w => w.id === id)
}

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
