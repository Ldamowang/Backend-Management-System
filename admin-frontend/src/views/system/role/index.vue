<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.role.title') }}</span>
          <el-button v-permission="'sys:role:add'" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ $t('system.role.addRole') }}
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" class="table-full">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" :label="$t('system.role.roleName')" min-width="140" />
        <el-table-column prop="roleKey" :label="$t('system.role.roleKey')" min-width="140" />
        <el-table-column prop="sortOrder" :label="$t('system.role.sort')" width="80" />
        <el-table-column :label="$t('system.role.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? $t('common.label.enabled') : $t('common.label.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('system.role.description')" min-width="200" />
        <el-table-column :label="$t('common.label.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:role:edit'" type="primary" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:role:edit'" type="warning" size="small" link @click="handleAssignMenu(row)">{{ $t('common.action.assignPermission') }}</el-button>
            <el-button v-permission="'sys:role:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.role.editRole') : t('system.role.addRole')" width="500px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item :label="$t('system.role.roleName')" prop="roleName">
          <el-input v-model="form.roleName" :placeholder="$t('system.role.roleNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.role.roleKey')" prop="roleKey">
          <el-input v-model="form.roleKey" :placeholder="$t('system.role.roleKeyPlaceholder')" :disabled="isEdit" />
        </el-form-item>
        <el-form-item :label="$t('system.role.sort')" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('system.role.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('system.role.description')">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="$t('system.role.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限对话框 -->
    <el-dialog v-model="menuDialogVisible" :title="$t('system.role.assignMenu')" width="500px">
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleMenuSubmit">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole, assignMenus } from '@/api/modules/role'
import { getMenuTree } from '@/api/modules/menu'
import type { RoleInfo, RoleForm } from '@/types/role'
import type { MenuItem } from '@/types/menu'

const { t } = useI18n()

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
  roleName: [{ required: true, message: () => t('system.role.roleNameRequired'), trigger: 'blur' }],
  roleKey: [{ required: true, message: () => t('system.role.roleKeyRequired'), trigger: 'blur' }]
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
  await ElMessageBox.confirm(t('system.role.confirmDelete', { name: row.roleName }), t('common.message.warning'), { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
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
  ElMessage.success(t(isEdit.value ? 'common.message.editSuccess' : 'common.message.addSuccess'))
  dialogVisible.value = false
  fetchData()
}

async function handleMenuSubmit() {
  const checkedKeys = treeRef.value?.getCheckedKeys(false) as number[]
  const halfCheckedKeys = treeRef.value?.getHalfCheckedKeys() as number[]
  await assignMenus(currentRoleId.value, [...checkedKeys, ...halfCheckedKeys])
  ElMessage.success(t('common.message.assignSuccess'))
  menuDialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, roleName: '', roleKey: '', sortOrder: 0, status: 1, description: '', menuIds: [] })
  formRef.value?.resetFields()
}

onMounted(fetchData)
</script>
