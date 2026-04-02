import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useIdempotent } from '../useIdempotent'

vi.mock('@/api/modules/idempotent', () => ({
  getIdempotentToken: vi.fn().mockResolvedValue({ data: 'mock-token-uuid' })
}))

describe('useIdempotent', () => {
  beforeEach(() => {
    const { clearToken } = useIdempotent()
    clearToken()
  })

  it('fetchToken 后 getToken 返回 token', async () => {
    const { fetchToken, getToken } = useIdempotent()
    await fetchToken()
    expect(getToken()).toBe('mock-token-uuid')
  })

  it('clearToken 清除 token', async () => {
    const { fetchToken, getToken, clearToken } = useIdempotent()
    await fetchToken()
    clearToken()
    expect(getToken()).toBeNull()
  })
})
