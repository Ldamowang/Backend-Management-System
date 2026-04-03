<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="isEdit ? '编辑自定义卡片' : '创建自定义卡片'"
    width="600px"
    :close-on-click-modal="false"
  >
    <!-- Steps 指示器 -->
    <div class="wizard-steps">
      <div v-for="s in steps" :key="s.step" class="wizard-step" :class="{ active: step === s.step, done: step > s.step }">
        <div class="step-circle">{{ step > s.step ? '✓' : s.step }}</div>
        <span class="step-label">{{ s.label }}</span>
      </div>
    </div>

    <!-- Step 内容 -->
    <StepSelectType v-if="step === 1" v-model="widgetType" />
    <StepConfigure v-if="step === 2" :widget-type="(widgetType as CustomWidgetType)" :form="form" />
    <StepPreview v-if="step === 3" :widget-type="(widgetType as CustomWidgetType)" :form="form" />

    <template #footer>
      <el-button v-if="step > 1" @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" :disabled="!canNext" @click="step++">下一步</el-button>
      <el-button v-if="step === 3" type="primary" @click="handleSubmit">
        {{ isEdit ? '保存修改' : '创建并添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import StepSelectType from './wizard/StepSelectType.vue'
import StepConfigure from './wizard/StepConfigure.vue'
import StepPreview from './wizard/StepPreview.vue'
import type { CustomWidgetType, CustomWidget, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  modelValue: boolean
  editWidget?: CustomWidget | null
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: []
  updated: []
}>()

const store = useDashboardStore()
const step = ref(1)

const isEdit = computed(() => !!props.editWidget)

const widgetType = ref<CustomWidgetType | ''>('')

const defaultConfigs: Record<CustomWidgetType, () => ChartConfig | StatConfig | NoteConfig | LinkConfig> = {
  chart: () => ({ chartType: 'line', dataSource: 'api', apiEndpoint: '', staticData: { labels: [], values: [] } }),
  stat: () => ({ dataSource: 'api', apiEndpoint: '', apiField: '', staticValue: 0, icon: 'DataAnalysis', color: '#6366F1' }),
  note: () => ({ content: '' }),
  link: () => ({ links: [{ name: '', path: '', icon: 'Link', color: '#6366F1' }] })
}

const form = ref<{ name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }>({
  name: '',
  span: 12,
  config: { chartType: 'line', dataSource: 'api' } as ChartConfig
})

// 类型切换时重置 config
watch(widgetType, (t) => {
  if (t && !isEdit.value) {
    form.value.config = defaultConfigs[t]()
  }
})

// 编辑模式：预填数据
watch(() => props.modelValue, (visible) => {
  if (visible && props.editWidget) {
    widgetType.value = props.editWidget.type
    form.value = {
      name: props.editWidget.name,
      span: props.editWidget.span,
      config: JSON.parse(JSON.stringify(props.editWidget.config))
    }
    step.value = 2 // 编辑时跳过类型选择
  } else if (visible) {
    step.value = 1
    widgetType.value = ''
    form.value = { name: '', span: 12, config: { chartType: 'line', dataSource: 'api' } as ChartConfig }
  }
})

const canNext = computed(() => {
  if (step.value === 1) return widgetType.value !== ''
  if (step.value === 2) return form.value.name.trim() !== ''
  return true
})

const steps = [
  { step: 1, label: '选择类型' },
  { step: 2, label: '配置内容' },
  { step: 3, label: '预览确认' }
]

function handleSubmit() {
  if (isEdit.value && props.editWidget) {
    store.updateCustomWidget(props.editWidget.id, {
      name: form.value.name,
      type: widgetType.value as CustomWidgetType,
      span: form.value.span,
      config: JSON.parse(JSON.stringify(form.value.config))
    })
    emit('updated')
  } else {
    const widget: CustomWidget = {
      id: `custom-${Date.now()}`,
      name: form.value.name,
      type: widgetType.value as CustomWidgetType,
      span: form.value.span,
      config: JSON.parse(JSON.stringify(form.value.config)),
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
    store.addCustomWidget(widget)
    emit('created')
  }
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.wizard-steps {
  display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 28px;
}
.wizard-step {
  display: flex; align-items: center; gap: 8px;
  &:not(:last-child)::after { content: ''; width: 40px; height: 1px; background: $border-color; margin: 0 12px; }
}
.step-circle {
  width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: $font-size-sm; font-weight: 700; background: $border-light; color: $text-secondary; transition: all $transition-base;
  .active & { background: $primary-color; color: #fff; }
  .done & { background: $success-color; color: #fff; }
}
.step-label {
  font-size: $font-size-sm; color: $text-secondary; font-weight: 500;
  .active & { color: $primary-color; font-weight: 600; }
}
</style>
