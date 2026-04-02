<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.job.title') }}</span>
          <el-button v-permission="'sys:job:add'" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ $t('system.job.addJob') }}
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <template #empty>
          <el-empty :description="$t('system.job.noData')" :image-size="120" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="jobName" :label="$t('system.job.jobName')" min-width="150" />
        <el-table-column prop="jobGroup" :label="$t('system.job.jobGroup')" width="100" />
        <el-table-column prop="invokeTarget" :label="$t('system.job.invokeTarget')" min-width="200" />
        <el-table-column prop="cronExpression" :label="$t('system.job.cronExpression')" min-width="140" />
        <el-table-column :label="$t('system.job.status')" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: number) => handleStatusChange(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('common.label.createTime')" width="170" />
        <el-table-column :label="$t('common.label.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:job:run'" type="success" size="small" link @click="handleRun(row)">{{ $t('common.action.run') }}</el-button>
            <el-button type="info" size="small" link @click="handleViewLogs(row)">{{ $t('common.action.viewLog') }}</el-button>
            <el-button v-permission="'sys:job:edit'" type="primary" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:job:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.job.editJob') : t('system.job.addJob')" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('system.job.jobName')" prop="jobName">
          <el-input v-model="form.jobName" :placeholder="$t('system.job.jobNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.job.jobGroup')">
          <el-input v-model="form.jobGroup" :placeholder="$t('system.job.jobGroupPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.job.invokeTarget')" prop="invokeTarget">
          <el-input v-model="form.invokeTarget" :placeholder="$t('system.job.invokeTargetPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.job.cronExpression')" prop="cronExpression">
          <el-input v-model="form.cronExpression" :placeholder="$t('system.job.cronPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('common.label.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('common.label.description')">
          <el-input v-model="form.description" type="textarea" :placeholder="$t('system.job.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 日志对话框 -->
    <el-dialog v-model="logDialogVisible" :title="t('system.job.jobLog') + ' - ' + currentJobName" width="800px">
      <el-table :data="logList" v-loading="logLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column :label="$t('common.label.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.success') : $t('common.label.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" :label="$t('system.job.execResult')" min-width="150" />
        <el-table-column prop="errorMsg" :label="$t('system.job.errorMsg')" min-width="150" />
        <el-table-column :label="$t('system.job.duration')" width="100">
          <template #default="{ row }">{{ row.duration }}ms</template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('common.label.createTime')" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="logPagination.page"
        v-model:page-size="logPagination.size"
        :total="logPagination.total"
        layout="total, prev, pager, next"
        class="pagination-right"
        @current-change="(p: number) => { logPagination.page = p; fetchLogs() }"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getJobList, createJob, updateJob, deleteJob, changeJobStatus, runJob, getJobLogs,
  type JobInfo, type JobForm, type JobLog
} from '@/api/modules/job'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()
const loading = ref(false)
const tableData = ref<JobInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const { pagination, handleSizeChange, handleCurrentChange } = usePagination()

const form = reactive<JobForm>({
  jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '', status: 1, description: ''
})

const formRules = {
  jobName: [{ required: true, message: t('system.job.jobNameRequired'), trigger: 'blur' }],
  invokeTarget: [{ required: true, message: t('system.job.invokeTargetRequired'), trigger: 'blur' }],
  cronExpression: [{ required: true, message: t('system.job.cronRequired'), trigger: 'blur' }]
}

// 日志相关
const logDialogVisible = ref(false)
const logLoading = ref(false)
const logList = ref<JobLog[]>([])
const currentJobId = ref(0)
const currentJobName = ref('')
const logPagination = reactive({ page: 1, size: 10, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getJobList(pagination.page, pagination.size)
    tableData.value = data.list
    pagination.total = data.total
  } finally { loading.value = false }
}

function handleAdd() { isEdit.value = false; dialogVisible.value = true }

function handleEdit(row: JobInfo) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, jobName: row.jobName, jobGroup: row.jobGroup,
    invokeTarget: row.invokeTarget, cronExpression: row.cronExpression,
    status: row.status, description: row.description
  })
  dialogVisible.value = true
}

async function handleDelete(row: JobInfo) {
  await ElMessageBox.confirm(t('system.job.confirmDelete', { name: row.jobName }), t('common.message.warning'), { type: 'warning' })
  await deleteJob(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

async function handleStatusChange(id: number, status: number) {
  await changeJobStatus(id, status)
  ElMessage.success(t('common.message.statusUpdateSuccess'))
  fetchData()
}

async function handleRun(row: JobInfo) {
  await ElMessageBox.confirm(t('system.job.confirmRun', { name: row.jobName }), t('common.message.confirmAction'), { type: 'info' })
  await runJob(row.id)
  ElMessage.success(t('common.message.runSuccess'))
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateJob(form.id!, form) }
  else { await createJob(form) }
  ElMessage.success(isEdit.value ? t('common.message.editSuccess') : t('common.message.addSuccess'))
  dialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '', status: 1, description: '' })
  formRef.value?.resetFields()
}

async function handleViewLogs(row: JobInfo) {
  currentJobId.value = row.id
  currentJobName.value = row.jobName
  logPagination.page = 1
  logDialogVisible.value = true
  fetchLogs()
}

async function fetchLogs() {
  logLoading.value = true
  try {
    const { data } = await getJobLogs(currentJobId.value, logPagination.page, logPagination.size)
    logList.value = data.list
    logPagination.total = data.total
  } finally { logLoading.value = false }
}

onMounted(fetchData)
watch(() => [pagination.page, pagination.size], fetchData)
</script>
