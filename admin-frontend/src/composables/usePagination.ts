import { reactive } from 'vue'

export function usePagination(defaultSize = 10) {
  const pagination = reactive({
    page: 1,
    size: defaultSize,
    total: 0
  })

  function handleSizeChange(size: number) {
    pagination.size = size
    pagination.page = 1
  }

  function handleCurrentChange(page: number) {
    pagination.page = page
  }

  function reset() {
    pagination.page = 1
    pagination.total = 0
  }

  return { pagination, handleSizeChange, handleCurrentChange, reset }
}
