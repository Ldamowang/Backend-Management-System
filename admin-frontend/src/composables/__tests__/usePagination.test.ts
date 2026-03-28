import { describe, it, expect } from 'vitest'
import { usePagination } from '../usePagination'

describe('usePagination', () => {
  describe('初始状态', () => {
    it('默认 page=1, size=10, total=0', () => {
      const { pagination } = usePagination()
      expect(pagination.page).toBe(1)
      expect(pagination.size).toBe(10)
      expect(pagination.total).toBe(0)
    })

    it('支持自定义 defaultSize', () => {
      const { pagination } = usePagination(20)
      expect(pagination.size).toBe(20)
    })
  })

  describe('handleSizeChange', () => {
    it('更新 size 并重置 page 为 1', () => {
      const { pagination, handleSizeChange } = usePagination()
      pagination.page = 3

      handleSizeChange(50)

      expect(pagination.size).toBe(50)
      expect(pagination.page).toBe(1) // page 被重置
    })
  })

  describe('handleCurrentChange', () => {
    it('更新 page', () => {
      const { pagination, handleCurrentChange } = usePagination()

      handleCurrentChange(5)

      expect(pagination.page).toBe(5)
    })

    it('不影响 size', () => {
      const { pagination, handleCurrentChange } = usePagination()

      handleCurrentChange(3)

      expect(pagination.size).toBe(10) // 保持默认值
    })
  })

  describe('reset', () => {
    it('重置 page 和 total', () => {
      const { pagination, reset } = usePagination()
      pagination.page = 5
      pagination.total = 100

      reset()

      expect(pagination.page).toBe(1)
      expect(pagination.total).toBe(0)
    })

    it('不影响 size', () => {
      const { pagination, reset } = usePagination(20)
      pagination.page = 5

      reset()

      expect(pagination.size).toBe(20) // size 保持不变
    })
  })
})
