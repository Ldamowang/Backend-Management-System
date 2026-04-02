import { ref } from 'vue'
import { useDictStore } from '@/stores/modules/dict'
import type { DictData } from '@/types/dict'

export function useDict(dictType: string) {
  const data = ref<DictData[]>([])
  const loading = ref(true)

  const store = useDictStore()
  store.getDictData(dictType)
    .then(result => { data.value = result })
    .finally(() => { loading.value = false })

  return { data, loading }
}

export function useDictLabel(dictType: string, value: string) {
  const label = ref(value)

  const store = useDictStore()
  store.getDictLabel(dictType, value)
    .then(result => { label.value = result })

  return label
}
