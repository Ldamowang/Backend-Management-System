import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

export function useExport() {
  const exporting = ref(false)

  const exportData = async (url: string, params?: Record<string, unknown>, fileName?: string) => {
    exporting.value = true
    try {
      const response = await request.get(url, {
        params,
        responseType: 'blob'
      })

      const blob = new Blob([response as unknown as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = fileName || 'export.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)

      ElMessage.success('导出成功')
    } catch {
      ElMessage.error('导出失败')
    } finally {
      exporting.value = false
    }
  }

  const downloadTemplate = (url: string, fileName?: string) => {
    return exportData(url, undefined, fileName || '导入模板.xlsx')
  }

  return { exporting, exportData, downloadTemplate }
}
