<template>
  <div class="login-container">
    <!-- 动态背景 -->
    <div class="login-bg">
      <div class="bg-orb bg-orb--1"></div>
      <div class="bg-orb bg-orb--2"></div>
      <div class="bg-orb bg-orb--3"></div>
      <div class="bg-grid"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card animate-fade-in-up">
      <div class="login-brand">
        <div class="brand-icon">
          <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
            <rect width="36" height="36" rx="10" fill="url(#brand-gradient)"/>
            <path d="M10 18L16 12L22 18L16 24L10 18Z" fill="white" opacity="0.9"/>
            <path d="M16 18L22 12L28 18L22 24L16 18Z" fill="white" opacity="0.6"/>
            <defs>
              <linearGradient id="brand-gradient" x1="0" y1="0" x2="36" y2="36">
                <stop stop-color="#6366F1"/>
                <stop offset="1" stop-color="#8B5CF6"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="login-title">{{ $t('login.title') }}</h1>
        <p class="login-subtitle">Enterprise Administration Platform</p>
      </div>

      <el-form ref="formRef" :model="loginForm" :rules="rules" size="large" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            :placeholder="$t('login.usernamePlaceholder')"
            prefix-icon="User"
          />
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
        <el-form-item class="remember-row">
          <el-checkbox v-model="loginForm.remember">{{ $t('login.remember') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            <span v-if="!loading">{{ $t('login.login') }}</span>
            <span v-else>Authenticating...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 底部 -->
    <div class="login-footer animate-fade-in stagger-6">
      <span>&copy; {{ new Date().getFullYear() }} Admin System</span>
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
  background: #080D19;
  position: relative;
  overflow: hidden;
}

// ===== 动态背景 =====
.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: float 8s ease-in-out infinite;

  &--1 {
    width: 500px;
    height: 500px;
    top: -10%;
    left: -5%;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.25), transparent 70%);
    animation-delay: 0s;
  }

  &--2 {
    width: 400px;
    height: 400px;
    bottom: -8%;
    right: -5%;
    background: radial-gradient(circle, rgba(139, 92, 246, 0.2), transparent 70%);
    animation-delay: -3s;
  }

  &--3 {
    width: 300px;
    height: 300px;
    top: 50%;
    left: 60%;
    background: radial-gradient(circle, rgba(16, 185, 129, 0.12), transparent 70%);
    animation-delay: -5s;
  }
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
}

// ===== 登录卡片 =====
.login-card {
  width: 440px;
  padding: 48px 40px 36px;
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(24px) saturate(1.4);
  border-radius: $border-radius-xl;
  border: 1px solid rgba(99, 102, 241, 0.15);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  position: relative;
  z-index: 1;
}

// ===== 品牌区 =====
.login-brand {
  text-align: center;
  margin-bottom: 36px;
}

.brand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    inset: -6px;
    border-radius: 16px;
    background: rgba(99, 102, 241, 0.15);
    animation: pulse-ring 2.5s ease-out infinite;
  }
}

.login-title {
  font-family: $font-family-heading;
  font-size: 26px;
  font-weight: 700;
  color: #F1F5F9;
  margin: 0;
  letter-spacing: -0.02em;
}

.login-subtitle {
  font-size: $font-size-sm;
  color: #64748B;
  margin-top: 6px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

// ===== 表单样式 =====
.login-form {
  :deep(.el-input__wrapper) {
    background: rgba(15, 23, 42, 0.5);
    border: 1px solid rgba(99, 102, 241, 0.12);
    border-radius: $border-radius-base;
    box-shadow: none;
    padding: 4px 16px;
    transition: all $transition-base;

    &:hover {
      border-color: rgba(99, 102, 241, 0.3);
    }

    &.is-focus {
      border-color: $primary-color;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: #E2E8F0;
    &::placeholder { color: #475569; }
  }

  :deep(.el-input__prefix .el-icon) {
    color: #64748B;
  }

  :deep(.el-checkbox__label) {
    color: #94A3B8;
  }

  :deep(.el-checkbox__inner) {
    background: rgba(15, 23, 42, 0.5);
    border-color: rgba(99, 102, 241, 0.2);
  }
}

.remember-row {
  margin-bottom: 8px;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-family: $font-family-heading;
  font-size: $font-size-md;
  font-weight: 600;
  letter-spacing: 0.02em;
  border-radius: $border-radius-base;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  border: none;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.3);
  transition: all $transition-base;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
    background: linear-gradient(135deg, #7577F5, #9B6FF9);
  }

  &:active {
    transform: translateY(0);
  }
}

// ===== 底部 =====
.login-footer {
  position: absolute;
  bottom: 24px;
  left: 0;
  right: 0;
  text-align: center;
  color: #334155;
  font-size: $font-size-xs;
  z-index: 1;
}
</style>
