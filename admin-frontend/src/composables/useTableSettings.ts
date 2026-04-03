import { ref, watch } from 'vue'

export interface ColumnSetting {
  key: string
  label: string
  visible: boolean
}

type TableDensity = 'large' | 'default' | 'small'

const STORAGE_PREFIX = 'table-settings:'

export function useTableSettings(tableId: string, defaultColumns: ColumnSetting[]) {
  const storageKey = STORAGE_PREFIX + tableId

  function loadFromStorage(): { columns: ColumnSetting[]; density: TableDensity } | null {
    try {
      const raw = localStorage.getItem(storageKey)
      if (!raw) return null
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  function saveToStorage() {
    localStorage.setItem(storageKey, JSON.stringify({
      columns: columns.value,
      density: density.value
    }))
  }

  const saved = loadFromStorage()
  const columns = ref<ColumnSetting[]>(
    saved?.columns ?? defaultColumns.map(c => ({ ...c }))
  )
  const density = ref<TableDensity>(saved?.density ?? 'default')

  function toggleColumn(key: string) {
    columns.value = columns.value.map(c =>
      c.key === key ? { ...c, visible: !c.visible } : c
    )
  }

  function reorderColumns(newOrder: ColumnSetting[]) {
    columns.value = newOrder
  }

  function resetColumns() {
    columns.value = defaultColumns.map(c => ({ ...c }))
    density.value = 'default'
    localStorage.removeItem(storageKey)
  }

  function setDensity(d: TableDensity) {
    density.value = d
  }

  watch([columns, density], saveToStorage, { deep: true })

  return { columns, density, toggleColumn, reorderColumns, resetColumns, setDensity }
}
