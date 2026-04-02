<template>
  <div>
    <!-- 字典类型列表 -->
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.dict.title') }}</span>
          <el-button v-permission="'sys:dict:add'" type="primary" @click="handleAddType">
            <el-icon><Plus /></el-icon>{{ $t('system.dict.addType') }}
          </el-button>
        </div>
      </template>

      <el-table :data="typeList" v-loading="typeLoading" highlight-current-row @row-click="handleTypeClick">
        <el-table-column prop="dictName" :label="$t('system.dict.dictName')" min-width="150" />
        <el-table-column prop="dictType" :label="$t('system.dict.dictType')" min-width="150">
          <template #default="{ row }">
            <el-tag>{{ row.dictType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.label.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.enabled') : $t('common.label.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('common.label.description')" min-width="200" />
        <el-table-column prop="createdTime" :label="$t('common.label.createTime')" width="170" />
        <el-table-column :label="$t('common.label.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:dict:edit'" type="primary" size="small" link @click.stop="handleEditType(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:dict:delete'" type="danger" size="small" link @click.stop="handleDeleteType(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典数据列表 -->
    <el-card class="card-gap" v-if="currentType">
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.dict.dictData') }} - {{ currentType.dictName }}（{{ currentType.dictType }}）</span>
          <el-button v-permission="'sys:dict:add'" type="primary" @click="handleAddData">
            <el-icon><Plus /></el-icon>{{ $t('system.dict.addData') }}
          </el-button>
        </div>
      </template>

      <el-table :data="dataList" v-loading="dataLoading">
        <el-table-column prop="dictLabel" :label="$t('system.dict.dictLabel')" min-width="150" />
        <el-table-column prop="dictValue" :label="$t('system.dict.dictValue')" min-width="120" />
        <el-table-column prop="sortOrder" :label="$t('common.label.sort')" width="80" />
        <el-table-column :label="$t('common.label.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('common.label.enabled') : $t('common.label.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('common.label.description')" min-width="200" />
        <el-table-column :label="$t('common.label.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:dict:edit'" type="primary" size="small" link @click="handleEditData(row)">{{ $t('common.action.edit') }}</el-button>
            <el-button v-permission="'sys:dict:delete'" type="danger" size="small" link @click="handleDeleteData(row)">{{ $t('common.action.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典类型表单 -->
    <el-dialog v-model="typeDialogVisible" :title="isTypeEdit ? t('system.dict.editType') : t('system.dict.addType')" width="500px" @closed="resetTypeForm">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeFormRules" label-width="100px">
        <el-form-item :label="$t('system.dict.dictName')" prop="dictName">
          <el-input v-model="typeForm.dictName" :placeholder="$t('system.dict.dictNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.dict.dictType')" prop="dictType">
          <el-input v-model="typeForm.dictType" :placeholder="$t('system.dict.dictTypePlaceholder')" :disabled="isTypeEdit" />
        </el-form-item>
        <el-form-item :label="$t('common.label.status')">
          <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('common.label.description')">
          <el-input v-model="typeForm.description" type="textarea" :placeholder="$t('system.dict.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmitType">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据表单 -->
    <el-dialog v-model="dataDialogVisible" :title="isDataEdit ? t('system.dict.editData') : t('system.dict.addData')" width="500px" @closed="resetDataForm">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataFormRules" label-width="100px">
        <el-form-item :label="$t('system.dict.dictLabel')" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" :placeholder="$t('system.dict.dictLabelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.dict.dictValue')" prop="dictValue">
          <el-input v-model="dataForm.dictValue" :placeholder="$t('system.dict.dictValuePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('common.label.sort')">
          <el-input-number v-model="dataForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item :label="$t('common.label.status')">
          <el-switch v-model="dataForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item :label="$t('common.label.description')">
          <el-input v-model="dataForm.description" type="textarea" :placeholder="$t('system.dict.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">{{ $t('common.action.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmitData">{{ $t('common.action.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getDictTypes, createDictType, updateDictType, deleteDictType,
  getDictDataByType, createDictData, updateDictData, deleteDictData
} from '@/api/modules/dict'
import type { DictType, DictTypeForm, DictData, DictDataForm } from '@/types/dict'

const { t } = useI18n()

// ========== 字典类型 ==========
const typeLoading = ref(false)
const typeList = ref<DictType[]>([])
const typeDialogVisible = ref(false)
const isTypeEdit = ref(false)
const typeFormRef = ref<FormInstance>()
const currentType = ref<DictType | null>(null)

const typeForm = reactive<DictTypeForm>({
  dictName: '', dictType: '', status: 1, description: ''
})

const typeFormRules = {
  dictName: [{ required: true, message: t('system.dict.dictNameRequired'), trigger: 'blur' }],
  dictType: [{ required: true, message: t('system.dict.dictTypeRequired'), trigger: 'blur' }]
}

async function fetchTypes() {
  typeLoading.value = true
  try { const { data } = await getDictTypes(); typeList.value = data }
  finally { typeLoading.value = false }
}

function handleAddType() { isTypeEdit.value = false; typeDialogVisible.value = true }

function handleEditType(row: DictType) {
  isTypeEdit.value = true
  Object.assign(typeForm, { id: row.id, dictName: row.dictName, dictType: row.dictType, status: row.status, description: row.description })
  typeDialogVisible.value = true
}

async function handleDeleteType(row: DictType) {
  await ElMessageBox.confirm(t('system.dict.confirmDeleteType', { name: row.dictName }), t('common.message.warning'), { type: 'warning' })
  await deleteDictType(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  if (currentType.value?.id === row.id) { currentType.value = null; dataList.value = [] }
  fetchTypes()
}

async function handleSubmitType() {
  await typeFormRef.value?.validate()
  if (isTypeEdit.value) { await updateDictType(typeForm.id!, typeForm) }
  else { await createDictType(typeForm) }
  ElMessage.success(isTypeEdit.value ? t('common.message.editSuccess') : t('common.message.addSuccess'))
  typeDialogVisible.value = false
  fetchTypes()
}

function resetTypeForm() {
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, description: '' })
  typeFormRef.value?.resetFields()
}

function handleTypeClick(row: DictType) {
  currentType.value = row
  fetchDataList(row.dictType)
}

// ========== 字典数据 ==========
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])
const dataDialogVisible = ref(false)
const isDataEdit = ref(false)
const dataFormRef = ref<FormInstance>()

const dataForm = reactive<DictDataForm>({
  dictType: '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1, description: ''
})

const dataFormRules = {
  dictLabel: [{ required: true, message: t('system.dict.dictLabelRequired'), trigger: 'blur' }],
  dictValue: [{ required: true, message: t('system.dict.dictValueRequired'), trigger: 'blur' }]
}

async function fetchDataList(dictType: string) {
  dataLoading.value = true
  try { const { data } = await getDictDataByType(dictType); dataList.value = data }
  finally { dataLoading.value = false }
}

function handleAddData() {
  isDataEdit.value = false
  dataForm.dictType = currentType.value!.dictType
  dataDialogVisible.value = true
}

function handleEditData(row: DictData) {
  isDataEdit.value = true
  Object.assign(dataForm, {
    id: row.id, dictType: row.dictType, dictLabel: row.dictLabel,
    dictValue: row.dictValue, sortOrder: row.sortOrder, status: row.status, description: row.description
  })
  dataDialogVisible.value = true
}

async function handleDeleteData(row: DictData) {
  await ElMessageBox.confirm(t('system.dict.confirmDeleteData', { name: row.dictLabel }), t('common.message.warning'), { type: 'warning' })
  await deleteDictData(row.id)
  ElMessage.success(t('common.message.deleteSuccess'))
  fetchDataList(currentType.value!.dictType)
}

async function handleSubmitData() {
  await dataFormRef.value?.validate()
  if (isDataEdit.value) { await updateDictData(dataForm.id!, dataForm) }
  else { await createDictData(dataForm) }
  ElMessage.success(isDataEdit.value ? t('common.message.editSuccess') : t('common.message.addSuccess'))
  dataDialogVisible.value = false
  fetchDataList(currentType.value!.dictType)
}

function resetDataForm() {
  Object.assign(dataForm, { id: undefined, dictType: '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1, description: '' })
  dataFormRef.value?.resetFields()
}

onMounted(fetchTypes)
</script>
