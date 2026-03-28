<template>
  <div>
    <el-card v-loading="loading">
      <template #header>
        <div class="flex-between">
          <span>系统设置</span>
          <el-button v-permission="'sys:config:edit'" type="primary" :loading="saving" @click="handleSave">
            <el-icon><DocumentAdd /></el-icon>保存设置
          </el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>基本设置</span></template>
            <el-form label-width="100px">
              <el-form-item label="系统名称">
                <el-input v-model="configMap['sys.name']" />
              </el-form-item>
              <el-form-item label="系统域名">
                <el-input v-model="configMap['sys.domain']" />
              </el-form-item>
              <el-form-item label="联系邮箱">
                <el-input v-model="configMap['sys.email']" />
              </el-form-item>
              <el-form-item label="联系电话">
                <el-input v-model="configMap['sys.phone']" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span>安全设置</span></template>
            <el-form label-width="100px">
              <el-form-item label="密码强度">
                <el-radio-group v-model="configMap['sys.password.strength']">
                  <el-radio value="1">低</el-radio>
                  <el-radio value="2">中</el-radio>
                  <el-radio value="3">高</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="登录限制">
                <el-switch v-model="configMap['sys.login.limit']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item label="登录超时">
                <el-input v-model="configMap['sys.login.timeout']" class="config-input-inline" />
                <span class="config-input-suffix">分钟</span>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12" class="config-section-gap">
          <el-card shadow="never">
            <template #header><span>通知设置</span></template>
            <el-form label-width="100px">
              <el-form-item label="邮件通知">
                <el-switch v-model="configMap['sys.notify.email']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item label="短信通知">
                <el-switch v-model="configMap['sys.notify.sms']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item label="系统通知">
                <el-switch v-model="configMap['sys.notify.system']" active-value="true" inactive-value="false" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="12" class="config-section-gap">
          <el-card shadow="never">
            <template #header><span>高级设置</span></template>
            <el-form label-width="100px">
              <el-form-item label="日志级别">
                <el-select v-model="configMap['sys.log.level']" class="table-full">
                  <el-option label="调试" value="debug" />
                  <el-option label="信息" value="info" />
                  <el-option label="警告" value="warn" />
                  <el-option label="错误" value="error" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据备份">
                <el-switch v-model="configMap['sys.backup']" active-value="true" inactive-value="false" />
              </el-form-item>
              <el-form-item label="性能监控">
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
import { getConfigs, updateConfigs, type SysConfig } from '@/api/modules/config'

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
    ElMessage.error('加载配置失败')
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
    ElMessage.success('设置保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(fetchConfigs)
</script>
