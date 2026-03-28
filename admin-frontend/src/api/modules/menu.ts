import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { MenuItem, MenuForm } from '@/types/menu'

export function getMenuTree(): Promise<ApiResponse<MenuItem[]>> {
  return request.get('/menus/tree')
}

export function getMenuById(id: number): Promise<ApiResponse<MenuItem>> {
  return request.get(`/menus/${id}`)
}

export function createMenu(data: MenuForm): Promise<ApiResponse<null>> {
  return request.post('/menus', data)
}

export function updateMenu(id: number, data: MenuForm): Promise<ApiResponse<null>> {
  return request.put(`/menus/${id}`, data)
}

export function deleteMenu(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/menus/${id}`)
}
