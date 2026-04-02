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
  getConfigs: vi.fn().mockResolvedValue({ data: [] })
}))
vi.mock('@/api/modules/dict', () => ({
  getDictTypes: vi.fn().mockResolvedValue({ data: [] })
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
