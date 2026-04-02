<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.notice.title') }}</span>
          <el-button v-permission="'sys:notice:add'" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ $t('system.notice.addNotice') }}
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <template #empty>
          <el-empty :description="$t('system.notice.noData')" :image-size="120" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" :label="$t('system.notice.noticeTitle')" min-width="200" />
        <el-table-column :label="$t('system.notice.noticeType')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.noticeType === 1 ? '' : 'warning'" size="small">
              {{ row.noticeType === 1 ? $t('system.notice.notification') : $t('system.notice.announcement') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.label.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? $t('system.notice.published') : $t('system.notice.draft') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" :label="$t('common.label.createdBy')" width="120" />
        <el-table-column prop="createdTime" :label="$t('common.label.createTime')" width="170" />
        <el-table-column :label="$t('common.label.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-permission="'sys:notice:edit'" type="success" size="small" link @click="handlePublish(row)">{{ $t('common.action.publish') }}</el-button>
            <el-button v-permission="'sys:notice:edit'" type="primary" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:notice:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.notice.editNotice') : t('system.notice.addNotice')" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item :label="$t('system.notice.noticeTitle')" prop="title">
          <el-input v-model="form.title" :placeholder="$t('system.notice.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.notice.noticeType')">
          <el-radio-group v-model="form.noticeType">
            <el-radio :value="1">{{ $t('system.notice.notification') }}</el-radio>
            <el-radio :value="2">{{ $t('system.notice.announcement') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('system.notice.content')">
          <el-input v-model="form.content" type="textarea" :rows="6" :placeholder="$t('system.notice.contentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getNoticeList, createNotice, updateNotice, deleteNotice, publishNotice, type NoticeInfo, type NoticeForm } from '@/api/modules/notice'
import { usePagination } from '@/composables/usePagination'

const { t } = useI18n()

const loading = ref(false)
const tableData = ref<NoticeInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const { pagination, handleSizeChange, handleCurrentChange } = usePagination()

const form = reactive<NoticeForm>({
  title: '', content: '', noticeType: 1, status: 0
})

const formRules = {
  title: [{ required: true, message: t('system.notice.titleRequired'), trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getNoticeList(pagination.page, pagination.size)
    tableData.value = data.list
    pagination.total = data.total
  } finally { loading.value = false }
}

function handleAdd() { isEdit.value = false; dialogVisible.value = true }

function handleEdit(row: NoticeInfo) {
  isEdit.value = true
  Object.assign(form, { id: row.id, title: row.title, content: row.content, noticeType: row.noticeType, status: row.status })
  dialogVisible.value = true
}

async function handleDelete(row: NoticeInfo) {
  await ElMessageBox.confirm(t('system.notice.confirmDelete', { name: row.title }), t('common.message.warning'), { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

async function handlePublish(row: NoticeInfo) {
  await ElMessageBox.confirm(t('system.notice.confirmPublish', { name: row.title }), t('common.message.confirmAction'), { type: 'info' })
  await publishNotice(row.id)
  ElMessage.success(t('common.message.publishSuccess'))
  fetchData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateNotice(form.id!, form) }
  else { await createNotice(form) }
  ElMessage.success(isEdit.value ? t('common.message.editSuccess') : t('common.message.addSuccess'))
  dialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, title: '', content: '', noticeType: 1, status: 0 })
  formRef.value?.resetFields()
}

onMounted(fetchData)
watch(() => [pagination.page, pagination.size], fetchData)
</script>
