<template>
  <div>
    <el-card>
      <template #header>
        <div class="flex-between">
          <span>{{ $t('system.online.title') }}</span>
          <el-button type="primary" @click="fetchData">
            <el-icon><Refresh /></el-icon>{{ $t('common.action.refresh') }}
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <template #empty>
          <el-empty :description="$t('system.online.noData')" :image-size="120" />
        </template>
        <el-table-column prop="userId" :label="$t('system.online.userId')" width="100" />
        <el-table-column prop="username" :label="$t('system.online.username')" min-width="120" />
        <el-table-column prop="nickname" :label="$t('system.online.nickname')" min-width="120" />
        <el-table-column prop="loginTime" :label="$t('system.online.loginTime')" min-width="170" />
        <el-table-column prop="tokenId" :label="$t('system.online.sessionId')" min-width="200" />
        <el-table-column :label="$t('common.label.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'sys:online:kick'" type="danger" size="small" link @click="handleKick(row)">{{ $t('system.online.forceLogout') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOnlineUsers, kickOutUser, type OnlineUser } from '@/api/modules/online'

const { t } = useI18n()

const loading = ref(false)
const tableData = ref<OnlineUser[]>([])

async function fetchData() {
  loading.value = true
  try {
    const { data } = await getOnlineUsers()
    tableData.value = data
  } finally {
    loading.value = false
  }
}

async function handleKick(row: OnlineUser) {
  await ElMessageBox.confirm(t('system.online.confirmKick', { name: row.username }), t('common.message.warning'), { type: 'warning' })
  await kickOutUser(row.token)
  ElMessage.success(t('common.message.kickSuccess'))
  fetchData()
}

onMounted(fetchData)
</script>
