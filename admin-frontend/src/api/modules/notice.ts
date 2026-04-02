import request from '@/api/request'
import type { ApiResponse, PageResponse } from '@/types/api'

export interface NoticeInfo {
  id: number
  title: string
  content: string
  noticeType: number
  status: number
  createdBy: string
  createdTime: string
}

export interface NoticeForm {
  id?: number
  title: string
  content: string
  noticeType: number
  status: number
}

export interface MyNotice {
  id: number
  noticeId: number
  title: string
  content: string
  noticeType: number
  isRead: number
  createdTime: string
}

// 管理接口
export function getNoticeList(page: number, size: number): Promise<PageResponse<NoticeInfo>> {
  return request.get('/notices', { params: { page, size } })
}

export function getNoticeById(id: number): Promise<ApiResponse<NoticeInfo>> {
  return request.get(`/notices/${id}`)
}

export function createNotice(data: NoticeForm): Promise<ApiResponse<null>> {
  return request.post('/notices', data)
}

export function updateNotice(id: number, data: NoticeForm): Promise<ApiResponse<null>> {
  return request.put(`/notices/${id}`, data)
}

export function deleteNotice(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/notices/${id}`)
}

export function publishNotice(id: number): Promise<ApiResponse<null>> {
  return request.patch(`/notices/${id}/publish`)
}

// 用户端接口
export function getMyNotices(): Promise<ApiResponse<MyNotice[]>> {
  return request.get('/notices/my')
}

export function getUnreadCount(): Promise<ApiResponse<number>> {
  return request.get('/notices/unread-count')
}

export function markNoticeRead(noticeId: number): Promise<ApiResponse<null>> {
  return request.patch(`/notices/${noticeId}/read`)
}

export function markAllNoticesRead(): Promise<ApiResponse<null>> {
  return request.patch('/notices/read-all')
}
