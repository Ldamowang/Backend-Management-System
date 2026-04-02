import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'auto'
export type LayoutMode = 'sidebar' | 'top'
export type ThemeColor = string

const SETTINGS_KEY = 'app-settings'

function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

/** 将 HEX 色值与白色混合，生成 Element Plus light-N 衍生色 */
function mixWhite(hex: string, percentage: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const mix = (c: number) => Math.round(c + (255 - c) * (percentage / 10))
  return `rgb(${mix(r)}, ${mix(g)}, ${mix(b)})`
}

/** 将 HEX 色值与黑色混合，生成 Element Plus dark-N 衍生色 */
function mixBlack(hex: string, percentage: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const mix = (c: number) => Math.round(c * (1 - percentage / 10))
  return `rgb(${mix(r)}, ${mix(g)}, ${mix(b)})`
}

/** 设置主色及所有衍生色到 CSS 变量 */
function applyPrimaryColor(color: string) {
  const el = document.documentElement
  el.style.setProperty('--el-color-primary', color)
  for (const level of [3, 5, 7, 9]) {
    el.style.setProperty(`--el-color-primary-light-${level}`, mixWhite(color, level))
  }
  el.style.setProperty('--el-color-primary-dark-2', mixBlack(color, 2))
}

/** 判断系统是否偏好暗色模式 */
function prefersDark(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

/** 根据模式应用 dark class */
function applyDarkClass(mode: ThemeMode) {
  const isDark = mode === 'dark' || (mode === 'auto' && prefersDark())
  document.documentElement.classList.toggle('dark', isDark)
}

export const useAppStore = defineStore('app', () => {
  const saved = loadSettings()

  const sidebarCollapsed = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')
  const themeMode = ref<ThemeMode>(saved.themeMode || 'light')
  const themeColor = ref<ThemeColor>(saved.themeColor || '#409eff')
  const layoutMode = ref<LayoutMode>(saved.layoutMode || 'sidebar')
  const showTagsView = ref<boolean>(saved.showTagsView !== false)
  const fixedHeader = ref<boolean>(saved.fixedHeader !== false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setDevice(val: 'desktop' | 'mobile') {
    device.value = val
  }

  function setThemeMode(mode: ThemeMode) {
    themeMode.value = mode
    applyDarkClass(mode)
  }

  function setThemeColor(color: ThemeColor) {
    themeColor.value = color
    applyPrimaryColor(color)
  }

  function setLayoutMode(mode: LayoutMode) {
    layoutMode.value = mode
  }

  function persistSettings() {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify({
      themeMode: themeMode.value,
      themeColor: themeColor.value,
      layoutMode: layoutMode.value,
      showTagsView: showTagsView.value,
      fixedHeader: fixedHeader.value
    }))
  }

  // 自动持久化
  watch([themeMode, themeColor, layoutMode, showTagsView, fixedHeader], persistSettings)

  // 初始化主题
  applyDarkClass(themeMode.value)
  if (themeColor.value !== '#409eff') {
    applyPrimaryColor(themeColor.value)
  }

  // auto 模式：监听系统主题变化
  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (themeMode.value === 'auto') {
        applyDarkClass('auto')
      }
    })
  }

  return {
    sidebarCollapsed, device, themeMode, themeColor, layoutMode, showTagsView, fixedHeader,
    toggleSidebar, setDevice, setThemeMode, setThemeColor, setLayoutMode
  }
})
