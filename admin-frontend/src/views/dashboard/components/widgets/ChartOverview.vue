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
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [t('dashboard.overview.userCount'), t('dashboard.overview.roleCount'), t('dashboard.overview.menuCount'), t('dashboard.overview.totalLogin')]
  },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar',
    barWidth: '40%',
    itemStyle: {
      borderRadius: [4, 4, 0, 0],
      color: (params: { dataIndex: number }) => {
        const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C']
        return colors[params.dataIndex]
      }
    },
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
