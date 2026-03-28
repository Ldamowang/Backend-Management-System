import { describe, it, expect, beforeEach } from 'vitest'
import {
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
  removeRefreshToken,
  clearAuth
} from '../auth'

describe('auth utils', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('getToken / setToken / removeToken', () => {
    it('初始值为空字符串', () => {
      expect(getToken()).toBe('')
    })

    it('setToken 后可通过 getToken 获取', () => {
      setToken('abc123')
      expect(getToken()).toBe('abc123')
    })

    it('removeToken 后返回空字符串', () => {
      setToken('abc123')
      removeToken()
      expect(getToken()).toBe('')
    })
  })

  describe('getRefreshToken / setRefreshToken / removeRefreshToken', () => {
    it('初始值为空字符串', () => {
      expect(getRefreshToken()).toBe('')
    })

    it('setRefreshToken 后可通过 getRefreshToken 获取', () => {
      setRefreshToken('refresh_xyz')
      expect(getRefreshToken()).toBe('refresh_xyz')
    })

    it('removeRefreshToken 后返回空字符串', () => {
      setRefreshToken('refresh_xyz')
      removeRefreshToken()
      expect(getRefreshToken()).toBe('')
    })
  })

  describe('clearAuth', () => {
    it('同时清除 access token 和 refresh token', () => {
      setToken('token1')
      setRefreshToken('token2')

      clearAuth()

      expect(getToken()).toBe('')
      expect(getRefreshToken()).toBe('')
    })
  })
})
