<template>
  <div>
    <!-- 公共字段：标题 + 宽度 -->
    <el-form label-position="top" :model="form">
      <el-form-item label="卡片标题" required>
        <el-input v-model="form.name" placeholder="输入卡片名称" />
      </el-form-item>

      <el-form-item label="宽度">
        <el-radio-group v-model="form.span">
          <el-radio-button :value="6">25%</el-radio-button>
          <el-radio-button :value="12">50%</el-radio-button>
          <el-radio-button :value="24">100%</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <!-- 图表配置 -->
      <template v-if="widgetType === 'chart'">
        <el-form-item label="图表类型">
          <el-radio-group v-model="(form.config as ChartConfig).chartType">
            <el-radio-button value="line">折线图</el-radio-button>
            <el-radio-button value="bar">柱状图</el-radio-button>
            <el-radio-button value="pie">饼图</el-radio-button>
            <el-radio-button value="area">面积图</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据来源">
          <el-radio-group v-model="(form.config as ChartConfig).dataSource">
            <el-radio-button value="api">预置 API</el-radio-button>
            <el-radio-button value="static">静态数据</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="(form.config as ChartConfig).dataSource === 'api'" label="API 端点">
          <el-select v-model="(form.config as ChartConfig).apiEndpoint" placeholder="选择数据源">
            <el-option v-for="api in presetApis" :key="api.key" :label="api.name" :value="api.key">
              <div>{{ api.name }}</div>
              <div style="font-size:12px;color:#94A3B8;">{{ api.description }}</div>
            </el-option>
          </el-select>
        </el-form-item>
        <template v-if="(form.config as ChartConfig).dataSource === 'static'">
          <el-form-item label="标签（逗号分隔）">
            <el-input v-model="staticLabelsStr" placeholder="如：一月,二月,三月" />
          </el-form-item>
          <el-form-item label="数值（逗号分隔）">
            <el-input v-model="staticValuesStr" placeholder="如：100,200,300" />
          </el-form-item>
        </template>
      </template>

      <!-- 统计配置 -->
      <template v-if="widgetType === 'stat'">
        <el-form-item label="数据来源">
          <el-radio-group v-model="(form.config as StatConfig).dataSource">
            <el-radio-button value="api">预置 API</el-radio-button>
            <el-radio-button value="static">静态数值</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'api'" label="API 端点">
          <el-select v-model="(form.config as StatConfig).apiEndpoint" placeholder="选择数据源">
            <el-option v-for="api in presetApis" :key="api.key" :label="api.name" :value="api.key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'api' && (form.config as StatConfig).apiEndpoint" label="字段">
          <el-select v-model="(form.config as StatConfig).apiField" placeholder="选择字段">
            <el-option v-for="f in selectedApiFields" :key="f.key" :label="f.label" :value="f.key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="(form.config as StatConfig).dataSource === 'static'" label="静态数值">
          <el-input-number v-model="(form.config as StatConfig).staticValue" :min="0" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select v-model="(form.config as StatConfig).icon" placeholder="选择图标">
            <el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon" />
          </el-select>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="(form.config as StatConfig).color" />
        </el-form-item>
      </template>

      <!-- 便签配置 -->
      <template v-if="widgetType === 'note'">
        <el-form-item label="内容（支持 Markdown）">
          <el-input v-model="(form.config as NoteConfig).content" type="textarea" :rows="8" placeholder="输入 Markdown 内容..." />
        </el-form-item>
      </template>

      <!-- 链接配置 -->
      <template v-if="widgetType === 'link'">
        <div class="link-header">
          <span class="step-desc">链接列表</span>
          <el-button size="small" @click="addLinkRow">+ 添加链接</el-button>
        </div>
        <div v-for="(link, idx) in (form.config as LinkConfig).links" :key="idx" class="link-row">
          <el-input v-model="link.name" placeholder="名称" style="width:25%" />
          <el-input v-model="link.path" placeholder="路由路径" style="width:30%" />
          <el-select v-model="link.icon" placeholder="图标" style="width:20%">
            <el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon" />
          </el-select>
          <el-color-picker v-model="link.color" size="small" />
          <el-button type="danger" :icon="Delete" circle size="small" @click="removeLinkRow(idx)" />
        </div>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { presetApis, getPresetApi } from '../widgets/presetApis'
import type { CustomWidgetType, ChartConfig, StatConfig, NoteConfig, LinkConfig } from '@/types/widget'

const props = defineProps<{
  widgetType: CustomWidgetType
  form: { name: string; span: number; config: ChartConfig | StatConfig | NoteConfig | LinkConfig }
}>()

const iconOptions = ['User', 'UserFilled', 'Monitor', 'DataAnalysis', 'TrendCharts', 'PieChart', 'Bell', 'Star', 'Setting', 'Link', 'Document', 'Folder', 'CircleCheck', 'Menu']

const selectedApiFields = computed(() => {
  const c = props.form.config as StatConfig
  if (c.apiEndpoint) {
    return getPresetApi(c.apiEndpoint)?.fields.filter(f => f.type === 'number') || []
  }
  return []
})

const staticLabelsStr = computed({
  get: () => (props.form.config as ChartConfig).staticData?.labels.join(',') || '',
  set: (v: string) => {
    const c = props.form.config as ChartConfig
    if (!c.staticData) c.staticData = { labels: [], values: [] }
    c.staticData.labels = v.split(',').map(s => s.trim()).filter(Boolean)
  }
})

const staticValuesStr = computed({
  get: () => (props.form.config as ChartConfig).staticData?.values.join(',') || '',
  set: (v: string) => {
    const c = props.form.config as ChartConfig
    if (!c.staticData) c.staticData = { labels: [], values: [] }
    c.staticData.values = v.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  }
})

function addLinkRow() {
  const c = props.form.config as LinkConfig
  c.links.push({ name: '', path: '', icon: 'Link', color: '#6366F1' })
}

function removeLinkRow(idx: number) {
  const c = props.form.config as LinkConfig
  c.links.splice(idx, 1)
}
</script>

<style scoped lang="scss">
.step-desc { font-size: $font-size-sm; color: $text-secondary; }
.link-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.link-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
</style>
