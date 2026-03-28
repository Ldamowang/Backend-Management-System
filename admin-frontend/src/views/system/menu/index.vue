<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>菜单管理</span>
          <el-button v-permission="'sys:menu:add'" type="primary" @click="handleAdd(0)">
            <el-icon><Plus /></el-icon>新增菜单
          </el-button>
        </div>
      </template>

      <el-table :data="menuTree" v-loading="loading" row-key="id" default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)">{{ menuTypeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="permission" label="权限标识" min-width="160" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.menuType !== 3" v-permission="'sys:menu:add'" type="primary" size="small" link @click="handleAdd(row.id)">新增</el-button>
            <el-button v-permission="'sys:menu:edit'" type="warning" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'sys:menu:delete'" type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" label="图标">
          <el-input v-model="form.icon" placeholder="图标名称 (如 User, Setting)" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 3" label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" label="组件路径">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 1" label="权限标识">
          <el-input v-model="form.permission" placeholder="如 sys:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/modules/menu'
import type { MenuItem, MenuForm } from '@/types/menu'

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
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

function menuTypeLabel(type: number) { return ['', '目录', '菜单', '按钮'][type] }
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
  await ElMessageBox.confirm(`确定删除菜单 "${row.menuName}"？`, '警告', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await updateMenu(form.id!, form) }
  else { await createMenu(form) }
  ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
  dialogVisible.value = false
  fetchData()
}

function resetForm() {
  Object.assign(form, { id: undefined, parentId: 0, menuName: '', menuType: 1, path: '', component: '', icon: '', sortOrder: 0, permission: '', visible: 1, status: 1 })
  formRef.value?.resetFields()
}

onMounted(fetchData)
</script>
