import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface ProfileInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  gender: number
}

export interface UpdateProfileForm {
  nickname: string
  email: string
  phone: string
  gender: number
}

export interface UpdatePasswordForm {
  oldPassword: string
  newPassword: string
}

export function getProfile(): Promise<ApiResponse<ProfileInfo>> {
  return request.get('/profile')
}

export function updateProfile(data: UpdateProfileForm): Promise<ApiResponse<null>> {
  return request.put('/profile', data)
}

export function updatePassword(data: UpdatePasswordForm): Promise<ApiResponse<null>> {
  return request.put('/profile/password', data)
}
