<template>
  <div>
    <div class="grid-toolbar">
      <el-button v-if="!store.editMode" type="primary" plain size="small" @click="store.toggleEditMode()">
        编辑布局
      </el-button>
      <template v-else>
        <el-button type="success" size="small" @click="store.toggleEditMode()">完成编辑</el-button>
        <el-button size="small" @click="showAddDialog = true">+ 添加卡片</el-button>
        <el-button size="small" type="warning" @click="store.resetLayout()">恢复默认</el-button>
      </template>
    </div>

    <el-row :gutter="20" class="dashboard-grid">
      <el-col
        v-for="widgetId in store.activeWidgets"
        :key="widgetId"
        :span="getSpan(widgetId)"
        class="grid-item"
      >
        <WidgetWrapper
          :widget-id="widgetId"
          :edit-mode="store.editMode"
          @remove="store.removeWidget(widgetId)"
        />
      </el-col>
    </el-row>

    <AddWidgetDialog v-model="showAddDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getWidgetMeta } from './widgets/registry'
import WidgetWrapper from './WidgetWrapper.vue'
import AddWidgetDialog from './AddWidgetDialog.vue'

const store = useDashboardStore()
const showAddDialog = ref(false)

function getSpan(widgetId: string): number {
  return getWidgetMeta(widgetId)?.defaultSpan ?? 12
}
</script>

<style scoped lang="scss">
.grid-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.grid-item {
  margin-bottom: 20px;
}
</style>
