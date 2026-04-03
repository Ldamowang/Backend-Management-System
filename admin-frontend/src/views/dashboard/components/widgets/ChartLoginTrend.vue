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
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: loginDays.value },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: t('dashboard.loginTimes'),
    type: 'line',
    smooth: true,
    areaStyle: { opacity: 0.15 },
    itemStyle: { color: '#409EFF' },
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
