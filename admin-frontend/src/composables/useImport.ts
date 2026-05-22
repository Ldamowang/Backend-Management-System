import { ref } from 'vue'
import request from '@/api/request'
import type { ApiResponse } from '@/types/api'

export interface ImportResultData {
  totalCount: number
  successCount: number
  failCount: number
  errors: ImportErrorDetail[]
}

export interface ImportErrorDetail {
  row: number
  field: string
  message: string
}

export function useImport() {
  const importing = ref(false)
  const importResult = ref<ImportResultData | null>(null)

  const importData = async (url: string, file: File): Promise<ImportResultData> => {
    importing.value = true
    importResult.value = null
    try {
      const formData = new FormData()
      formData.append('file', file)
      const res: ApiResponse<ImportResultData> = await request.post(url, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      importResult.value = res.data
      return res.data
    } finally {
      importing.value = false
    }
  }

  return { importing, importResult, importData }
}
