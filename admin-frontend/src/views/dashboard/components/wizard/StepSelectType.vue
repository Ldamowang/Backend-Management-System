<template>
  <div>
    <div class="step-title">选择卡片类型</div>
    <div class="step-desc">选择你想创建的卡片类型</div>
    <div class="type-grid">
      <div
        v-for="t in types"
        :key="t.value"
        class="type-card"
        :class="{ active: modelValue === t.value }"
        @click="$emit('update:modelValue', t.value)"
      >
        <div class="type-emoji">{{ t.emoji }}</div>
        <div class="type-name">{{ t.label }}</div>
        <div class="type-desc">{{ t.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CustomWidgetType } from '@/types/widget'

defineProps<{ modelValue: CustomWidgetType | '' }>()
defineEmits<{ 'update:modelValue': [value: CustomWidgetType] }>()

const types = [
  { value: 'chart' as const, emoji: '📈', label: '图表', desc: '折线图 / 柱状图 / 饼图 / 面积图' },
  { value: 'stat' as const, emoji: '🔢', label: '统计数字', desc: '数值指标 + 趋势' },
  { value: 'note' as const, emoji: '📝', label: '富文本便签', desc: 'Markdown / 公告 / 备注' },
  { value: 'link' as const, emoji: '🔗', label: '快捷链接', desc: '自定义导航入口' }
]
</script>

<style scoped lang="scss">
.step-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; color: $text-primary; }
.step-desc { font-size: $font-size-sm; color: $text-secondary; margin: 4px 0 20px; }
.type-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.type-card {
  padding: 20px; border-radius: $border-radius-lg; border: 1px solid $border-light;
  text-align: center; cursor: pointer; transition: all $transition-base;
  &:hover { border-color: $primary-color; background: rgba(99, 102, 241, 0.04); }
  &.active { border-color: $primary-color; background: rgba(99, 102, 241, 0.08); box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); }
}
.type-emoji { font-size: 32px; margin-bottom: 8px; }
.type-name { font-family: $font-family-heading; font-size: $font-size-md; font-weight: 600; }
.type-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 4px; }
</style>
