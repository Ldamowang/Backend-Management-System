<template>
  <div class="note-content" v-html="renderedContent"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import type { NoteConfig } from '@/types/widget'

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()

const config = computed<NoteConfig | null>(() => {
  const w = store.getCustomWidget(props.widgetId)
  return w ? w.config as NoteConfig : null
})

const renderedContent = computed(() => {
  const content = config.value?.content || ''
  return content
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    .replace(/\n/g, '<br>')
})
</script>

<style scoped lang="scss">
.note-content {
  font-size: $font-size-base;
  line-height: 1.7;
  color: $text-regular;
  min-height: 60px;

  :deep(h2) { font-size: $font-size-xl; font-weight: 700; margin-bottom: 8px; color: $text-primary; }
  :deep(h3) { font-size: $font-size-lg; font-weight: 600; margin-bottom: 6px; color: $text-primary; }
  :deep(h4) { font-size: $font-size-md; font-weight: 600; margin-bottom: 4px; color: $text-primary; }
  :deep(strong) { font-weight: 600; color: $text-primary; }
  :deep(ul) { padding-left: 20px; margin: 8px 0; }
  :deep(li) { margin: 4px 0; }
}
</style>
