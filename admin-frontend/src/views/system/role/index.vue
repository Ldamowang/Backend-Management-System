<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>角色管理</span>
          <el-button v-permission="'sys:role:add'" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="角色标识" min-width="140" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:role:edit'" type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'sys:role:edit'" type="warning" size="small" link @click="handleAssignMenu(row)">分配权限</el-button>
            <el-button v-permission="'sys:role:delete'" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="请输入角色标识" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMenuSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, assignMenus } from '@/api/modules/role'
import { getMenuTree } from '@/api/modules/menu'
import type { RoleInfo, RoleForm } from '@/types/role'
import type { MenuItem } from '@/types/menu'

const loading = ref(false)
const tableData = ref<RoleInfo[]>([])
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const treeRef = ref<InstanceType<typeof ElTree>>()

const menuTree = ref<MenuItem[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRoleId = ref<number>(0)

const form = reactive<RoleForm>({
  roleName: '', roleKey: '', sortOrder: 0, status: 1, description: '', menuIds: []
})

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getRoleList()
    tableData.value = data
  } finally { loading.value = false }
}

function handleAdd() { isEdit.value = false; dialogVisible.value = true }
function handleEdit(row: RoleInfo) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row: RoleInfo) {
  await ElMessageBox.confirm(`确定删除角色 "${row.roleName}"？`, '警告', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleAssignMenu(row: RoleInfo) {
  currentRoleId.value = row.id
  const { data } = await getMenuTree()
  menuTree.value = data
  checkedMenuIds.value = row.menuIds || []
  menuDialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateRole(form.id!, form) }
  else { await createRole(form) }
  ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  fetchData()
}

async function handleMenuSubmit() {
  const checkedKeys = treeRef.value?.getCheckedKeys(false) as number[]
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[]
  await assignMenus(currentRoleId.value, [...checkedKeys, ...halfCheckedKeys])
  ElMessage.success('权限分配成功')
  menuDialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, roleName: '', roleKey: '', sortOrder: 0, status: 1, description: '', menuIds: [] })
  formRef.value?.resetFields()
}

onMounted(fetchData)
</script>
