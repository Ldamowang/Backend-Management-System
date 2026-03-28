import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '../modules/app'

describe('app store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始状态 - sidebar 展开，设备为 desktop', () => {
    const store = useAppStore()
    expect(store.sidebarCollapsed).toBe(false)
    expect(store.device).toBe('desktop')
  })

  it('toggleSidebar 切换侧边栏状态', () => {
    const store = useAppStore()

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('setDevice 设置设备类型', () => {
    const store = useAppStore()

    store.setDevice('mobile')
    expect(store.device).toBe('mobile')

    store.setDevice('desktop')
    expect(store.device).toBe('desktop')
  })
})
