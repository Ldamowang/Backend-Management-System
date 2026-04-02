import request from '@/api/request'
import type { ApiResponse, PageResponse } from '@/types/api'

export interface FileInfo {
  id: number
  fileName: string
  storedName: string
  filePath: string
  fileSize: number
  fileType: string
  fileExt: string
  uploader: string
  createdTime: string
}

export function uploadFile(file: File): Promise<ApiResponse<FileInfo>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getFileList(page: number, size: number): Promise<PageResponse<FileInfo>> {
  return request.get('/files', { params: { page, size } })
}

export function deleteFile(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/files/${id}`)
}

export function getDownloadUrl(id: number): string {
  return `/api/files/download/${id}`
}
