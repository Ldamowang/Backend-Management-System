import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export function getIdempotentToken(): Promise<ApiResponse<string>> {
  return request.get('/idempotent/token')
}
