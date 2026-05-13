import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { TotpSetupResponse, TotpStatusResponse } from '@/types/twoFactor'

export function setup2FA(): Promise<ApiResponse<TotpSetupResponse>> {
  return request.post('/2fa/setup')
}

export function verify2FA(code: string): Promise<ApiResponse<void>> {
  return request.post('/2fa/verify', { code })
}

export function disable2FA(): Promise<ApiResponse<void>> {
  return request.delete('/2fa')
}

export function get2FAStatus(): Promise<ApiResponse<TotpStatusResponse>> {
  return request.get('/2fa/status')
}
