<template>
  <div class="table-toolbar">
    <div class="toolbar-left">
      <slot name="left" />
    </div>
    <div class="toolbar-right">
      <el-tooltip content="刷新">
        <el-button :icon="Refresh" circle size="small" @click="$emit('refresh')" />
      </el-tooltip>

      <el-tooltip content="密度">
        <el-popover trigger="click" width="120">
          <template #reference>
            <el-button :icon="DCaret" circle size="small" />
          </template>
          <el-radio-group :model-value="density" @update:model-value="$emit('update:density', $event)">
            <el-radio-button label="large">宽松</el-radio-button>
            <el-radio-button label="default">默认</el-radio-button>
            <el-radio-button label="small">紧凑</el-radio-button>
          </el-radio-group>
        </el-popover>
      </el-tooltip>

      <el-tooltip content="列设置">
        <el-popover trigger="click" width="180">
          <template #reference>
            <el-button :icon="Setting" circle size="small" />
          </template>
          <div class="column-settings">
            <div v-for="col in columns" :key="col.key" class="column-item">
              <el-checkbox
                :model-value="col.visible"
                @update:model-value="$emit('toggleColumn', col.key)"
              >
                {{ col.label }}
              </el-checkbox>
            </div>
            <el-divider style="margin: 8px 0" />
            <el-button size="small" text type="primary" @click="$emit('reset')">重置</el-button>
          </div>
        </el-popover>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh, DCaret, Setting } from '@element-plus/icons-vue'
import type { ColumnSetting } from '@/composables/useTableSettings'

defineProps<{
  columns: ColumnSetting[]
  density: string
}>()

defineEmits<{
  refresh: []
  'update:density': [value: string]
  toggleColumn: [key: string]
  reset: []
}>()
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-right {
  display: flex;
  gap: 4px;
}
.column-item {
  padding: 2px 0;
}
</style>
