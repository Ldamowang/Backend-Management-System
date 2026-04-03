<template>
  <div class="chart-container">
    <v-chart :option="chartOption" autoresize />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { getLoginLogs, type LoginLog } from '@/api/modules/log'

const { t } = useI18n()
use([CanvasRenderer, LineChart, TooltipComponent, GridComponent])

const loginDays = ref<string[]>([])
const loginCounts = ref<number[]>([])

function getRecentDays(days: number): string[] {
  const result: string[] = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    result.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return result
}

function countLoginsByDay(logs: LoginLog[], days: number): number[] {
  const counts = new Array(days).fill(0)
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  for (const log of logs) {
    const logDate = new Date(log.loginTime)
    const diffDays = Math.floor((today.getTime() - logDate.getTime()) / (1000 * 60 * 60 * 24))
    if (diffDays >= 0 && diffDays < days) counts[days - 1 - diffDays]++
  }
  return counts
}

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(15, 23, 42, 0.9)',
    borderColor: 'rgba(99, 102, 241, 0.2)',
    textStyle: { color: '#E2E8F0', fontFamily: 'DM Sans' },
    borderRadius: 8
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: loginDays.value,
    axisLine: { lineStyle: { color: '#E2E8F0' } },
    axisLabel: { color: '#64748B', fontFamily: 'DM Sans', fontSize: 12 },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
    axisLabel: { color: '#94A3B8', fontFamily: 'DM Sans', fontSize: 12 }
  },
  series: [{
    name: t('dashboard.loginTimes'),
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 3, color: '#6366F1' },
    itemStyle: { color: '#6366F1', borderWidth: 2, borderColor: '#fff' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(99, 102, 241, 0.2)' },
          { offset: 1, color: 'rgba(99, 102, 241, 0.01)' }
        ]
      }
    },
    animationDuration: 1000,
    animationEasing: 'cubicOut',
    data: loginCounts.value
  }]
}))

onMounted(async () => {
  loginDays.value = getRecentDays(7)
  try {
    const res = await getLoginLogs({ page: 1, size: 50 })
    loginCounts.value = countLoginsByDay(res.data.list, 7)
  } catch { /* ignore */ }
})
</script>

<style scoped>
.chart-container { height: 300px; }
</style>
