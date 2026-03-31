<template>
  <div>
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable class="search-input" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="searchForm.email" placeholder="请输入邮箱" clearable class="search-input" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable class="search-select">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="card-gap">
      <template #header>
        <div class="flex-between">
          <span>用户列表</span>
          <div>
            <el-button v-permission="'sys:user:export'" type="success" :loading="exportLoading" @click="handleExport">
              <el-icon><Download /></el-icon>导出
            </el-button>
            <el-button v-permission="'sys:user:add'" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" class="table-full">
        <template #empty>
          <el-empty description="暂无用户数据" :image-size="120" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val: number) => handleStatusChange(row.id, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:user:edit'" type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'sys:user:delete'" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" class="table-full">
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, updateUserStatus, exportUsers } from '@/api/modules/user'
import { getRoleList } from '@/api/modules/role'
import { usePagination } from '@/composables/usePagination'
import type { UserInfo, UserForm } from '@/types/user'
import type { RoleInfo } from '@/types/role'

const loading = ref(false)
const submitLoading = ref(false)
const exportLoading = ref(false)
const tableData = ref<UserInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const { pagination, handleSizeChange, handleCurrentChange } = usePagination()
const roleList = ref<RoleInfo[]>([])

const searchForm = reactive({ username: '', email: '', status: undefined as number | undefined })

const form = reactive<UserForm>({
  username: '', password: '', nickname: '', email: '', phone: '', gender: 0, status: 1, roleIds: []
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ type: 'email' as const, message: '请输入有效邮箱', trigger: 'blur' }]
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
  searchForm.username = ''; searchForm.email = ''; searchForm.status = undefined
  pagination.page = 1; fetchData()
}

async function fetchRoles() {
  try {
    const { data } = await getRoleList()
    roleList.value = data
  } catch { /* handled by interceptor */ }
}

async function handleExport() {
  exportLoading.value = true
  try {
    const blob = await exportUsers(searchForm) as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户列表.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

function handleAdd() { isEdit.value = false; fetchRoles(); dialogVisible.value = true }
function handleEdit(row: UserInfo) {
  isEdit.value = true
  Object.assign(form, { ...row, password: '' })
  fetchRoles()
  dialogVisible.value = true
}

async function handleDelete(row: UserInfo) {
  await ElMessageBox.confirm(`确定删除用户 "${row.username}"？`, '警告', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleStatusChange(id: number, status: number) {
  await updateUserStatus(id, status)
  ElMessage.success('状态更新成功')
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
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitLoading.value = false }
}

function resetForm() {
  Object.assign(form, { id: undefined, username: '', password: '', nickname: '', email: '', phone: '', gender: 0, status: 1, roleIds: [] })
  formRef.value?.resetFields()
}

onMounted(fetchData)

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
