import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useDictStore } from '../modules/dict'

vi.mock('@/api/modules/dict', () => ({
  getDictDataByType: vi.fn()
}))

import { getDictDataByType } from '@/api/modules/dict'
const mockedGetDictData = vi.mocked(getDictDataByType)

describe('useDictStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('getDictData', () => {
    it('首次调用请求 API 并缓存结果', async () => {
      const mockData = [
        { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' },
        { id: 2, dictType: 'sys_gender', dictLabel: '女', dictValue: '2', sortOrder: 2, status: 1, description: '', createdTime: '' }
      ]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      const result = await store.getDictData('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledWith('sys_gender')
      expect(result).toEqual(mockData)
    })

    it('第二次调用使用缓存不再请求 API', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      await store.getDictData('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledTimes(1)
    })
  })

  describe('getDictLabel', () => {
    it('根据 value 返回对应 label', async () => {
      const mockData = [
        { id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }
      ]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      const label = await store.getDictLabel('sys_gender', '1')
      expect(label).toBe('男')
    })

    it('未找到 value 返回原始值', async () => {
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: [] } as any)

      const store = useDictStore()
      const label = await store.getDictLabel('sys_gender', '99')
      expect(label).toBe('99')
    })
  })

  describe('refreshDict', () => {
    it('强制刷新清除缓存并重新请求', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      await store.refreshDict('sys_gender')

      expect(mockedGetDictData).toHaveBeenCalledTimes(2)
    })
  })

  describe('clearAll', () => {
    it('清空所有缓存', async () => {
      const mockData = [{ id: 1, dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sortOrder: 1, status: 1, description: '', createdTime: '' }]
      mockedGetDictData.mockResolvedValue({ code: 200, message: 'ok', data: mockData } as any)

      const store = useDictStore()
      await store.getDictData('sys_gender')
      store.clearAll()

      await store.getDictData('sys_gender')
      expect(mockedGetDictData).toHaveBeenCalledTimes(2)
    })
  })
})
