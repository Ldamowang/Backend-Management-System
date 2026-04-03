import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export function useSubmit() {
  const submitting = ref(false)
  let lastSubmitTime = 0
  const THROTTLE_MS = 300

  const submit = async <T>(
    apiFn: () => Promise<T>,
    options?: {
      successMessage?: string
      onSuccess?: (result: T) => void
      onError?: (error: Error) => void
      throttle?: number
    }
  ): Promise<T | undefined> => {
    const now = Date.now()
    const throttle = options?.throttle ?? THROTTLE_MS
    if (now - lastSubmitTime < throttle) return undefined
    lastSubmitTime = now

    if (submitting.value) return undefined

    submitting.value = true
    try {
      const result = await apiFn()
      if (options?.successMessage) {
        ElMessage.success(options.successMessage)
      }
      options?.onSuccess?.(result)
      return result
    } catch (error: any) {
      if (options?.onError) {
        options.onError(error)
      } else {
        ElMessage.error(error.message || '操作失败')
      }
      return undefined
    } finally {
      submitting.value = false
    }
  }

  return { submitting, submit }
}
