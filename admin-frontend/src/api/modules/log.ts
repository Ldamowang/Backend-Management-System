import request from '@/api/request'
import type { PageQuery, PageResponse } from '@/types/api'

export interface OperationLog {
  id: number
  userId: number
  username: string
  module: string
  operation: string
  method: string
  requestUrl: string
  requestMethod: string
  requestParams: string
  responseCode: number
  ip: string
  duration: number
  status: number
  errorMsg: string
  createdTime: string
}

export interface LoginLog {
  id: number
  username: string
  ip: string
  location: string
  browser: string
  os: string
  status: number
  message: string
  loginTime: string
}

export function getOperationLogs(params: PageQuery): Promise<PageResponse<OperationLog>> {
  return request.get('/logs/operation', { params })
}

export function getLoginLogs(params: PageQuery): Promise<PageResponse<LoginLog>> {
  return request.get('/logs/login', { params })
}
