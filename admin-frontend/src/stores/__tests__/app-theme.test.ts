import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '../modules/app'

// jsdom 不支持 matchMedia，需要全局 mock
const mockMatchMedia = vi.fn().mockReturnValue({
  matches: false,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn()
})
vi.stubGlobal('matchMedia', mockMatchMedia)

describe('app store - theme enhancement', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    document.documentElement.classList.remove('dark')
    document.documentElement.style.removeProperty('--el-color-primary')
    mockMatchMedia.mockReturnValue({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })
  })

  it('setThemeMode("auto") 应根据系统偏好设置暗黑模式', () => {
    const store = useAppStore()
    // 模拟系统偏好为暗色
    mockMatchMedia.mockReturnValue({
      matches: true,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    })

    store.setThemeMode('auto')
    expect(store.themeMode).toBe('auto')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('setThemeColor 应设置主色和衍生色', () => {
    const store = useAppStore()
    store.setThemeColor('#722ED1')
    expect(store.themeColor).toBe('#722ED1')
    expect(document.documentElement.style.getPropertyValue('--el-color-primary')).toBe('#722ED1')
    // 验证衍生色
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-3')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-5')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-7')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-light-9')).toBeTruthy()
    expect(document.documentElement.style.getPropertyValue('--el-color-primary-dark-2')).toBeTruthy()
  })

  it('themeMode 应持久化到 localStorage', async () => {
    const store = useAppStore()
    store.setThemeMode('dark')
    await nextTick()
    const saved = JSON.parse(localStorage.getItem('app-settings') || '{}')
    expect(saved.themeMode).toBe('dark')
  })
})
