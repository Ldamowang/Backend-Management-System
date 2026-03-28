import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { LoginForm, LoginResult, UserInfoResult } from '@/types/user'

export function login(data: LoginForm): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<ApiResponse<null>> {
  return request.post('/auth/logout')
}

export function getUserInfo(): Promise<ApiResponse<UserInfoResult>> {
  return request.get('/auth/userinfo')
}

export function refreshToken(refreshToken: string): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/refresh', { refreshToken })
}
