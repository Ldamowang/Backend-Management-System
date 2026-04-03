import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { widgetRegistry, DEFAULT_LAYOUT, getWidgetMeta, resolveWidgetMeta, type WidgetMeta } from '@/views/dashboard/components/widgets/registry'
import type { CustomWidget } from '@/types/widget'

const STORAGE_KEY = 'dashboard-layout'
const CUSTOM_WIDGETS_KEY = 'custom-widgets'

export const useDashboardStore = defineStore('dashboard', () => {
  // ===== Layout =====
  function loadLayout(): string[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed
      }
    } catch { /* ignore */ }
    return [...DEFAULT_LAYOUT]
  }

  function saveLayout() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(activeWidgets.value))
  }

  const activeWidgets = ref<string[]>(loadLayout())
  const editMode = ref(false)

  // ===== Custom Widgets =====
  function loadCustomWidgets(): CustomWidget[] {
    try {
      const raw = localStorage.getItem(CUSTOM_WIDGETS_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed)) return parsed
      }
    } catch { /* ignore */ }
    return []
  }

  function saveCustomWidgets() {
    localStorage.setItem(CUSTOM_WIDGETS_KEY, JSON.stringify(customWidgets.value))
  }

  const customWidgets = ref<CustomWidget[]>(loadCustomWidgets())

  function addCustomWidget(widget: CustomWidget) {
    customWidgets.value = [...customWidgets.value, widget]
    saveCustomWidgets()
    addWidget(widget.id)
  }

  function updateCustomWidget(widgetId: string, updates: Partial<Omit<CustomWidget, 'id'>>) {
    customWidgets.value = customWidgets.value.map(w =>
      w.id === widgetId ? { ...w, ...updates, updatedAt: Date.now() } : w
    )
    saveCustomWidgets()
  }

  function removeCustomWidget(widgetId: string) {
    customWidgets.value = customWidgets.value.filter(w => w.id !== widgetId)
    saveCustomWidgets()
    removeWidget(widgetId)
  }

  function getCustomWidget(widgetId: string): CustomWidget | undefined {
    return customWidgets.value.find(w => w.id === widgetId)
  }

  // ===== Preset Widgets =====
  const availableWidgets = computed<WidgetMeta[]>(() =>
    widgetRegistry.filter(w => !activeWidgets.value.includes(w.id))
  )

  const activeWidgetMetas = computed<WidgetMeta[]>(() =>
    activeWidgets.value
      .map(id => resolveWidgetMeta(id, customWidgets.value))
      .filter((m): m is WidgetMeta => m !== undefined)
  )

  function addWidget(widgetId: string) {
    if (!activeWidgets.value.includes(widgetId)) {
      activeWidgets.value = [...activeWidgets.value, widgetId]
      saveLayout()
    }
  }

  function removeWidget(widgetId: string) {
    activeWidgets.value = activeWidgets.value.filter(id => id !== widgetId)
    saveLayout()
    // 如果是自定义卡片，也从 customWidgets 中删除
    if (widgetId.startsWith('custom-')) {
      customWidgets.value = customWidgets.value.filter(w => w.id !== widgetId)
      saveCustomWidgets()
    }
  }

  function reorderWidgets(newOrder: string[]) {
    activeWidgets.value = [...newOrder]
    saveLayout()
  }

  function resetLayout() {
    activeWidgets.value = [...DEFAULT_LAYOUT]
    localStorage.removeItem(STORAGE_KEY)
  }

  function toggleEditMode() {
    editMode.value = !editMode.value
  }

  return {
    activeWidgets,
    editMode,
    customWidgets,
    availableWidgets,
    activeWidgetMetas,
    addWidget,
    removeWidget,
    reorderWidgets,
    resetLayout,
    toggleEditMode,
    addCustomWidget,
    updateCustomWidget,
    removeCustomWidget,
    getCustomWidget
  }
})
