<template>
  <el-card shadow="hover" class="widget-wrapper" :class="{ 'edit-mode': editMode }">
    <template #header v-if="meta">
      <div class="widget-header">
        <span v-if="editMode" class="drag-handle">&#x2807;</span>
        <span>{{ meta.name }}</span>
        <el-button
          v-if="editMode"
          type="danger"
          :icon="Close"
          circle
          size="small"
          class="remove-btn"
          @click="$emit('remove')"
        />
      </div>
    </template>
    <component :is="asyncComponent" :widget-id="widgetId" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { getWidgetMeta } from './widgets/registry'

const props = defineProps<{
  widgetId: string
  editMode: boolean
}>()

defineEmits<{ remove: [] }>()

const meta = computed(() => getWidgetMeta(props.widgetId))

const asyncComponent = computed(() => {
  const m = meta.value
  if (!m) return null
  return defineAsyncComponent(m.component)
})
</script>

<style scoped lang="scss">
.widget-wrapper.edit-mode {
  border: 2px dashed var(--el-color-primary);
}
.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: #909399;
  user-select: none;
}
.remove-btn {
  margin-left: auto;
}
</style>
