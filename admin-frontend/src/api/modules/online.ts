import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface OnlineUser {
  userId: number
  username: string
  nickname: string
  loginTime: string
  tokenId: string
  token: string
}

export function getOnlineUsers(): Promise<ApiResponse<OnlineUser[]>> {
  return request.get('/online-users')
}

export function kickOutUser(token: string): Promise<ApiResponse<null>> {
  return request.delete(`/online-users/${token}`)
}
