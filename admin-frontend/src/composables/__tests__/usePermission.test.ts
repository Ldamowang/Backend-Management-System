import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermission } from '../usePermission'
import { useUserStore } from '@/stores/modules/user'

// mock API 和 auth 模块
vi.mock('@/api/modules/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn()
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  setRefreshToken: vi.fn(),
  clearAuth: vi.fn()
}))

describe('usePermission', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('hasPermission', () => {
    it('admin 角色拥有所有权限', () => {
      const userStore = useUserStore()
      userStore.roles = ['admin']

      const { hasPermission } = usePermission()
      expect(hasPermission('sys:user:add')).toBe(true)
      expect(hasPermission('any:permission')).toBe(true)
    })

    it('普通角色匹配具体权限 - 返回 true', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']
      userStore.permissions = ['sys:user:list', 'sys:user:edit']

      const { hasPermission } = usePermission()
      expect(hasPermission('sys:user:list')).toBe(true)
    })

    it('普通角色无匹配权限 - 返回 false', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']
      userStore.permissions = ['sys:user:list']

      const { hasPermission } = usePermission()
      expect(hasPermission('sys:user:delete')).toBe(false)
    })

    it('支持数组参数 - 任一匹配即返回 true', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']
      userStore.permissions = ['sys:user:edit']

      const { hasPermission } = usePermission()
      expect(hasPermission(['sys:user:delete', 'sys:user:edit'])).toBe(true)
    })

    it('数组参数全部不匹配 - 返回 false', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']
      userStore.permissions = ['sys:user:list']

      const { hasPermission } = usePermission()
      expect(hasPermission(['sys:user:delete', 'sys:user:add'])).toBe(false)
    })

    it('空字符串权限 - 返回 false', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']
      userStore.permissions = []

      const { hasPermission } = usePermission()
      expect(hasPermission('')).toBe(false)
    })
  })

  describe('hasRole', () => {
    it('匹配角色 - 返回 true', () => {
      const userStore = useUserStore()
      userStore.roles = ['admin', 'editor']

      const { hasRole } = usePermission()
      expect(hasRole('admin')).toBe(true)
      expect(hasRole('editor')).toBe(true)
    })

    it('不匹配角色 - 返回 false', () => {
      const userStore = useUserStore()
      userStore.roles = ['editor']

      const { hasRole } = usePermission()
      expect(hasRole('admin')).toBe(false)
    })

    it('无角色 - 返回 false', () => {
      const userStore = useUserStore()
      userStore.roles = []

      const { hasRole } = usePermission()
      expect(hasRole('admin')).toBe(false)
    })
  })
})
