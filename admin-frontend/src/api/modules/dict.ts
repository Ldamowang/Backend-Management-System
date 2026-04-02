import request from '@/api/request'
import type { ApiResponse } from '@/types/api'
import type { DictType, DictTypeForm, DictData, DictDataForm } from '@/types/dict'

// 字典类型
export function getDictTypes(): Promise<ApiResponse<DictType[]>> {
  return request.get('/dicts/types')
}

export function getDictTypeById(id: number): Promise<ApiResponse<DictType>> {
  return request.get(`/dicts/types/${id}`)
}

export function createDictType(data: DictTypeForm): Promise<ApiResponse<null>> {
  return request.post('/dicts/types', data)
}

export function updateDictType(id: number, data: DictTypeForm): Promise<ApiResponse<null>> {
  return request.put(`/dicts/types/${id}`, data)
}

export function deleteDictType(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/dicts/types/${id}`)
}

// 字典数据
export function getDictDataByType(dictType: string): Promise<ApiResponse<DictData[]>> {
  return request.get(`/dicts/data/${dictType}`)
}

export function createDictData(data: DictDataForm): Promise<ApiResponse<null>> {
  return request.post('/dicts/data', data)
}

export function updateDictData(id: number, data: DictDataForm): Promise<ApiResponse<null>> {
  return request.put(`/dicts/data/${id}`, data)
}

export function deleteDictData(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/dicts/data/${id}`)
}
