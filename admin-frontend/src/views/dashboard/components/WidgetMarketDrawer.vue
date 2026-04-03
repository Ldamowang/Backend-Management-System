<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title=""
    size="480px"
    :with-header="false"
  >
    <div class="drawer-content">
      <!-- Header -->
      <div class="drawer-header">
        <div>
          <h3 class="drawer-title">卡片市场</h3>
          <p class="drawer-desc">浏览和管理仪表盘卡片</p>
        </div>
        <el-button :icon="Close" circle size="small" @click="$emit('update:modelValue', false)" />
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="drawer-tabs">
        <el-tab-pane label="预置卡片" name="preset">
          <!-- 自定义创建入口 -->
          <div class="create-entry" @click="openCreateWizard">
            <div class="create-icon">✨</div>
            <div class="create-text">
              <div class="create-title">自定义创建卡片</div>
              <div class="create-desc">图表 · 统计 · 便签 · 链接</div>
            </div>
          </div>

          <!-- 搜索 -->
          <el-input v-model="searchQuery" placeholder="搜索卡片..." prefix-icon="Search" clearable class="search-input" />

          <!-- 分类筛选 -->
          <div class="filter-tags">
            <el-check-tag v-for="tag in filterTags" :key="tag.value" :checked="activeFilter === tag.value" @change="activeFilter = tag.value">
              {{ tag.label }}
            </el-check-tag>
          </div>

          <!-- 卡片列表 -->
          <div class="widget-list">
            <div v-for="widget in filteredWidgets" :key="widget.id" class="widget-item" :class="{ added: isAdded(widget.id) }">
              <div class="widget-icon" :style="{ background: typeGradient(widget.type) }">
                {{ typeEmoji(widget.type) }}
              </div>
              <div class="widget-info">
                <div class="widget-name">{{ widget.name }}</div>
                <div class="widget-type-tag">
                  <el-tag size="small" :type="typeTagColor(widget.type)" effect="plain">{{ typeLabel(widget.type) }}</el-tag>
                </div>
              </div>
              <el-button v-if="!isAdded(widget.id)" type="success" size="small" plain @click="store.addWidget(widget.id)">+ 添加</el-button>
              <span v-else class="added-label">已添加 ✓</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="added">
          <template #label>
            已添加 <el-badge :value="store.activeWidgets.length" :max="99" class="tab-badge" />
          </template>

          <div class="widget-list">
            <div v-for="widgetId in store.activeWidgets" :key="widgetId" class="widget-item">
              <div class="widget-icon" :style="{ background: resolveGradient(widgetId) }">
                {{ resolveEmoji(widgetId) }}
              </div>
              <div class="widget-info">
                <div class="widget-name">
                  {{ resolveName(widgetId) }}
                  <el-tag v-if="widgetId.startsWith('custom-')" size="small" type="warning" effect="plain" class="custom-tag">自定义</el-tag>
                </div>
                <div class="widget-type-tag">
                  <el-tag size="small" effect="plain">{{ resolveTypeLabel(widgetId) }}</el-tag>
                </div>
              </div>
              <div class="action-btns">
                <el-button v-if="widgetId.startsWith('custom-')" size="small" type="primary" plain @click="editWidget(widgetId)">编辑</el-button>
                <el-button size="small" type="danger" plain @click="store.removeWidget(widgetId)">移除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 向导弹窗 -->
    <CreateWidgetWizard
      v-model="wizardVisible"
      :edit-widget="editingWidget"
      @created="onWizardDone"
      @updated="onWizardDone"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { widgetRegistry, getWidgetMeta, resolveWidgetMeta } from './widgets/registry'
import CreateWidgetWizard from './CreateWidgetWizard.vue'
import type { CustomWidget } from '@/types/widget'

