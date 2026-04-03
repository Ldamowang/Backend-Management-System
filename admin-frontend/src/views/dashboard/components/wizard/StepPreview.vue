<template>
  <div>
    <div class="step-title">预览确认</div>
    <div class="step-desc">确认卡片配置并预览效果</div>

    <div class="preview-summary">
      <div class="summary-row"><span class="label">名称</span><span>{{ form.name }}</span></div>
      <div class="summary-row"><span class="label">类型</span><span>{{ typeLabel }}</span></div>
      <div class="summary-row"><span class="label">宽度</span><span>{{ spanLabel }}</span></div>
    </div>

    <div class="preview-card">
      <el-card shadow="never">
        <template #header><span style="font-weight:600;">{{ form.name }}</span></template>
        <div class="preview-placeholder">
          <span style="font-size:40px;">{{ typeEmoji }}</span>
          <p>卡片将在添加后渲染实际内容</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CustomWidgetType, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  widgetType: CustomWidgetType
  form: { name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }
}>()

const typeMap: Record<string, { label: string; emoji: string }> = {
  chart: { label: '图表', emoji: '📈' },
  stat: { label: '统计数字', emoji: '🔢' },
  note: { label: '富文本便签', emoji: '📝' },
  link: { label: '快捷链接', emoji: '🔗' }
}

const typeLabel = computed(() => typeMap[props.widgetType]?.label || '')
const typeEmoji = computed(() => typeMap[props.widgetType]?.emoji || '')
const spanLabel = computed(() => ({ 6: '25%', 12: '50%', 24: '100%' }[props.form.span] || '50%'))
</script>

<style scoped lang="scss">
.step-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; color: $text-primary; }
.step-desc { font-size: $font-size-sm; color: $text-secondary; margin: 4px 0 20px; }
.preview-summary { margin-bottom: 20px; }
.summary-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid $border-light; font-size: $font-size-base; }
.label { color: $text-secondary; font-weight: 500; }
.preview-card { margin-top: 16px; }
.preview-placeholder { text-align: center; padding: 24px; color: $text-secondary; font-size: $font-size-sm; }
</style>
