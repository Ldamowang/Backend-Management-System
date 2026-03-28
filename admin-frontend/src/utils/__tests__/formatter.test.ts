import { describe, it, expect } from 'vitest'
import { formatDate, formatFileSize } from '../formatter'

describe('formatter utils', () => {
  describe('formatDate', () => {
    it('格式化 Date 对象为默认格式', () => {
      const date = new Date(2026, 2, 17, 14, 30, 5) // 2026-03-17 14:30:05
      expect(formatDate(date)).toBe('2026-03-17 14:30:05')
    })

    it('格式化 ISO 字符串', () => {
      // 使用 UTC 避免时区问题
      const result = formatDate('2026-01-01T00:00:00.000Z')
      expect(result).toMatch(/2026-01-01/)
    })

    it('支持自定义格式', () => {
      const date = new Date(2026, 0, 5, 8, 3, 9)
      expect(formatDate(date, 'YYYY/MM/DD')).toBe('2026/01/05')
    })

    it('数字补零', () => {
      const date = new Date(2026, 0, 5, 3, 7, 9) // 月 1, 日 5, 时 3, 分 7, 秒 9
      expect(formatDate(date)).toBe('2026-01-05 03:07:09')
    })

    it('无效日期返回空字符串', () => {
      expect(formatDate('not-a-date')).toBe('')
    })

    it('接受 Date 和 string 类型', () => {
      const d = new Date(2026, 5, 15, 12, 0, 0)
      const str = d.toISOString()
      // 两者都能正常执行
      expect(formatDate(d)).toBeTruthy()
      expect(formatDate(str)).toBeTruthy()
    })
  })

  describe('formatFileSize', () => {
    it('0 字节', () => {
      expect(formatFileSize(0)).toBe('0 B')
    })

    it('字节级别', () => {
      expect(formatFileSize(500)).toBe('500 B')
    })

    it('KB 级别', () => {
      expect(formatFileSize(1024)).toBe('1 KB')
      expect(formatFileSize(1536)).toBe('1.5 KB')
    })

    it('MB 级别', () => {
      expect(formatFileSize(1048576)).toBe('1 MB')
      expect(formatFileSize(2621440)).toBe('2.5 MB')
    })

    it('GB 级别', () => {
      expect(formatFileSize(1073741824)).toBe('1 GB')
    })
  })
})
