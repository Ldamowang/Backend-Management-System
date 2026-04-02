import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Tab {
  path: string
  title: string
  icon?: string
  name?: string   // 路由 name，用于 keep-alive
  pinned: boolean
  closable: boolean
}

const TABS_KEY = 'app-tabs'

function loadTabs(): Tab[] {
  try {
    const raw = localStorage.getItem(TABS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

const DEFAULT_TAB: Tab = {
  path: '/dashboard',
  title: '仪表盘',
  icon: 'Monitor',
  name: 'Dashboard',
  pinned: true,
  closable: false
}

export const useTabStore = defineStore('tab', () => {
  const saved = loadTabs()
  const tabs = ref<Tab[]>(saved.length > 0 ? saved : [{ ...DEFAULT_TAB }])
  const activeTab = ref(tabs.value[tabs.value.length - 1]?.path || '/dashboard')

  // 确保仪表盘始终存在
  if (!tabs.value.find(t => t.path === '/dashboard')) {
    tabs.value.unshift({ ...DEFAULT_TAB })
  }

  const cachedViews = computed(() =>
    tabs.value.filter(t => t.name).map(t => t.name!)
  )

  function persistTabs() {
    localStorage.setItem(TABS_KEY, JSON.stringify(tabs.value))
  }

  function addTab(tab: { path: string; title: string; icon?: string; name?: string }) {
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push({
        path: tab.path,
        title: tab.title,
        icon: tab.icon,
        name: tab.name,
        pinned: false,
        closable: true
      })
    }
    activeTab.value = tab.path
    persistTabs()
  }

  function setActiveTab(path: string) {
    activeTab.value = path
  }

  function closeTab(path: string): string {
    const tab = tabs.value.find(t => t.path === path)
    if (!tab || tab.pinned) return activeTab.value

    const index = tabs.value.indexOf(tab)
    tabs.value.splice(index, 1)

    // 如果关闭的是当前标签，激活相邻标签
    if (activeTab.value === path) {
      const next = tabs.value[index] || tabs.value[index - 1]
      activeTab.value = next?.path || '/dashboard'
    }

    persistTabs()
    return activeTab.value
  }

  function closeOtherTabs(path: string) {
    tabs.value = tabs.value.filter(t => t.pinned || t.path === path)
    activeTab.value = path
    persistTabs()
  }

  function closeLeftTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index <= 0) return
    tabs.value = tabs.value.filter((t, i) => t.pinned || i >= index)
    activeTab.value = path
    persistTabs()
  }

  function closeRightTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index < 0) return
    tabs.value = tabs.value.filter((t, i) => t.pinned || i <= index)
    activeTab.value = path
    persistTabs()
  }

  function closeAllTabs() {
    tabs.value = tabs.value.filter(t => t.pinned)
    activeTab.value = tabs.value[0]?.path || '/dashboard'
    persistTabs()
  }

  function pinTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab) {
      tab.pinned = true
      tab.closable = false
      persistTabs()
    }
  }

  function unpinTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab && tab.path !== '/dashboard') {
      tab.pinned = false
      tab.closable = true
      persistTabs()
    }
  }

  function reorderTabs(fromIndex: number, toIndex: number) {
    const [moved] = tabs.value.splice(fromIndex, 1)
    tabs.value.splice(toIndex, 0, moved)
    persistTabs()
  }

  function refreshTab(path: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab?.name) {
      // 暂时移出缓存列表，触发组件重新挂载
      const name = tab.name
      tab.name = undefined
      setTimeout(() => {
        tab.name = name
      }, 100)
    }
  }

  function resetTabs() {
    tabs.value = [{ ...DEFAULT_TAB }]
    activeTab.value = '/dashboard'
    persistTabs()
  }

  return {
    tabs,
    activeTab,
    cachedViews,
    addTab,
    setActiveTab,
    closeTab,
    closeOtherTabs,
    closeLeftTabs,
    closeRightTabs,
    closeAllTabs,
    pinTab,
    unpinTab,
    reorderTabs,
    refreshTab,
    resetTabs
  }
})
