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
