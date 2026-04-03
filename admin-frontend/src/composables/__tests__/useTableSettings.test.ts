import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTableSettings, type ColumnSetting } from '../useTableSettings'

const mockStorage: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: vi.fn((key: string) => mockStorage[key] || null),
  setItem: vi.fn((key: string, value: string) => { mockStorage[key] = value }),
  removeItem: vi.fn((key: string) => { delete mockStorage[key] })
})

describe('useTableSettings', () => {
  const defaultColumns: ColumnSetting[] = [
    { key: 'name', label: '姓名', visible: true },
    { key: 'email', label: '邮箱', visible: true },
    { key: 'phone', label: '电话', visible: false }
  ]

  beforeEach(() => {
    Object.keys(mockStorage).forEach(k => delete mockStorage[k])
  })

  it('初始加载默认列设置', () => {
    const { columns } = useTableSettings('test-table', defaultColumns)
    expect(columns.value).toHaveLength(3)
    expect(columns.value[0].key).toBe('name')
  })

  it('toggleColumn 切换列可见性', () => {
    const { columns, toggleColumn } = useTableSettings('test-table', defaultColumns)
    toggleColumn('phone')
    expect(columns.value.find(c => c.key === 'phone')?.visible).toBe(true)
  })

  it('resetColumns 恢复默认', () => {
    const { columns, toggleColumn, resetColumns } = useTableSettings('test-table', defaultColumns)
    toggleColumn('name')
    resetColumns()
    expect(columns.value[0].visible).toBe(true)
  })

  it('density 默认为 default', () => {
    const { density } = useTableSettings('test-table', defaultColumns)
    expect(density.value).toBe('default')
  })

  it('setDensity 设置密度', () => {
    const { density, setDensity } = useTableSettings('test-table', defaultColumns)
    setDensity('small')
    expect(density.value).toBe('small')
  })
})
