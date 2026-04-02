<template>
  <div>
    <el-card>
      <template #header><span>{{ $t('system.log.operation') }}</span></template>
      <el-table :data="tableData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="$t('system.log.operationUser')" width="120" />
        <el-table-column prop="module" :label="$t('system.log.operationModule')" width="120" />
        <el-table-column prop="operation" :label="$t('system.log.operationType')" width="100" />
        <el-table-column prop="requestMethod" :label="$t('system.log.requestMethod')" width="100" />
        <el-table-column prop="requestUrl" :label="$t('system.log.requestUrl')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" :label="$t('system.log.ip')" width="140" />
        <el-table-column prop="duration" :label="$t('system.log.duration')" width="100" />
        <el-table-column :label="$t('common.label.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.success') : $t('common.label.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('system.log.operationTime')" min-width="170" />
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
import { useI18n } from 'vue-i18n'
import { getOperationLogs, type OperationLog } from '@/api/modules/log'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()
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
