<template>
  <div class="stat-item">
    <div class="stat-icon-wrapper">
      <div class="stat-icon" :style="iconStyle">
        <el-icon :size="24"><component :is="config.icon" /></el-icon>
      </div>
    </div>
    <div class="stat-content">
      <div class="stat-value">{{ displayValue }}</div>
      <div class="stat-label">{{ config.label }}</div>
    </div>
    <div class="stat-decoration" :style="decorationStyle"></div>
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
  gradient: string
}

const configMap: Record<string, StatConfig> = {
  'stat-users': {
    key: 'userCount', label: 'dashboard.userCount', icon: 'User',
    color: '#6366F1', bgColor: 'rgba(99, 102, 241, 0.1)',
    gradient: 'linear-gradient(135deg, rgba(99, 102, 241, 0.08), rgba(139, 92, 246, 0.04))'
  },
  'stat-roles': {
    key: 'roleCount', label: 'dashboard.roleCount', icon: 'UserFilled',
    color: '#10B981', bgColor: 'rgba(16, 185, 129, 0.1)',
    gradient: 'linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(52, 211, 153, 0.04))'
  },
  'stat-today-login': {
    key: 'todayLoginCount', label: 'dashboard.loginCount', icon: 'Monitor',
    color: '#F59E0B', bgColor: 'rgba(245, 158, 11, 0.1)',
    gradient: 'linear-gradient(135deg, rgba(245, 158, 11, 0.08), rgba(251, 191, 36, 0.04))'
  },
  'stat-system': {
    key: null, label: 'dashboard.systemStatus', icon: 'CircleCheck',
    color: '#10B981', bgColor: 'rgba(16, 185, 129, 0.1)',
    gradient: 'linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(52, 211, 153, 0.04))'
  }
}

const config = computed(() => {
  const c = configMap[props.widgetId] || configMap['stat-users']
  return { ...c, label: t(c.label) }
})

const iconStyle = computed(() => ({
  backgroundColor: config.value.bgColor,
  color: config.value.color
}))

const decorationStyle = computed(() => ({
  background: config.value.gradient
}))

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
  padding: 4px 0;
  position: relative;
  overflow: hidden;
}

.stat-icon-wrapper {
  flex-shrink: 0;
  margin-right: 18px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform $transition-base;

  .stat-item:hover & {
    transform: scale(1.08);
  }
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-family: $font-family-heading;
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
  letter-spacing: -0.02em;
  animation: countUp 0.5s ease both;
}

.stat-label {
  font-size: $font-size-sm;
  color: $text-secondary;
  margin-top: 4px;
  font-weight: 500;
}

.stat-decoration {
  position: absolute;
  right: -20px;
  top: -20px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0.5;
  pointer-events: none;
}
</style>
