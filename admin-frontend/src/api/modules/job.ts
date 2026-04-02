import request from '@/api/request'
import type { ApiResponse, PageResponse } from '@/types/api'

export interface JobInfo {
  id: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  description: string
  createdTime: string
}

export interface JobForm {
  id?: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  description: string
}

export interface JobLog {
  id: number
  jobId: number
  jobName: string
  invokeTarget: string
  status: number
  message: string
  errorMsg: string
  duration: number
  createdTime: string
}

export function getJobList(page: number, size: number): Promise<PageResponse<JobInfo>> {
  return request.get('/jobs', { params: { page, size } })
}

export function getJobById(id: number): Promise<ApiResponse<JobInfo>> {
  return request.get(`/jobs/${id}`)
}

export function createJob(data: JobForm): Promise<ApiResponse<null>> {
  return request.post('/jobs', data)
}

export function updateJob(id: number, data: JobForm): Promise<ApiResponse<null>> {
  return request.put(`/jobs/${id}`, data)
}

export function deleteJob(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/jobs/${id}`)
}

export function changeJobStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.patch(`/jobs/${id}/status`, { status })
}

export function runJob(id: number): Promise<ApiResponse<null>> {
  return request.post(`/jobs/${id}/run`)
}

export function getJobLogs(jobId: number, page: number, size: number): Promise<PageResponse<JobLog>> {
  return request.get(`/jobs/${jobId}/logs`, { params: { page, size } })
}
