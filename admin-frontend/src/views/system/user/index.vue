<template>
  <div>
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item :label="$t('system.user.username')">
          <el-input v-model="searchForm.username" :placeholder="$t('system.user.usernamePlaceholder')" clearable class="search-input" />
        </el-form-item>
        <el-form-item :label="$t('system.user.email')">
          <el-input v-model="searchForm.email" :placeholder="$t('system.user.emailPlaceholder')" clearable class="search-input" />
        </el-form-item>
        <el-form-item :label="$t('system.user.dept')">
          <el-tree-select
            v-model="searchForm.deptId"
            :data="deptTreeData"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            :placeholder="$t('system.user.deptPlaceholder')"
            clearable
            class="search-select"
          />
        </el-form-item>
        <el-form-item :label="$t('system.user.status')">
          <el-select v-model="searchForm.status" :placeholder="$t('system.user.statusPlaceholder')" clearable class="search-select">
            <el-option :label="$t('common.label.enabled')" :value="1" />
            <el-option :label="$t('common.label.disabled')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>{{ $t('common.action.search') }}</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>{{ $t('common.action.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="card-gap">
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.user.list') }}</span>
          <div>
            <el-button v-permission="'sys:user:export'" type="success" :loading="exporting" @click="handleExport">
              <el-icon><Download /></el-icon>{{ $t('common.action.export') }}
            </el-button>
            <el-button v-permission="'sys:user:add'" @click="showImportDialog = true">
              <el-icon><Upload /></el-icon>导入
            </el-button>
            <el-button v-permission="'sys:user:add'" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>{{ $t('system.user.addUser') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" class="table-full">
        <template #empty>
          <el-empty :description="$t('system.user.noData')" :image-size="120" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="$t('system.user.username')" min-width="120" />
        <el-table-column prop="nickname" :label="$t('system.user.nickname')" min-width="120" />
        <el-table-column prop="email" :label="$t('system.user.email')" min-width="180" />
        <el-table-column prop="phone" :label="$t('system.user.phone')" min-width="140" />
        <el-table-column :label="$t('system.user.status')" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: number) => handleStatusChange(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" :label="$t('system.user.createTime')" min-width="170" />
        <el-table-column :label="$t('common.label.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:user:edit'" type="primary" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:user:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination-right"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.user.editUser') : t('system.user.addUser')" width="500px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item :label="$t('system.user.username')" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" :placeholder="$t('system.user.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="!isEdit" :label="$t('system.user.password')" prop="password">
          <el-input v-model="form.password" type="password" :placeholder="$t('system.user.passwordPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="$t('system.user.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="$t('system.user.nicknamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.user.email')" prop="email">
          <el-input v-model="form.email" :placeholder="$t('system.user.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.user.phone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="$t('system.user.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.user.dept')">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTreeData"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            :placeholder="$t('system.user.deptPlaceholder')"
            clearable
            class="table-full"
          />
        </el-form-item>
        <el-form-item :label="$t('system.user.role')" prop="roleIds">
          <el-select v-model="form.roleIds" multiple :placeholder="$t('system.user.rolePlaceholder')" class="table-full">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('system.user.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>

    <ImportDialog
      v-model="showImportDialog"
      import-url="/users/import"
      @download-template="handleDownloadTemplate"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus, downloadImportTemplate } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'
import { getDeptSimpleTree } from '@/api/modules/dept'
import { usePagination } from '@/composables/usePagination'
import { useExport } from '@/composables/useExport'
import ImportDialog from '@/components/ImportDialog.vue'
import type { UserInfo, UserForm } from '@/types/user'
import type { RoleInfo } from '@/types/role'
import type { DeptSimpleItem } from '@/types/dept'

const { t } = useI18n()

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<UserInfo[]>([])
const dialogVisible = ref(false)
const showImportDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const { pagination, handleSizeChange, handleCurrentChange } = usePagination()
const { exporting, exportData } = useExport()
const roleList = ref<RoleInfo[]>([])
const deptTreeData = ref<DeptSimpleItem[]>([])

const searchForm = reactive({ username: '', email: '', status: undefined as number | undefined, deptId: undefined as number | undefined })

const form = reactive<UserForm>({
  username: '', password: '', nickname: '', email: '', phone: '', gender: 0, status: 1, roleIds: [], deptId: undefined
})

const formRules = {
  username: [{ required: true, message: () => t('system.user.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('system.user.passwordRequired'), trigger: 'blur' }],
  email: [{ type: 'email' as const, message: () => t('system.user.emailInvalid'), trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getUserList({ ...searchForm, page: pagination.page, size: pagination.size })
    tableData.value = data.list
    pagination.total = data.total
  } catch { /* handled by interceptor */ } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; fetchData() }
function handleReset() {
  searchForm.username = ''; searchForm.email = ''; searchForm.status = undefined; searchForm.deptId = undefined
  pagination.page = 1; fetchData()
}

async function fetchRoles() {
  try {
    const { data } = await getRoleList()
    roleList.value = data
  } catch { /* handled by interceptor */ }
}

async function fetchDeptTree() {
  try {
    const { data } = await getDeptSimpleTree()
    deptTreeData.value = data
  } catch { /* handled by interceptor */ }
}

function handleExport() {
  const params: Record<string, unknown> = {}
  if (searchForm.username) params.username = searchForm.username
  if (searchForm.email) params.email = searchForm.email
  if (searchForm.status !== undefined) params.status = searchForm.status
  if (searchForm.deptId !== undefined) params.deptId = searchForm.deptId
  exportData('/users/export', params, '用户列表.xlsx')
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadImportTemplate() as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('模板下载失败')
  }
}

function handleImportSuccess() {
  fetchData()
}

function handleAdd() { isEdit.value = false; fetchRoles(); fetchDeptTree(); dialogVisible.value = true }
function handleEdit(row: UserInfo) {
  isEdit.value = true
  Object.assign(form, { ...row, password: '' })
  fetchRoles()
  fetchDeptTree()
  dialogVisible.value = true
}

async function handleDelete(row: UserInfo) {
  await ElMessageBox.confirm(t('system.user.confirmDelete', { name: row.username }), t('common.message.warning'), { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

async function handleStatusChange(id: number, status: number) {
  await updateUserStatus(id, status)
  ElMessage.success(t('common.message.statusUpdateSuccess'))
  fetchData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(form.id!, form)
    } else {
      await createUser(form)
    }
    ElMessage.success(t(isEdit.value ? 'common.message.editSuccess' : 'common.message.addSuccess'))
    dialogVisible.value = false
    fetchData()
  } finally { submitLoading.value = false }
}

function resetForm() {
  Object.assign(form, { id: undefined, username: '', password: '', nickname: '', email: '', phone: '', gender: 0, status: 1, roleIds: [], deptId: undefined })
  formRef.value?.resetFields()
}

onMounted(() => { fetchData(); fetchDeptTree() })

// 监听分页变化 - 使用 watch 监听 pagination 变化自动刷新数据
watch(
  () => [pagination.page, pagination.size],
  () => { fetchData() }
)
</script>

<style scoped lang="scss">
.search-input {
  width: 200px;
}

.search-select {
  width: 150px;
}
</style>
