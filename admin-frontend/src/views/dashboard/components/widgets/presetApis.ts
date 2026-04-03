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
