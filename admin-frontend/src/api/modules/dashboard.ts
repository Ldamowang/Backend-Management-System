import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface DashboardStats {
  userCount: number
  roleCount: number
  menuCount: number
  todayLoginCount: number
  totalLoginCount: number
}

export function getStats(): Promise<ApiResponse<DashboardStats>> {
  return request.get('/dashboard/stats')
}
