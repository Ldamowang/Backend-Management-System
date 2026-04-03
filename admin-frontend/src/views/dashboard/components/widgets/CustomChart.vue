<template>
  <div class="chart-container">
    <v-chart v-if="chartOption" :option="chartOption" autoresize />
    <div v-else class="empty-chart">暂无数据</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { useDashboardStore } from '@/stores/modules/dashboard'
import { getPresetApi } from './presetApis'
import type { ChartConfig } from '@/types/widget'

use([CanvasRenderer, LineChart, BarChart, PieChart, TooltipComponent, GridComponent, LegendComponent])

const props = defineProps<{ widgetId: string }>()
const store = useDashboardStore()
const apiData = ref<Record<string, unknown>>({})

const config = computed<ChartConfig | null>(() => {
  const w = store.getCustomWidget(props.widgetId)
  return w ? w.config as ChartConfig : null
})

const chartOption = computed(() => {
  const c = config.value
  if (!c) return null

  let labels: string[] = []
  let values: number[] = []

  if (c.dataSource === 'static' && c.staticData) {
    labels = c.staticData.labels
    values = c.staticData.values
  } else if (c.dataSource === 'api' && c.apiEndpoint) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      labels = api.fields.filter(f => f.type === 'number').map(f => f.label)
      values = api.fields.filter(f => f.type === 'number').map(f => Number(apiData.value[f.key] ?? 0))
    }
  }

  if (labels.length === 0) return null

  if (c.chartType === 'pie') {
    return {
      tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(99,102,241,0.2)', textStyle: { color: '#E2E8F0' }, borderRadius: 8 },
      legend: { bottom: 0, textStyle: { color: '#64748B', fontSize: 12 } },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: labels.map((l, i) => ({ name: l, value: values[i] }))
      }]
    }
  }

  const seriesType = c.chartType === 'area' ? 'line' : c.chartType
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(99,102,241,0.2)', textStyle: { color: '#E2E8F0' }, borderRadius: 8 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#64748B', fontSize: 12 }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } }, axisLabel: { color: '#94A3B8', fontSize: 12 } },
    series: [{
      type: seriesType, data: values, smooth: true, barWidth: '40%',
      ...(c.chartType === 'area' ? { areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(99,102,241,0.2)' }, { offset: 1, color: 'rgba(99,102,241,0.01)' }] } } } : {}),
      itemStyle: { borderRadius: c.chartType === 'bar' ? [6, 6, 0, 0] : 0, color: '#6366F1' }
    }]
  }
})

onMounted(async () => {
  const c = config.value
  if (c?.dataSource === 'api' && c.apiEndpoint) {
    const api = getPresetApi(c.apiEndpoint)
    if (api) {
      try { apiData.value = await api.fetcher() } catch { /* ignore */ }
    }
  }
})
</script>

<style scoped>
.chart-container { height: 300px; }
.empty-chart { height: 300px; display: flex; align-items: center; justify-content: center; color: #94A3B8; font-size: 14px; }
</style>
