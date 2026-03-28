import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  configName: string
  configType: number
  description: string
}

export function getConfigs(): Promise<ApiResponse<SysConfig[]>> {
  return request.get('/configs')
}

export function updateConfigs(data: SysConfig[]): Promise<ApiResponse<null>> {
  return request.put('/configs', data)
}
