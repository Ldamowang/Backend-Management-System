<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.menu.title') }}</span>
          <el-button v-permission="'sys:menu:add'" type="primary" @click="handleAdd(0)">
            <el-icon><Plus /></el-icon>{{ $t('system.menu.addMenu') }}
          </el-button>
        </div>
      </template>

      <el-table :data="menuTree" v-loading="loading" row-key="id" default-expand-all>
        <el-table-column prop="menuName" :label="$t('system.menu.menuName')" min-width="180" />
        <el-table-column prop="icon" :label="$t('system.menu.icon')" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.label.type')" width="100">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)">{{ menuTypeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" :label="$t('system.menu.routePath')" min-width="160" />
        <el-table-column prop="permission" :label="$t('system.menu.permission')" min-width="160" />
        <el-table-column prop="sortOrder" :label="$t('system.menu.sort')" width="80" />
        <el-table-column :label="$t('common.label.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.enabled') : $t('common.label.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.label.operation')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 3" v-permission="'sys:menu:add'" type="primary" size="small" link @click="handleAdd(row.id)">{{ $t('common.action.add') }}</el-button>
            <el-button v-permission="'sys:menu:edit'" type="warning" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:menu:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.menu.editMenu') : t('system.menu.addMenu')" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('system.menu.menuType')" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">{{ $t('system.menu.directory') }}</el-radio>
            <el-radio :value="2">{{ $t('system.menu.menuItem') }}</el-radio>
            <el-radio :value="3">{{ $t('system.menu.button') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('system.menu.menuName')" prop="menuName">
          <el-input v-model="form.menuName" :placeholder="$t('system.menu.menuNamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" :label="$t('system.menu.icon')">
          <el-input v-model="form.icon" :placeholder="$t('system.menu.iconPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" :label="$t('system.menu.routePath')" prop="path">
          <el-input v-model="form.path" :placeholder="$t('system.menu.routePathPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" :label="$t('system.menu.component')">
          <el-input v-model="form.component" :placeholder="$t('system.menu.componentPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 1" :label="$t('system.menu.permission')">
          <el-input v-model="form.permission" :placeholder="$t('system.menu.permissionPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('common.label.sort')">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.label.status')">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/modules/menu'
import type { MenuItem, MenuForm } from '@/types/menu'

const { t } = useI18n()

const loading = ref(false)
const menuTree = ref<MenuItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<MenuForm>({
  parentId: 0, menuName: '', menuType: 1, path: '', component: '', icon: '',
  sortOrder: 0, permission: '', visible: 1, status: 1
})

const formRules = {
  menuName: [{ required: true, message: () => t('system.menu.menuNameRequired'), trigger: 'blur' }],
  menuType: [{ required: true, message: () => t('system.menu.menuTypeRequired'), trigger: 'change' }]
}

function menuTypeLabel(type: number) { return ['', t('system.menu.directory'), t('system.menu.menuItem'), t('system.menu.button')][type] }
function menuTypeTag(type: number) { return ['', '', 'success', 'warning'][type] as '' | 'success' | 'warning' }

async function fetchData() {
  loading.value = true
  try { const { data } = await getMenuTree(); menuTree.value = data }
  finally { loading.value = false }
}

function handleAdd(parentId: number) {
  isEdit.value = false
  form.parentId = parentId
  dialogVisible.value = true
}

function handleEdit(row: MenuItem) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleDelete(row: MenuItem) {
  await ElMessageBox.confirm(t('system.menu.confirmDelete', { name: row.menuName }), t('common.message.warning'), { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateMenu(form.id!, form) }
  else { await createMenu(form) }
  ElMessage.success(t(isEdit.value ? 'common.message.editSuccess' : 'common.message.addSuccess'))
  dialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, parentId: 0, menuName: '', menuType: 1, path: '', component: '', icon: '', sortOrder: 0, permission: '', visible: 1, status: 1 })
  formRef.value?.resetFields()
}

onMounted(fetchData)
</script>
