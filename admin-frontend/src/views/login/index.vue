<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">{{ $t('login.title') }}</h2>
      <el-form ref="formRef" :model="loginForm" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" :placeholder="$t('login.usernamePlaceholder')" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            :placeholder="$t('login.passwordPlaceholder')"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.remember">{{ $t('login.remember') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="table-full" @click="handleLogin">
            {{ $t('login.login') }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 密码过期强制修改弹窗 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="密码已过期，请修改密码"
      width="420px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
          <PasswordStrength :password="pwdForm.newPassword" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/modules/user'
import { updatePassword } from '@/api/modules/profile'
import PasswordStrength from '@/components/PasswordStrength.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }]
}

// 密码过期修改相关
const passwordDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()

const pwdForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
    loading.value = true
    const { passwordExpired } = await userStore.login(loginForm)

    if (passwordExpired) {
      passwordDialogVisible.value = true
      return
    }

    ElMessage.success(t('login.loginSuccess'))
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : t('login.loginFailed')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function handleChangePassword() {
  try {
    await pwdFormRef.value?.validate()
    pwdLoading.value = true
    await updatePassword({
      oldPassword: loginForm.password,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordDialogVisible.value = false
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '密码修改失败'
    ElMessage.error(message)
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: $header-bg;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-lg;
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: $text-primary;
  font-size: $font-size-xxl;
}
</style>
