import request from '@/api/request'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { UserInfo, UserForm, UserQuery } from '@/types/user'

export function getUserList(params: UserQuery): Promise<PageResponse<UserInfo>> {
  return request.get('/users', { params })
}

export function getUserById(id: number): Promise<ApiResponse<UserInfo>> {
  return request.get(`/users/${id}`)
}

export function createUser(data: UserForm): Promise<ApiResponse<null>> {
  return request.post('/users', data)
}

export function updateUser(id: number, data: UserForm): Promise<ApiResponse<null>> {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/users/${id}`)
}

export function updateUserStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.patch(`/users/${id}/status`, { status })
}

export function exportUsers(params?: UserQuery): Promise<Blob> {
  return request.get('/users/export', {
    params,
    responseType: 'blob'
  })
}
