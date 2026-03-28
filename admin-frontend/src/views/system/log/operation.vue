<template>
  <div>
    <el-card>
      <template #header><span>操作日志</span></template>
      <el-table :data="tableData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作用户" width="120" />
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="operation" label="操作类型" width="100" />
        <el-table-column prop="requestMethod" label="请求方法" width="100" />
        <el-table-column prop="requestUrl" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="操作时间" min-width="170" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="(s: number) => { pagination.size = s; pagination.page = 1; fetchData() }"
        @current-change="(p: number) => { pagination.page = p; fetchData() }"
        class="pagination-right"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOperationLogs, type OperationLog } from '@/api/modules/log'
import { usePagination } from '@/composables/usePagination'

const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const { pagination } = usePagination()

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getOperationLogs({ page: pagination.page, size: pagination.size })
    tableData.value = data.list
    pagination.total = data.total
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>
