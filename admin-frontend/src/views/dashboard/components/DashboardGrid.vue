<template>
  <div>
    <div class="grid-toolbar">
      <el-button v-if="!store.editMode" type="primary" plain size="small" class="edit-btn" @click="store.toggleEditMode()">
        编辑布局
      </el-button>
      <template v-else>
        <el-button type="success" size="small" @click="store.toggleEditMode()">完成编辑</el-button>
        <el-button size="small" @click="showAddDialog = true">+ 添加卡片</el-button>
        <el-button size="small" type="warning" @click="store.resetLayout()">恢复默认</el-button>
      </template>
    </div>

    <draggable
      v-model="store.activeWidgets"
      :item-key="(id: string) => id"
      :disabled="!store.editMode"
      :animation="200"
      ghost-class="widget-ghost"
      drag-class="widget-drag"
      class="dashboard-grid"
      @end="onDragEnd"
    >
      <template #item="{ element: widgetId }">
        <div
          class="grid-item animate-fade-in-up"
          :class="[`stagger-${store.activeWidgets.indexOf(widgetId) + 1}`, spanClass(widgetId)]"
        >
          <WidgetWrapper
            :widget-id="widgetId"
            :edit-mode="store.editMode"
            @remove="store.removeWidget(widgetId)"
          />
        </div>
      </template>
    </draggable>

    <AddWidgetDialog v-model="showAddDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import draggable from 'vuedraggable'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getWidgetMeta } from './widgets/registry'
import WidgetWrapper from './WidgetWrapper.vue'
import AddWidgetDialog from './AddWidgetDialog.vue'

const store = useDashboardStore()
const showAddDialog = ref(false)

function spanClass(widgetId: string): string {
  const span = getWidgetMeta(widgetId)?.defaultSpan ?? 12
  return `span-${span}`
}

function onDragEnd() {
  store.reorderWidgets([...store.activeWidgets])
}
</script>

<style scoped lang="scss">
.grid-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.edit-btn {
  border-radius: $border-radius-sm;
}

// ===== Flex-wrap 栅格布局 =====
.dashboard-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.grid-item {
  min-height: 0;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1),
              box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  // 栅格宽度（基于 24 列 + 20px gap）
  &.span-6  { width: calc(25% - 15px); }
  &.span-8  { width: calc(33.333% - 13.333px); }
  &.span-12 { width: calc(50% - 10px); }
  &.span-16 { width: calc(66.666% - 6.666px); }
  &.span-24 { width: 100%; }
}

// ===== 拖拽动画 =====
.widget-ghost {
  opacity: 0.4;
  border-radius: $border-radius-lg;

  :deep(.el-card) {
    border: 2px dashed $primary-color;
    background: rgba(99, 102, 241, 0.04);
  }
}

.widget-drag {
  transform: rotate(1.5deg) scale(1.02);
  box-shadow: $box-shadow-hover;
  z-index: 100;
}
</style>
