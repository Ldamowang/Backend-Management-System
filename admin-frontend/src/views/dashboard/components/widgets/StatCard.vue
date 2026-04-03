<template>
  <div class="stat-item">
    <div class="stat-icon" :style="{ backgroundColor: config.bgColor, color: config.color }">
      <el-icon :size="28"><component :is="config.icon" /></el-icon>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ displayValue }}</div>
      <div class="stat-label">{{ config.label }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getStats, type DashboardStats } from '@/api/modules/dashboard'

const props = defineProps<{ widgetId: string }>()
const { t } = useI18n()
const statsData = ref<DashboardStats>({ userCount: 0, roleCount: 0, menuCount: 0, todayLoginCount: 0, totalLoginCount: 0 })

interface StatConfig {
  key: keyof DashboardStats | null
  label: string
  icon: string
  color: string
  bgColor: string
}

const configMap: Record<string, StatConfig> = {
  'stat-users': { key: 'userCount', label: 'dashboard.userCount', icon: 'User', color: '#409EFF', bgColor: 'rgba(64,158,255,0.1)' },
  'stat-roles': { key: 'roleCount', label: 'dashboard.roleCount', icon: 'UserFilled', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' },
  'stat-today-login': { key: 'todayLoginCount', label: 'dashboard.loginCount', icon: 'Monitor', color: '#E6A23C', bgColor: 'rgba(230,162,60,0.1)' },
  'stat-system': { key: null, label: 'dashboard.systemStatus', icon: 'CircleCheck', color: '#67C23A', bgColor: 'rgba(103,194,58,0.1)' }
}

const config = computed(() => {
  const c = configMap[props.widgetId] || configMap['stat-users']
  return { ...c, label: t(c.label) }
})

const displayValue = computed(() => {
  const c = configMap[props.widgetId]
  if (!c || c.key === null) return t('dashboard.running')
  return statsData.value[c.key].toLocaleString()
})

onMounted(async () => {
  try {
    const res = await getStats()
    statsData.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped lang="scss">
.stat-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}
.stat-value {
  font-size: 26px;
  font-weight: bold;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
