<template>
  <div class="search-result-item" :class="{ selected }" @click="emit('select')">
    <el-icon class="result-icon"><component :is="icon || 'Search'" /></el-icon>
    <div class="result-content">
      <span class="result-title" v-html="highlightedTitle" />
      <span class="result-category">{{ categoryLabel }}</span>
    </div>
    <el-icon v-if="selected" class="enter-icon"><Right /></el-icon>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SearchResult } from '@/composables/useGlobalSearch'

const props = defineProps<{
  result: SearchResult
  keyword: string
  selected: boolean
}>()

const emit = defineEmits<{ select: [] }>()
const { t } = useI18n()

const icon = computed(() => props.result.icon)

const categoryLabel = computed(() =>
  t(`common.search.category.${props.result.category}`)
)

const highlightedTitle = computed(() => {
  if (!props.keyword) return props.result.title
  const regex = new RegExp(`(${props.keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return props.result.title.replace(regex, '<mark>$1</mark>')
})
</script>

<style scoped lang="scss">
.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 4px;

  &:hover, &.selected {
    background: var(--el-fill-color-light);
  }
}

.result-icon {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  :deep(mark) {
    background: var(--el-color-primary-light-7);
    color: var(--el-color-primary);
    padding: 0 2px;
    border-radius: 2px;
  }
}

.result-category {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  margin-left: 12px;
}

.enter-icon {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
</style>
