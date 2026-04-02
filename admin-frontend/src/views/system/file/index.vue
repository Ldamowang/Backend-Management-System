<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.file.title') }}</span>
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
          >
            <el-button type="primary" :loading="uploadLoading">
              <el-icon><Upload /></el-icon>{{ $t('system.file.uploadFile') }}
            </el-button>
          </el-upload>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <template #empty>
          <el-empty :description="$t('system.file.noData')" :image-size="120" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="fileName" :label="$t('system.file.fileName')" min-width="200" />
        <el-table-column :label="$t('system.file.size')" width="120">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="fileExt" :label="$t('system.file.fileType')" width="80" />
        <el-table-column prop="uploader" :label="$t('system.file.uploader')" width="120" />
        <el-table-column prop="createdTime" :label="$t('system.file.uploadTime')" min-width="170" />
        <el-table-column :label="$t('common.label.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleDownload(row)">{{ $t('common.action.download') }}</el-button>
            <el-button v-permission="'sys:file:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-right"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFileList, uploadFile, deleteFile, getDownloadUrl, type FileInfo } from '@/api/modules/file'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()

const loading = ref(false)
const uploadLoading = ref(false)
const tableData = ref<FileInfo[]>([])
const { pagination, handleSizeChange, handleCurrentChange } = usePagination()

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getFileList(pagination.page, pagination.size)
    tableData.value = data.list
    pagination.total = data.total
  } finally { loading.value = false }
}

function handleBeforeUpload(file: File) {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error(t('common.message.fileSizeLimit', { size: '10MB' }))
    return false
  }
  return true
}

async function handleUpload(options: { file: File }) {
  uploadLoading.value = true
  try {
    await uploadFile(options.file)
    ElMessage.success(t('common.message.uploadSuccess'))
    fetchData()
  } catch {
    ElMessage.error(t('common.message.uploadFailed'))
  } finally { uploadLoading.value = false }
}

function handleDownload(row: FileInfo) {
  const url = getDownloadUrl(row.id)
  const link = document.createElement('a')
  link.href = url
  link.download = row.fileName
  link.click()
}

async function handleDelete(row: FileInfo) {
  await ElMessageBox.confirm(t('system.file.confirmDelete', { name: row.fileName }), t('common.message.warning'), { type: 'warning' })
  await deleteFile(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(fetchData)
watch(() => [pagination.page, pagination.size], fetchData)
</script>