defineProps<{ modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()

const store = useDashboardStore()
const activeTab = ref('preset')
const searchQuery = ref('')
const activeFilter = ref('all')
const wizardVisible = ref(false)
const editingWidget = ref<CustomWidget | null>(null)

const filterTags = [
  { label: '全部', value: 'all' },
  { label: '统计', value: 'stat' },
  { label: '图表', value: 'chart' },
  { label: '表格', value: 'table' },
  { label: '其他', value: 'shortcut' }
]

const filteredWidgets = computed(() => {
  let list = [...widgetRegistry]
  if (activeFilter.value !== 'all') {
    list = list.filter(w => w.type === activeFilter.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    list = list.filter(w => w.name.toLowerCase().includes(q))
  }
  return list
})

function isAdded(id: string): boolean {
  return store.activeWidgets.includes(id)
}

function typeEmoji(type: string): string {
  return { stat: '📊', chart: '📈', table: '📋', shortcut: '🔗' }[type] || '📦'
}

function typeGradient(type: string): string {
  const map: Record<string, string> = {
    stat: 'linear-gradient(135deg, #6366F1, #818CF8)',
    chart: 'linear-gradient(135deg, #10B981, #34D399)',
    table: 'linear-gradient(135deg, #F59E0B, #FBBF24)',
    shortcut: 'linear-gradient(135deg, #EF4444, #F87171)'
  }
  return map[type] || map.stat
}

function typeLabel(type: string): string {
  return { stat: '统计', chart: '图表', table: '表格', shortcut: '快捷' }[type] || type
}

function typeTagColor(type: string): '' | 'success' | 'warning' | 'danger' {
  return { stat: '' as const, chart: 'success' as const, table: 'warning' as const, shortcut: 'danger' as const }[type] || ''
}

function resolveName(id: string): string {
  const meta = resolveWidgetMeta(id, store.customWidgets)
  return meta?.name || id
}

function resolveTypeLabel(id: string): string {
  if (id.startsWith('custom-')) {
    const w = store.getCustomWidget(id)
    const labelMap: Record<string, string> = { chart: '图表', stat: '统计', note: '便签', link: '链接' }
    return labelMap[w?.type || ''] || ''
  }
  const meta = getWidgetMeta(id)
  return meta ? typeLabel(meta.type) : ''
}

function resolveEmoji(id: string): string {
  if (id.startsWith('custom-')) {
    const w = store.getCustomWidget(id)
    const emojiMap: Record<string, string> = { chart: '📈', stat: '🔢', note: '📝', link: '🔗' }
    return emojiMap[w?.type || ''] || '📦'
  }
  const meta = getWidgetMeta(id)
  return meta ? typeEmoji(meta.type) : '📦'
}

function resolveGradient(id: string): string {
  if (id.startsWith('custom-')) return 'linear-gradient(135deg, #8B5CF6, #A78BFA)'
  const meta = getWidgetMeta(id)
  return meta ? typeGradient(meta.type) : typeGradient('stat')
}

function openCreateWizard() {
  editingWidget.value = null
  wizardVisible.value = true
}

function editWidget(id: string) {
  editingWidget.value = store.getCustomWidget(id) || null
  wizardVisible.value = true
}

function onWizardDone() {
  editingWidget.value = null
}
</script>

<style scoped lang="scss">
.drawer-content { padding: 0; }
.drawer-header {
  display: flex; justify-content: space-between; align-items: flex-start; padding: 24px 24px 0;
}
.drawer-title { font-family: $font-family-heading; font-size: $font-size-lg; font-weight: 700; margin: 0; }
.drawer-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 2px; }
.drawer-tabs { padding: 0 24px; }

.create-entry {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  border: 1px dashed $primary-color; border-radius: $border-radius-base;
  background: rgba(99, 102, 241, 0.04); cursor: pointer; margin-bottom: 16px;
  transition: all $transition-base;
  &:hover { background: rgba(99, 102, 241, 0.08); }
}
.create-icon { font-size: 24px; }
.create-title { font-family: $font-family-heading; font-weight: 600; color: $primary-color; }
.create-desc { font-size: $font-size-xs; color: $text-secondary; margin-top: 2px; }

.search-input { margin-bottom: 12px; }

.filter-tags { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }

.widget-list { display: flex; flex-direction: column; gap: 8px; }
.widget-item {
  display: flex; align-items: center; gap: 14px; padding: 12px 16px;
  border: 1px solid $border-light; border-radius: $border-radius-base; transition: all $transition-base;
  &:hover { border-color: $border-color; }
  &.added { opacity: 0.45; }
}
.widget-icon {
  width: 40px; height: 40px; border-radius: $border-radius-sm; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
}
.widget-info { flex: 1; min-width: 0; }
.widget-name { font-weight: 600; font-size: $font-size-sm; display: flex; align-items: center; gap: 6px; }
.widget-type-tag { margin-top: 4px; }
.added-label { font-size: $font-size-xs; color: $text-secondary; white-space: nowrap; }
.custom-tag { margin-left: 4px; }
.action-btns { display: flex; gap: 6px; flex-shrink: 0; }
.tab-badge { margin-left: 6px; }
</style>
