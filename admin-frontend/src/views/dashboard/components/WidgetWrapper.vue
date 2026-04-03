<template>
  <el-card class="widget-wrapper" :class="{ 'edit-mode': editMode }" shadow="never">
    <template #header v-if="meta">
      <div class="widget-header">
        <span v-if="editMode" class="drag-handle">&#x2807;</span>
        <span class="widget-title">{{ meta.name }}</span>
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
.widget-wrapper {
  height: 100%;
  border: 1px solid $border-light;
  box-shadow: $box-shadow-sm;
  transition: box-shadow $transition-base, transform $transition-base, border-color $transition-base;

  &:hover {
    box-shadow: $box-shadow-base;
    border-color: $border-color;
  }

  &.edit-mode {
    border: 2px dashed $primary-color;
    background: rgba(99, 102, 241, 0.02);
    cursor: grab;

    &:active {
      cursor: grabbing;
    }
  }
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.widget-title {
  font-family: $font-family-heading;
  font-weight: 600;
  font-size: $font-size-base;
  color: $text-primary;
}

.drag-handle {
  cursor: grab;
  font-size: 18px;
  color: $text-secondary;
  user-select: none;
  transition: color $transition-fast;
  &:hover { color: $primary-color; }
}

.remove-btn {
  margin-left: auto;
}
</style>
