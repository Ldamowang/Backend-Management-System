<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <!-- JVM 信息 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="flex-between">
              <span>{{ $t('system.monitor.jvmInfo') }}</span>
              <el-button type="primary" size="small" link @click="fetchData">
                <el-icon><Refresh /></el-icon>{{ $t('common.action.refresh') }}
              </el-button>
            </div>
          </template>
          <el-descriptions :column="1" border v-if="serverInfo">
            <el-descriptions-item :label="$t('system.monitor.javaVersion')">{{ serverInfo.jvm.javaVersion }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.jvmName')">{{ serverInfo.jvm.jvmName }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.heapUsed')">{{ serverInfo.jvm.heapUsed }} MB</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.heapMax')">{{ serverInfo.jvm.heapMax }} MB</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.nonHeapUsed')">{{ serverInfo.jvm.nonHeapUsed }} MB</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.uptime')">{{ formatUptime(serverInfo.jvm.uptime) }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="serverInfo" style="margin-top: 12px">
            <span style="font-size: 13px; color: #606266">{{ $t('system.monitor.heapUsage') }}</span>
            <el-progress
              :percentage="Math.round((serverInfo.jvm.heapUsed / serverInfo.jvm.heapMax) * 100)"
              :color="progressColor"
              style="margin-top: 6px"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 操作系统信息 -->
      <el-col :span="12">
        <el-card>
          <template #header><span>{{ $t('system.monitor.osInfo') }}</span></template>
          <el-descriptions :column="1" border v-if="serverInfo">
            <el-descriptions-item :label="$t('system.monitor.osName')">{{ serverInfo.os.name }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.osArch')">{{ serverInfo.os.arch }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.osVersion')">{{ serverInfo.os.version }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.cpuCores')">{{ serverInfo.os.availableProcessors }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.systemLoad')">{{ serverInfo.os.systemLoadAverage >= 0 ? serverInfo.os.systemLoadAverage.toFixed(2) : 'N/A' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 磁盘信息 -->
      <el-col :span="12">
        <el-card>
          <template #header><span>{{ $t('system.monitor.diskInfo') }}</span></template>
          <el-descriptions :column="1" border v-if="serverInfo">
            <el-descriptions-item :label="$t('system.monitor.diskTotal')">{{ serverInfo.disk.total }} GB</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.diskUsed')">{{ serverInfo.disk.used }} GB</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.diskAvailable')">{{ serverInfo.disk.usable }} GB</el-descriptions-item>
          </el-descriptions>
          <div v-if="serverInfo" style="margin-top: 12px">
            <span style="font-size: 13px; color: #606266">{{ $t('system.monitor.diskUsage') }}</span>
            <el-progress
              :percentage="serverInfo.disk.total > 0 ? Math.round((serverInfo.disk.used / serverInfo.disk.total) * 100) : 0"
              :color="progressColor"
              style="margin-top: 6px"
            />
          </div>
        </el-card>
      </el-col>

      <!-- Redis 信息 -->
      <el-col :span="12">
        <el-card>
          <template #header><span>{{ $t('system.monitor.redisInfo') }}</span></template>
          <el-descriptions :column="1" border v-if="serverInfo && !serverInfo.redis.error">
            <el-descriptions-item :label="$t('system.monitor.redisVersion')">{{ serverInfo.redis.version }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.redisMemory')">{{ serverInfo.redis.usedMemory }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.redisConnections')">{{ serverInfo.redis.connectedClients }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.redisUptime')">{{ serverInfo.redis.uptimeInDays }} {{ $t('system.monitor.days') }}</el-descriptions-item>
            <el-descriptions-item :label="$t('system.monitor.redisKeys')">{{ serverInfo.redis.dbSize }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="serverInfo?.redis?.error" :title="serverInfo.redis.error" type="error" :closable="false" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getServerInfo, type ServerInfo } from '@/api/modules/server'

const { t } = useI18n()
const loading = ref(false)
const serverInfo = ref<ServerInfo | null>(null)

const progressColor = [
  { color: '#67c23a', percentage: 50 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getServerInfo()
    serverInfo.value = data
  } finally { loading.value = false }
}

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const mins = Math.floor((seconds % 3600) / 60)
  const parts: string[] = []
  if (days > 0) parts.push(`${days}${t('system.monitor.days')}`)
  if (hours > 0) parts.push(`${hours}${t('system.monitor.hours')}`)
  parts.push(`${mins}${t('system.monitor.minutes')}`)
  return parts.join(' ')
}

onMounted(fetchData)
</script>
