<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" :style="{ backgroundColor: stat.bgColor, color: stat.color }">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="card-gap">
      <el-col :span="12">
        <el-card>
          <template #header><span>{{ $t('dashboard.loginTrend') }}</span></template>
          <div class="chart-container">
            <v-chart :option="loginTrendOption" autoresize />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>{{ $t('dashboard.systemOverview') }}</span></template>
          <div class="chart-container">
            <v-chart :option="overviewOption" autoresize />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="card-gap">
      <template #header><span>{{ $t('dashboard.recentLogin') }}</span></template>
      <el-table :data="recentLogs" class="table-full" size="small">
        <el-table-column prop="username" :label="$t('dashboard.column.username')" width="120" />
        <el-table-column prop="ip" :label="$t('dashboard.column.ip')" width="140" />
        <el-table-column prop="location" :label="$t('dashboard.column.location')" min-width="140" />
        <el-table-column prop="browser" :label="$t('dashboard.column.browser')" min-width="120" />
        <el-table-column :label="$t('dashboard.column.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? $t('dashboard.column.success') : $t('dashboard.column.failed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" :label="$t('dashboard.column.loginTime')" min-width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'
import { getStats, type DashboardStats } from '@/api/modules/dashboard'
import { getLoginLogs, type LoginLog } from '@/api/modules/log'

const { t } = useI18n()

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const statsData = ref<DashboardStats>({ userCount: 0, roleCount: 0, menuCount: 0, todayLoginCount: 0, totalLoginCount: 0 })
const recentLogs = ref<LoginLog[]>([])

const stats = computed(() => [
  { label: t('dashboard.userCount'), value: statsData.value.userCount.toLocaleString(), icon: 'User', color: '#409EFF', bgColor: 'rgba(64,158,255,0.1)' },
  { label: t('dashboard.roleCount'), value: statsData.value.roleCount.toLocaleString(), icon: 'UserFilled', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' },
  { label: t('dashboard.loginCount'), value: statsData.value.totalLoginCount.toLocaleString(), icon: 'Monitor', color: '#E6A23C', bgColor: 'rgba(230,162,60,0.1)' },
  { label: t('dashboard.systemStatus'), value: t('dashboard.running'), icon: 'CircleCheck', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' }
])

// 近7日登录趋势图
const loginDays = ref<string[]>([])
const loginCounts = ref<number[]>([])

const loginTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: loginDays.value
  },
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

// 系统概览柱状图
const overviewOption = computed(() => ({
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

/** 生成近7日日期标签 */
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

/** 根据登录日志统计每日登录次数 */
function countLoginsByDay(logs: LoginLog[], days: number): number[] {
  const counts: number[] = new Array(days).fill(0)
  const today = new Date()
  today.setHours(23, 59, 59, 999)

  for (const log of logs) {
    const logDate = new Date(log.loginTime)
    const diffDays = Math.floor((today.getTime() - logDate.getTime()) / (1000 * 60 * 60 * 24))
    if (diffDays >= 0 && diffDays < days) {
      counts[days - 1 - diffDays]++
    }
  }
  return counts
}

onMounted(async () => {
  loginDays.value = getRecentDays(7)

  // 并行请求统计数据和登录日志
  const [statsRes, logsRes] = await Promise.all([
    getStats().catch(() => ({ data: { userCount: 0, roleCount: 0, menuCount: 0, todayLoginCount: 0, totalLoginCount: 0 } })),
    getLoginLogs({ page: 1, size: 50 }).catch(() => ({ data: { list: [], total: 0, page: 1, size: 50 } }))
  ])

  statsData.value = statsRes.data
  const allLogs = logsRes.data.list
  recentLogs.value = allLogs.slice(0, 10)
  loginCounts.value = countLoginsByDay(allLogs, 7)
})
</script>

<style scoped lang="scss">
.stat-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: $border-radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-lg;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: $text-primary;
}

.stat-label {
  font-size: $font-size-base;
  color: $text-secondary;
  margin-top: $spacing-xs;
}
</style>
