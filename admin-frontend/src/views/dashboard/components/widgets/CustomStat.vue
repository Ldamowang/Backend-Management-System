<template>
  <div class="stat-item">
    <div class="stat-icon" :style="{ backgroundColor: bgColor, color: config?.color || '#6366F1' }">
      <el-icon :size="24"><component :is="config?.icon || 'DataAnalysis'" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ displayValue }}</div>
      <div class="stat-label">{{ widget?.name || '' }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getPresetApi } from './presetApis'
import type { StatConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()
const apiValue = ref<number>(0)

const widget = computed(() => store.getCustomWidget(props.widgetId))
const config = computed<StatConfig | null>(() => widget.value ? widget.value.config as StatConfig : null)

const bgColor = computed(() => {
  const color = config.value?.color || '#6366F1'
  return color + '1A' // 10% opacity hex
})

const displayValue = computed(() => {
  const c = config.value
  if (!c) return '—'
  if (c.dataSource === 'static') return (c.staticValue ?? 0).toLocaleString()
  return apiValue.value.toLocaleString()
})

onMounted(async () => {
  const c = config.value
  if (c?.dataSource === 'api' && c.apiEndpoint && c.apiField) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      try {
        const data = await api.fetcher()
        apiValue.value = Number(data[c.apiField] ?? 0)
      } catch { /* ignore */ }
    }
  }
})
</script>

<style scoped lang="scss">
.stat-item { display: flex; align-items: center; padding: 4px 0; }
.stat-icon {
  width: 52px; height: 52px; border-radius: $border-radius-base;
  display: flex; align-items: center; justify-content: center; margin-right: 18px;
}
.stat-value {
  font-family: $font-family-heading; font-size: 28px; font-weight: 700;
  color: $text-primary; line-height: 1.2; letter-spacing: -0.02em;
}
.stat-label { font-size: $font-size-sm; color: $text-secondary; margin-top: 4px; font-weight: 500; }
</style>
