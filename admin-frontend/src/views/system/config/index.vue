<template>
  <div>
    <el-card v-loading="loading">
      <template #header>
        <div class="flex-between">
          <span>{{ $t('common.settings.title') }}</span>
          <el-button v-permission="'sys:config:edit'" type="primary" :loading="saving" @click="handleSave">
            <el-icon><DocumentAdd /></el-icon>{{ $t('system.config.saveSettings') }}
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>{{ $t('system.config.basicSettings') }}</span></template>
            <el-form label-width="100px">
              <el-form-item :label="$t('system.config.systemName')">
                <el-input v-model="configMap['sys.name']" />
              </el-form-item>
              <el-form-item :label="$t('system.config.systemDomain')">
                <el-input v-model="configMap['sys.domain']" />
              </el-form-item>
              <el-form-item :label="$t('system.config.contactEmail')">
                <el-input v-model="configMap['sys.email']" />
              </el-form-item>
              <el-form-item :label="$t('system.config.contactPhone')">
                <el-input v-model="configMap['sys.phone']" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>{{ $t('system.config.securitySettings') }}</span></template>
            <el-form label-width="100px">
              <el-form-item :label="$t('system.config.passwordStrength')">
                <el-radio-group v-model="configMap['sys.password.strength']">
                  <el-radio value="1">{{ $t('system.config.passwordLow') }}</el-radio>
                  <el-radio value="2">{{ $t('system.config.passwordMedium') }}</el-radio>
                  <el-radio value="3">{{ $t('system.config.passwordHigh') }}</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item :label="$t('system.config.loginLimit')">
                <el-switch v-model="configMap['sys.login.limit']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item :label="$t('system.config.loginTimeout')">
                <el-input v-model="configMap['sys.login.timeout']" class="config-input-inline" />
                <span class="config-input-suffix">{{ $t('system.config.loginTimeoutUnit') }}</span>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12" class="config-section-gap">
          <el-card shadow="never">
            <template #header><span>{{ $t('system.config.notificationSettings') }}</span></template>
            <el-form label-width="100px">
              <el-form-item :label="$t('system.config.emailNotification')">
                <el-switch v-model="configMap['sys.notify.email']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item :label="$t('system.config.smsNotification')">
                <el-switch v-model="configMap['sys.notify.sms']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item :label="$t('system.config.systemNotification')">
                <el-switch v-model="configMap['sys.notify.system']" active-value="true" inactive-value="false" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12" class="config-section-gap">
          <el-card shadow="never">
            <template #header><span>{{ $t('system.config.advancedSettings') }}</span></template>
            <el-form label-width="100px">
              <el-form-item :label="$t('system.config.logLevel')">
                <el-select v-model="configMap['sys.log.level']" class="table-full">
                  <el-option :label="$t('system.config.logDebug')" value="debug" />
                  <el-option :label="$t('system.config.logInfo')" value="info" />
                  <el-option :label="$t('system.config.logWarn')" value="warn" />
                  <el-option :label="$t('system.config.logError')" value="error" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('system.config.dataBackup')">
                <el-switch v-model="configMap['sys.backup']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item :label="$t('system.config.performanceMonitor')">
                <el-switch v-model="configMap['sys.performance']" active-value="true" inactive-value="false" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getConfigs, updateConfigs, type SysConfig } from '@/api/modules/config'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)

/** 原始配置列表（用于回写时携带 id 等信息） */
const configList = ref<SysConfig[]>([])

/** key-value 映射，方便表单双向绑定 */
const configMap = reactive<Record<string, string>>({
  'sys.name': '后台管理系统',
  'sys.domain': '',
  'sys.email': '',
  'sys.phone': '',
  'sys.password.strength': '2',
  'sys.login.limit': 'true',
  'sys.login.timeout': '30',
  'sys.notify.email': 'true',
  'sys.notify.sms': 'false',
  'sys.notify.system': 'true',
  'sys.log.level': 'info',
  'sys.backup': 'true',
  'sys.performance': 'true'
})

async function fetchConfigs() {
  loading.value = true
  try {
    const { data } = await getConfigs()
    configList.value = data
    for (const item of data) {
      if (item.configKey in configMap) {
        configMap[item.configKey] = item.configValue
      }
    }
  } catch {
    ElMessage.error(t('common.message.configLoadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    // 将当前表单值回写到配置列表
    const updatedList = configList.value.map((item) => ({
      ...item,
      configValue: item.configKey in configMap ? configMap[item.configKey] : item.configValue
    }))
    await updateConfigs(updatedList)
    ElMessage.success(t('common.message.configSaveSuccess'))
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>
