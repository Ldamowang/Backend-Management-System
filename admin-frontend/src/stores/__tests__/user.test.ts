import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../modules/user'

// mock API 模块
vi.mock('@/api/modules/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn()
}))

// mock auth utils
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  setRefreshToken: vi.fn(),
  clearAuth: vi.fn()
}))

describe('user store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('token 为空字符串', () => {
      const store = useUserStore()
      expect(store.token).toBe('')
    })

    it('userInfo 为 null', () => {
      const store = useUserStore()
      expect(store.userInfo).toBeNull()
    })

    it('roles 和 permissions 为空数组', () => {
      const store = useUserStore()
      expect(store.roles).toEqual([])
      expect(store.permissions).toEqual([])
    })
  })

  describe('hasPermission', () => {
    it('admin 角色拥有所有权限', () => {
      const store = useUserStore()
      store.roles = ['admin']

      expect(store.hasPermission('sys:user:add')).toBe(true)
      expect(store.hasPermission('any:perm')).toBe(true)
    })

    it('普通角色需要具体权限', () => {
      const store = useUserStore()
      store.roles = ['editor']
      store.permissions = ['sys:user:list', 'sys:user:edit']

      expect(store.hasPermission('sys:user:list')).toBe(true)
      expect(store.hasPermission('sys:user:delete')).toBe(false)
    })

    it('无角色无权限 - 返回 false', () => {
      const store = useUserStore()
      expect(store.hasPermission('sys:user:add')).toBe(false)
    })
  })

  describe('updateLocalProfile', () => {
    it('更新部分 userInfo 字段（不可变更新）', () => {
      const store = useUserStore()
      store.userInfo = {
        id: 1,
        username: 'test',
        nickname: '原昵称',
        deptId: 1,
        email: 'old@example.com',
        phone: '13800000000',
        avatar: '',
        gender: 1,
        status: 1,
        roles: [],
        permissions: [],
        createdTime: ''
      }

      const original = store.userInfo
      store.updateLocalProfile({ nickname: '新昵称', email: 'new@example.com' })

      // 引用不同（不可变更新）
      expect(store.userInfo).not.toBe(original)
      // 值正确
      expect(store.userInfo!.nickname).toBe('新昵称')
      expect(store.userInfo!.email).toBe('new@example.com')
      // 未修改字段保持不变
      expect(store.userInfo!.username).toBe('test')
    })

    it('userInfo 为 null 时不抛出异常', () => {
      const store = useUserStore()
      expect(() => store.updateLocalProfile({ nickname: 'x' })).not.toThrow()
      expect(store.userInfo).toBeNull()
    })
  })

  describe('resetState', () => {
    it('重置所有状态', () => {
      const store = useUserStore()
      store.token = 'some-token'
      store.userInfo = {
        id: 1,
        username: '',
        nickname: '',
        deptId: 0,
        email: '',
        phone: '',
        avatar: '',
        gender: 0,
        status: 1,
        roles: [],
        permissions: [],
        createdTime: ''
      }
      store.roles = ['admin']
      store.permissions = ['sys:user:add']

      store.resetState()

      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(store.roles).toEqual([])
      expect(store.permissions).toEqual([])
      expect(store.menus).toEqual([])
    })
  })
})
