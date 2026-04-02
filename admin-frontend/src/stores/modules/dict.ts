import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDictDataByType } from '@/api/modules/dict'
import type { DictData } from '@/types/dict'

export const useDictStore = defineStore('dict', () => {
  const dictMap = ref<Record<string, DictData[]>>({})
  const pendingRequests = new Map<string, Promise<DictData[]>>()

  async function getDictData(dictType: string): Promise<DictData[]> {
    if (dictMap.value[dictType]) {
      return dictMap.value[dictType]
    }

    if (pendingRequests.has(dictType)) {
      return pendingRequests.get(dictType)!
    }

    const promise = getDictDataByType(dictType).then(({ data }) => {
      dictMap.value = { ...dictMap.value, [dictType]: data }
      pendingRequests.delete(dictType)
      return data
    }).catch(() => {
      pendingRequests.delete(dictType)
      return [] as DictData[]
    })

    pendingRequests.set(dictType, promise)
    return promise
  }

  async function getDictLabel(dictType: string, value: string): Promise<string> {
    const data = await getDictData(dictType)
    const item = data.find(d => d.dictValue === value)
    return item ? item.dictLabel : value
  }

  async function refreshDict(dictType: string): Promise<DictData[]> {
    const { [dictType]: _, ...rest } = dictMap.value
    dictMap.value = rest
    return getDictData(dictType)
  }

  function clearAll() {
    dictMap.value = {}
    pendingRequests.clear()
  }

  return { dictMap, getDictData, getDictLabel, refreshDict, clearAll }
})
