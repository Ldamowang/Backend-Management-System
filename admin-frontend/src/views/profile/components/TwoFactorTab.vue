<template>
  <div class="two-factor-tab">
    <div v-loading="statusLoading" class="two-factor-status">
      <div class="status-header">
        <h4>{{ $t('profile.twoFactor') }}</h4>
        <el-tag :type="enabled ? 'success' : 'info'" size="small">
          {{ enabled ? $t('profile.twoFactorEnabled') : $t('profile.twoFactorDisabled') }}
        </el-tag>
      </div>
      <p class="status-desc">{{ $t('profile.twoFactorDesc') }}</p>
      <div class="status-actions">
        <el-button v-if="!enabled" type="primary" @click="setupDialogVisible = true">
          {{ $t('profile.enable2FA') }}
        </el-button>
        <el-button v-else type="danger" plain @click="handleDisable">
          {{ $t('profile.disable2FA') }}
        </el-button>
      </div>
    </div>

    <TwoFactorSetupDialog
      v-model:visible="setupDialogVisible"
      @success="handleSetupSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get2FAStatus, disable2FA } from '@/api/modules/twoFactor'
import TwoFactorSetupDialog from './TwoFactorSetupDialog.vue'

const { t } = useI18n()
const enabled = ref(false)
const statusLoading = ref(false)
const setupDialogVisible = ref(false)

async function fetchStatus() {
  statusLoading.value = true
  try {
    const { data } = await get2FAStatus()
    enabled.value = data.enabled
  } finally {
    statusLoading.value = false
  }
}

async function handleDisable() {
  try {
    await ElMessageBox.confirm(
      t('profile.disable2FAConfirm'),
      t('profile.disable2FA'),
      { type: 'warning' }
    )
    await disable2FA()
    enabled.value = false
    ElMessage.success(t('profile.disableSuccess'))
  } catch {
    // 用户取消
  }
}

function handleSetupSuccess() {
  enabled.value = true
  setupDialogVisible.value = false
}

onMounted(fetchStatus)
</script>

<style scoped>
.two-factor-status {
  max-width: 500px;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.status-header h4 {
  margin: 0;
  font-size: 16px;
}

.status-desc {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-bottom: 20px;
}

.status-actions {
  margin-top: 16px;
}
</style>
