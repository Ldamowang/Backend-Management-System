import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { widgetRegistry, DEFAULT_LAYOUT, getWidgetMeta, type WidgetMeta } from '@/views/dashboard/components/widgets/registry'

const STORAGE_KEY = 'dashboard-layout'

export const useDashboardStore = defineStore('dashboard', () => {
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

  const availableWidgets = computed<WidgetMeta[]>(() =>
    widgetRegistry.filter(w => !activeWidgets.value.includes(w.id))
  )

  const activeWidgetMetas = computed<WidgetMeta[]>(() =>
    activeWidgets.value
      .map(id => getWidgetMeta(id))
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
    availableWidgets,
    activeWidgetMetas,
    addWidget,
    removeWidget,
    reorderWidgets,
    resetLayout,
    toggleEditMode
  }
})
