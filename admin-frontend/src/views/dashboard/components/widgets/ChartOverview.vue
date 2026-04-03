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
import { BarChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'
import { getStats, type DashboardStats } from '@/api/modules/dashboard'

const { t } = useI18n()
use([CanvasRenderer, BarChart, TooltipComponent, GridComponent])

const statsData = ref<DashboardStats>({ userCount: 0, roleCount: 0, menuCount: 0, todayLoginCount: 0, totalLoginCount: 0 })

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
    data: [
      t('dashboard.overview.userCount'),
      t('dashboard.overview.roleCount'),
      t('dashboard.overview.menuCount'),
      t('dashboard.overview.totalLogin')
    ],
    axisLine: { lineStyle: { color: '#E2E8F0' } },
    axisLabel: { color: '#64748B', fontFamily: 'DM Sans', fontSize: 12 },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
    axisLabel: { color: '#94A3B8', fontFamily: 'DM Sans', fontSize: 12 }
  },
  series: [{
    type: 'bar',
    barWidth: '40%',
    itemStyle: {
      borderRadius: [6, 6, 0, 0],
      color: (params: { dataIndex: number }) => {
        const gradients = [
          { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#818CF8' }, { offset: 1, color: '#6366F1' }
          ]},
          { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#34D399' }, { offset: 1, color: '#10B981' }
          ]},
          { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#FBBF24' }, { offset: 1, color: '#F59E0B' }
          ]},
          { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#F87171' }, { offset: 1, color: '#EF4444' }
          ]}
        ]
        return gradients[params.dataIndex]
      }
    },
    animationDuration: 800,
    animationEasing: 'cubicOut',
    data: [
      statsData.value.userCount,
      statsData.value.roleCount,
      statsData.value.menuCount,
      statsData.value.totalLoginCount
    ]
  }]
}))

onMounted(async () => {
  try {
    const res = await getStats()
    statsData.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.chart-container { height: 300px; }
</style>
