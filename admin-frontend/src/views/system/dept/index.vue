<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.dept.title') }}</span>
          <el-button v-permission="'sys:dept:add'" type="primary" @click="handleAdd(0)">
            <el-icon><Plus /></el-icon>{{ $t('system.dept.addDept') }}
          </el-button>
        </div>
      </template>

      <el-table :data="deptTree" v-loading="loading" row-key="id" default-expand-all>
        <el-table-column prop="deptName" :label="$t('system.dept.deptName')" min-width="200" />
        <el-table-column prop="leader" :label="$t('system.dept.leader')" width="120" />
        <el-table-column prop="phone" :label="$t('system.dept.phone')" width="140" />
        <el-table-column prop="email" :label="$t('system.dept.email')" min-width="180" />
        <el-table-column prop="sortOrder" :label="$t('system.dept.sort')" width="80" />
        <el-table-column :label="$t('common.label.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.enabled') : $t('common.label.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.label.operation')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:dept:add'" type="primary" size="small" link @click="handleAdd(row.id)">{{ $t('common.action.add') }}</el-button>
            <el-button v-permission="'sys:dept:edit'" type="warning" size="small" link @click="handleEdit(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:dept:delete'" type="danger" size="small" link @click="handleDelete(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? t('system.dept.editDept') : t('system.dept.addDept')" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="$t('system.dept.parentDept')">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeData"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            :render-after-expand="false"
            :placeholder="$t('system.dept.parentDeptPlaceholder')"
            clearable
            class="table-full"
          />
        </el-form-item>
        <el-form-item :label="$t('system.dept.deptName')" prop="deptName">
          <el-input v-model="form.deptName" :placeholder="$t('system.dept.deptNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.dept.leader')">
          <el-input v-model="form.leader" :placeholder="$t('system.dept.leaderPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.dept.phone')">
          <el-input v-model="form.phone" :placeholder="$t('system.dept.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.dept.email')">
          <el-input v-model="form.email" :placeholder="$t('system.dept.emailPlaceholder')" />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getDeptTree, createDept, updateDept, deleteDept } from '@/api/modules/dept'
import type { DeptItem, DeptForm } from '@/types/dept'

const { t } = useI18n()

const loading = ref(false)
const deptTree = ref<DeptItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<DeptForm>({
  parentId: 0, deptName: '', sortOrder: 0, leader: '', phone: '', email: '', status: 1
})

const formRules = {
  deptName: [{ required: true, message: () => t('system.dept.deptNameRequired'), trigger: 'blur' }]
}

const parentTreeData = computed(() => {
  const topNode = { id: 0, deptName: t('system.dept.topDept'), children: deptTree.value }
  return [topNode]
})

async function fetchData() {
  loading.value = true
  try { const { data } = await getDeptTree(); deptTree.value = data }
  finally { loading.value = false }
}

function handleAdd(parentId: number) {
  isEdit.value = false
  form.parentId = parentId
  dialogVisible.value = true
}

function handleEdit(row: DeptItem) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id, parentId: row.parentId, deptName: row.deptName,
    sortOrder: row.sortOrder, leader: row.leader, phone: row.phone,
    email: row.email, status: row.status
  })
  dialogVisible.value = true
}

async function handleDelete(row: DeptItem) {
  await ElMessageBox.confirm(t('system.dept.confirmDelete', { name: row.deptName }), t('common.message.warning'), { type: 'warning' })
  await deleteDept(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateDept(form.id!, form) }
  else { await createDept(form) }
  ElMessage.success(t(isEdit.value ? 'common.message.editSuccess' : 'common.message.addSuccess'))
  dialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, parentId: 0, deptName: '', sortOrder: 0, leader: '', phone: '', email: '', status: 1 })
  formRef.value?.resetFields()
}

onMounted(fetchData)
</script>
