<template>
  <el-dialog
    :model-value="visible"
    :title="$t('profile.enable2FA')"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-steps :active="step" align-center style="margin-bottom: 24px">
      <el-step :title="$t('profile.setupStep1')" />
      <el-step :title="$t('profile.setupStep2')" />
      <el-step :title="$t('profile.setupStep3')" />
    </el-steps>

    <!-- Step 1: 扫描二维码 -->
    <div v-if="step === 0" v-loading="setupLoading" class="setup-step">
      <p>{{ $t('profile.scanQRCode') }}</p>
      <div class="qr-container">
        <canvas ref="qrCanvas"></canvas>
      </div>
      <p class="manual-entry">
        {{ $t('profile.manualEntry') }}
        <code class="secret-key">{{ setupData?.secretKey }}</code>
      </p>
    </div>

    <!-- Step 2: 保存备用码 -->
    <div v-if="step === 1" class="setup-step">
      <el-alert
        type="warning"
        :closable="false"
        :title="$t('profile.backupCodesWarning')"
        style="margin-bottom: 16px"
      />
      <div class="backup-codes">
        <code v-for="code in setupData?.backupCodes" :key="code" class="backup-code">
          {{ code }}
        </code>
      </div>
      <el-button size="small" @click="copyBackupCodes">
        {{ $t('profile.copyAll') }}
      </el-button>
    </div>

    <!-- Step 3: 验证绑定 -->
    <div v-if="step === 2" class="setup-step">
      <p>{{ $t('profile.enterCode') }}</p>
      <el-input
        v-model="verifyCode"
        placeholder="000000"
        maxlength="6"
        size="large"
        style="width: 200px"
        @keyup.enter="handleVerify"
      />
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">{{ $t('profile.prevStep') }}</el-button>
      <el-button v-if="step < 2" type="primary" @click="step++">
        {{ $t('profile.nextStep') }}
      </el-button>
      <el-button
        v-if="step === 2"
        type="primary"
        :loading="verifying"
        :disabled="!verifyCode"
        @click="handleVerify"
      >
        {{ $t('profile.verifyAndEnable') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { setup2FA, verify2FA } from '@/api/modules/twoFactor'
import type { TotpSetupResponse } from '@/types/twoFactor'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const { t } = useI18n()
const step = ref(0)
const setupLoading = ref(false)
const setupData = ref<TotpSetupResponse | null>(null)
const verifyCode = ref('')
const verifying = ref(false)
const qrCanvas = ref<HTMLCanvasElement | null>(null)

watch(() => props.visible, async (val) => {
  if (val) {
    step.value = 0
    verifyCode.value = ''
    setupData.value = null
    await fetchSetup()
  }
})

async function fetchSetup() {
  setupLoading.value = true
  try {
    const { data } = await setup2FA()
    setupData.value = data
    await nextTick()
    if (qrCanvas.value && data.qrCodeUri) {
      await QRCode.toCanvas(qrCanvas.value, data.qrCodeUri, { width: 200, margin: 2 })
    }
  } catch {
    ElMessage.error(t('profile.fetchSetupFailed'))
  } finally {
    setupLoading.value = false
  }
}

async function handleVerify() {
  if (!verifyCode.value) return
  verifying.value = true
  try {
    await verify2FA(verifyCode.value)
    ElMessage.success(t('profile.setupSuccess'))
    emit('success')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : t('profile.verifyFailed')
    ElMessage.error(message)
    verifyCode.value = ''
  } finally {
    verifying.value = false
  }
}

async function copyBackupCodes() {
  if (!setupData.value) return
  const text = setupData.value.backupCodes.join('\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('profile.copied'))
  } catch {
    ElMessage.error(t('profile.copyFailed'))
  }
}
</script>

<style scoped>
.setup-step {
  text-align: center;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.qr-container {
  margin: 16px 0;
}

.manual-entry {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 12px;
}

.secret-key {
  display: inline-block;
  padding: 2px 8px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
  word-break: break-all;
}

.backup-codes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  max-width: 320px;
  margin: 0 auto 16px;
}

.backup-code {
  padding: 6px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
}
</style>
