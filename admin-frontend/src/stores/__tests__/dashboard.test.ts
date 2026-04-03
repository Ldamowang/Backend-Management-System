import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '../modules/dashboard'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: vi.fn((key: string) => mockStorage[key] || null),
  setItem: vi.fn((key: string, value: string) => { mockStorage[key] = value }),
  removeItem: vi.fn((key: string) => { delete mockStorage[key] })
})

describe('useDashboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.keys(mockStorage).forEach(k => delete mockStorage[k])
  })

  it('初始化加载默认布局', () => {
    const store = useDashboardStore()
    expect(store.activeWidgets.length).toBeGreaterThan(0)
    expect(store.editMode).toBe(false)
  })

  it('addWidget 添加卡片到末尾', () => {
    const store = useDashboardStore()
    store.removeWidget('table-recent-login')
    store.addWidget('table-recent-login')
    expect(store.activeWidgets).toContain('table-recent-login')
  })

  it('removeWidget 移除卡片', () => {
    const store = useDashboardStore()
    store.removeWidget('stat-users')
    expect(store.activeWidgets).not.toContain('stat-users')
  })

  it('reorderWidgets 更新顺序', () => {
    const store = useDashboardStore()
    const reversed = [...store.activeWidgets].reverse()
    store.reorderWidgets(reversed)
    expect(store.activeWidgets).toEqual(reversed)
  })

  it('resetLayout 恢复默认', () => {
    const store = useDashboardStore()
    store.removeWidget('stat-users')
    store.resetLayout()
    expect(store.activeWidgets).toContain('stat-users')
  })

  it('toggleEditMode 切换编辑模式', () => {
    const store = useDashboardStore()
    store.toggleEditMode()
    expect(store.editMode).toBe(true)
    store.toggleEditMode()
    expect(store.editMode).toBe(false)
  })

  it('availableWidgets 返回未添加的 widget', () => {
    const store = useDashboardStore()
    store.removeWidget('shortcut-links')
    expect(store.availableWidgets.some(w => w.id === 'shortcut-links')).toBe(true)
  })
})
