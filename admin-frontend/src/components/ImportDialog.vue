<template>
  <el-dialog
    v-model="visible"
    title="数据导入"
    width="520px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <!-- 上传区域 -->
    <div v-if="!result">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
      >
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div class="upload-text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="upload-tip">
            仅支持 .xlsx / .xls 文件
            <el-button type="primary" link @click="$emit('download-template')">
              下载导入模板
            </el-button>
          </div>
        </template>
      </el-upload>
    </div>

    <!-- 导入结果 -->
    <div v-else>
      <el-result
        :icon="result.failCount === 0 ? 'success' : 'warning'"
        :title="`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`"
      />
      <el-table
        v-if="result.errors.length > 0"
        :data="result.errors"
        max-height="300"
        style="margin-top: 12px"
      >
        <el-table-column label="行号" prop="row" width="70" />
        <el-table-column label="字段" prop="field" width="120" />
        <el-table-column label="错误信息" prop="message" />
      </el-table>
    </div>

    <template #footer>
      <template v-if="!result">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="handleImport">
          确认导入
        </el-button>
      </template>
      <el-button v-else type="primary" @click="handleClose">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { useImport } from '@/composables/useImport'
import type { ImportResultData } from '@/composables/useImport'
import type { UploadFile } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  importUrl: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'download-template': []
  'success': []
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, (val) => { visible.value = val })
watch(visible, (val) => { emit('update:modelValue', val) })

const selectedFile = ref<File | null>(null)
const result = ref<ImportResultData | null>(null)
const { importing, importData } = useImport()

const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw || null
}

const handleImport = async () => {
  if (!selectedFile.value) return
  result.value = await importData(props.importUrl, selectedFile.value)
  if (result.value.failCount === 0) {
    emit('success')
  }
}

const handleClose = () => {
  visible.value = false
  selectedFile.value = null
  result.value = null
}
</script>

<style scoped>
.upload-text {
  color: var(--el-text-color-regular);
  margin-top: 8px;
}

.upload-tip {
  margin-top: 8px;
}
</style>
