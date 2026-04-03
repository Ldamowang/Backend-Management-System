<template>
  <div>
    <el-card>
      <template #header><span>{{ $t('system.log.operation') }}</span></template>

      <!-- Search Form -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="$t('system.log.operationModule')">
          <el-select
            v-model="searchForm.module"
            :placeholder="$t('system.log.modulePlaceholder')"
            clearable
            style="width: 160px"
          >
            <el-option label="system" value="system" />
            <el-option label="auth" value="auth" />
            <el-option label="profile" value="profile" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('common.label.status')">
          <el-select
            v-model="searchForm.status"
            :placeholder="$t('system.log.statusPlaceholder')"
            clearable
            style="width: 140px"
          >
            <el-option :label="$t('common.label.success')" :value="1" />
            <el-option :label="$t('common.label.failed')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('system.log.dateRange')">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            :start-placeholder="$t('system.log.dateRangePlaceholder')"
            :end-placeholder="$t('system.log.dateRangePlaceholder')"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            {{ $t('common.action.search') }}
          </el-button>
          <el-button @click="handleReset">
            {{ $t('common.action.reset') }}
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="$t('system.log.operationUser')" width="120" />
        <el-table-column prop="module" :label="$t('system.log.operationModule')" width="120" />
        <el-table-column prop="operation" :label="$t('system.log.operationType')" width="100" />
        <el-table-column prop="requestMethod" :label="$t('system.log.requestMethod')" width="100" />
        <el-table-column prop="requestUrl" :label="$t('system.log.requestUrl')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targetType" :label="$t('system.log.targetType')" width="110" />
        <el-table-column prop="targetId" :label="$t('system.log.targetId')" width="100" />
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
        <el-table-column :label="$t('common.label.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.beforeData || row.afterData"
              type="primary"
              link
              size="small"
              @click="showDiffDialog(row)"
            >
              {{ $t('system.log.viewChanges') }}
            </el-button>
          </template>
        </el-table-column>
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

    <!-- Change Detail Dialog -->
    <el-dialog
      v-model="diffDialogVisible"
      :title="$t('system.log.changeDetail')"
      width="900px"
      destroy-on-close
    >
      <div v-if="currentRow" class="diff-container">
        <div class="diff-panel">
          <div class="diff-header">{{ $t('system.log.beforeData') }}</div>
          <el-scrollbar height="400px">
            <pre class="diff-content">{{ formatJson(currentRow.beforeData) }}</pre>
          </el-scrollbar>
        </div>
        <div class="diff-panel">
          <div class="diff-header">{{ $t('system.log.afterData') }}</div>
          <el-scrollbar height="400px">
            <pre class="diff-content">{{ formatJson(currentRow.afterData) }}</pre>
          </el-scrollbar>
        </div>
      </div>
      <div v-else class="no-data-tip">{{ $t('system.log.noChangeData') }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getOperationLogs, type OperationLog, type OperationLogQuery } from '@/api/modules/log'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const { pagination } = usePagination()

const searchForm = reactive({
  module: '',
  status: undefined as number | undefined,
  dateRange: null as [string, string] | null
})

const diffDialogVisible = ref(false)
const currentRow = ref<OperationLog | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const params: OperationLogQuery = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchForm.module) {
      params.module = searchForm.module
    }
    if (searchForm.status !== undefined && searchForm.status !== null) {
      params.status = searchForm.status
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const { data } = await getOperationLogs(params)
    tableData.value = data.list
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  searchForm.module = ''
  searchForm.status = undefined
  searchForm.dateRange = null
  pagination.page = 1
  fetchData()
}

function showDiffDialog(row: OperationLog) {
  currentRow.value = row
  diffDialogVisible.value = true
}

function formatJson(jsonStr?: string): string {
  if (!jsonStr) return t('system.log.noChangeData')
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(fetchData)
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.diff-container {
  display: flex;
  gap: 16px;
}

.diff-panel {
  flex: 1;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
}

.diff-header {
  padding: 8px 16px;
  font-weight: 600;
  background-color: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.diff-content {
  padding: 12px 16px;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.no-data-tip {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
}
</style>
