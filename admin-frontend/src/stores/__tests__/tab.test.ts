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
