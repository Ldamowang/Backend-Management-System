import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface JvmInfo {
  javaVersion: string
  javaVendor: string
  jvmName: string
  heapUsed: number
  heapMax: number
  heapCommitted: number
  nonHeapUsed: number
  startTime: string
  uptime: number
}

export interface OsInfo {
  name: string
  arch: string
  version: string
  availableProcessors: number
  systemLoadAverage: number
}

export interface DiskInfo {
  total: number
  free: number
  usable: number
  used: number
}

export interface RedisInfo {
  version: string
  usedMemory: string
  connectedClients: string
  uptimeInDays: string
  dbSize: number
  error?: string
}

export interface ServerInfo {
  jvm: JvmInfo
  os: OsInfo
  disk: DiskInfo
  redis: RedisInfo
}

export function getServerInfo(): Promise<ApiResponse<ServerInfo>> {
  return request.get('/server/info')
}
