<template>
  <div class="link-grid">
    <button
      v-for="link in links"
      :key="link.path"
      class="link-item"
      @click="router.push(link.path)"
    >
      <div class="link-icon" :style="{ background: link.color || '#6366F1' }">
        <el-icon :size="20" color="#fff"><component :is="link.icon || 'Link'" /></el-icon>
      </div>
      <span class="link-label">{{ link.name }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/modules/dashboard'
import type { LinkConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const router = useRouter()
const store = useDashboardStore()

const links = computed(() => {
  const w = store.getCustomWidget(props.widgetId)
  if (!w) return []
  return (w.config as LinkConfig).links || []
})
</script>

<style scoped lang="scss">
.link-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.link-item {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 20px 12px; border-radius: $border-radius-base; cursor: pointer;
  border: 1px solid transparent; background: transparent; transition: all $transition-base;
  &:hover {
    background: $border-lighter; border-color: $border-light; transform: translateY(-2px);
    .link-icon { transform: scale(1.1); }
  }
}
.link-icon {
  width: 44px; height: 44px; border-radius: $border-radius-base;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 3px 8px rgba(0,0,0,0.08); transition: all $transition-base;
}
.link-label { font-size: $font-size-sm; font-weight: 500; color: $text-regular; }
</style>
