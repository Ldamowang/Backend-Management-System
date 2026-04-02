import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { DeptItem, DeptSimpleItem, DeptForm } from '@/types/dept'

export function getDeptTree(): Promise<ApiResponse<DeptItem[]>> {
  return request.get('/depts/tree')
}

export function getDeptSimpleTree(): Promise<ApiResponse<DeptSimpleItem[]>> {
  return request.get('/depts/simple-tree')
}

export function getDeptById(id: number): Promise<ApiResponse<DeptItem>> {
  return request.get(`/depts/${id}`)
}

export function createDept(data: DeptForm): Promise<ApiResponse<null>> {
  return request.post('/depts', data)
}

export function updateDept(id: number, data: DeptForm): Promise<ApiResponse<null>> {
  return request.put(`/depts/${id}`, data)
}

export function deleteDept(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/depts/${id}`)
}
