<template>
  <el-card class="widget-wrapper" :class="{ 'edit-mode': editMode }" shadow="never">
    <template #header v-if="meta">
      <div class="widget-header">
        <span v-if="editMode" class="drag-handle">&#x2807;</span>
        <span class="widget-title">{{ meta.name }}</span>
        <div v-if="editMode" class="header-actions">
          <el-button
            v-if="isCustom"
            type="primary"
            :icon="EditPen"
            circle
            size="small"
            @click="$emit('edit')"
          />
          <el-button
            type="danger"
            :icon="Close"
            circle
            size="small"
            @click="$emit('remove')"
          />
        </div>
      </div>
    </template>
    <component :is="asyncComponent" :widget-id="widgetId" />
  </el-card>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { Close, EditPen } from '@element-plus/icons-vue'
import { resolveWidgetMeta } from './widgets/registry'
import { useDashboardStore } from '@/stores/modules/dashboard'

const props = defineProps<{
  widgetId: string
  editMode: boolean
}>()

defineEmits<{ remove: []; edit: [] }>()

const store = useDashboardStore()

const isCustom = computed(() => props.widgetId.startsWith('custom-'))

const meta = computed(() => resolveWidgetMeta(props.widgetId, store.customWidgets))

const asyncComponent = computed(() => {
  const m = meta.value
  if (!m) return null
  return defineAsyncComponent(m.component)
})
</script>

<style scoped lang="scss">
.widget-wrapper {
  height: 100%; border: 1px solid $border-light; box-shadow: $box-shadow-sm;
  transition: box-shadow $transition-base, transform $transition-base, border-color $transition-base;
  &:hover { box-shadow: $box-shadow-base; border-color: $border-color; }
  &.edit-mode { border: 2px dashed $primary-color; background: rgba(99, 102, 241, 0.02); cursor: grab; &:active { cursor: grabbing; } }
}
.widget-header { display: flex; align-items: center; gap: 8px; }
.widget-title { font-family: $font-family-heading; font-weight: 600; font-size: $font-size-base; color: $text-primary; }
.drag-handle { cursor: grab; font-size: 18px; color: $text-secondary; user-select: none; &:hover { color: $primary-color; } }
.header-actions { margin-left: auto; display: flex; gap: 4px; }
</style>
