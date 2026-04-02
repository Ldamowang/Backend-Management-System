import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDict, useDictLabel } from '../useDict'

vi.mock('@/api/modules/dict', () => ({
  getDictDataByType: vi.fn()
}))

import { getDictDataByType } from '@/api/modules/dict'
const mockedGetDictData = vi.mocked(getDictDataByType)

describe('useDict', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('返回响应式的字典数据列表', async () => {
    const mockData = [
      { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
    ]
    mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

    const { data, loading } = useDict('sys_gender')

    expect(loading.value).toBe(true)
    await new Promise(resolve => setTimeout(resolve, 50))
    expect(data.value).toEqual(mockData)
    expect(loading.value).toBe(false)
  })
})

describe('useDictLabel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('返回响应式的标签文本', async () => {
    const mockData = [
      { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
    ]
    mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

    const label = useDictLabel('sys_gender', '1')
    await new Promise(resolve => setTimeout(resolve, 50))
    expect(label.value).toBe('男')
  })
})
