import { ref } from 'vue'
import { getIdempotentToken } from '@/api/modules/idempotent'

const currentToken = ref<string | null>(null)

export function useIdempotent() {
  async function fetchToken() {
    try {
      const { data } = await getIdempotentToken()
      currentToken.value = data
    } catch {
      currentToken.value = null
    }
  }

  function getToken(): string | null {
    return currentToken.value
  }

  function clearToken() {
    currentToken.value = null
  }

  return { fetchToken, getToken, clearToken, currentToken }
}
