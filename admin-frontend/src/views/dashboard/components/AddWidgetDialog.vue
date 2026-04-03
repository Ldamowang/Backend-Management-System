<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="添加卡片" width="560">
    <div v-if="store.availableWidgets.length === 0" class="empty-tip">
      所有卡片已添加到布局中
    </div>
    <div v-else class="widget-grid">
      <div
        v-for="widget in store.availableWidgets"
        :key="widget.id"
        class="widget-option"
        @click="addAndClose(widget.id)"
      >
        <div class="widget-type-badge">{{ typeLabel(widget.type) }}</div>
        <div class="widget-name">{{ widget.name }}</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { useDashboardStore } from '@/stores/modules/dashboard'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const store = useDashboardStore()

function typeLabel(type: string): string {
  const map: Record<string, string> = { stat: '统计', chart: '图表', table: '表格', shortcut: '快捷' }
  return map[type] || type
}

function addAndClose(widgetId: string) {
  store.addWidget(widgetId)
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.widget-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.widget-option {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
  &:hover { border-color: var(--el-color-primary); background-color: #f0f9ff; }
}
.widget-type-badge {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.widget-name {
  font-size: 14px;
  font-weight: 500;
}
.empty-tip {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>
