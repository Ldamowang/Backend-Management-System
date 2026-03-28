import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { RoleInfo, RoleForm } from '@/types/role'

export function getRoleList(): Promise<ApiResponse<RoleInfo[]>> {
  return request.get('/roles')
}

export function getRoleById(id: number): Promise<ApiResponse<RoleInfo>> {
  return request.get(`/roles/${id}`)
}

export function createRole(data: RoleForm): Promise<ApiResponse<null>> {
  return request.post('/roles', data)
}

export function updateRole(id: number, data: RoleForm): Promise<ApiResponse<null>> {
  return request.put(`/roles/${id}`, data)
}

export function deleteRole(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/roles/${id}`)
}

export function assignMenus(roleId: number, menuIds: number[]): Promise<ApiResponse<null>> {
  return request.post(`/roles/${roleId}/menus`, { menuIds })
}
