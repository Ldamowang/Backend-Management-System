<template>
  <el-table :data="recentLogs" size="small">
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLoginLogs, type LoginLog } from '@/api/modules/log'

const recentLogs = ref<LoginLog[]>([])

onMounted(async () => {
  try {
    const res = await getLoginLogs({ page: 1, size: 10 })
    recentLogs.value = res.data.list
  } catch { /* ignore */ }
})
</script>
