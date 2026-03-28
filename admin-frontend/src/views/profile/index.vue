<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header><span>个人信息</span></template>
          <div class="profile-avatar-section">
            <el-avatar :size="80" :icon="User" />
            <h3 class="profile-name">{{ profileData.nickname || '未设置' }}</h3>
            <p class="profile-username">{{ profileData.username }}</p>
            <el-divider />
            <div class="text-left">
              <p><el-icon><Message /></el-icon> {{ profileData.email || '未设置' }}</p>
              <p class="profile-info-item"><el-icon><Phone /></el-icon> {{ profileData.phone || '未设置' }}</p>
              <p class="profile-info-item"><el-icon><Male /></el-icon> {{ genderLabel }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="info">
              <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="80px" class="profile-form">
                <el-form-item label="昵称" prop="nickname">
                  <el-input v-model="infoForm.nickname" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="infoForm.phone" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-radio-group v-model="infoForm.gender">
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                    <el-radio :value="0">保密</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="infoLoading" @click="handleUpdateInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" class="profile-form">
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdLoading" @click="handleUpdatePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Message, Phone, Male } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/modules/user'
import { getProfile, updateProfile, updatePassword, type ProfileInfo } from '@/api/modules/profile'

const userStore = useUserStore()
const activeTab = ref('info')
const infoFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const infoLoading = ref(false)
const pwdLoading = ref(false)

const profileData = reactive<ProfileInfo>({
  id: 0, username: '', nickname: '', email: '', phone: '', avatar: '', gender: 0
})

const infoForm = reactive({ nickname: '', email: '', phone: '', gender: 0 })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const genderLabel = computed(() => {
  const map: Record<number, string> = { 0: '保密', 1: '男', 2: '女' }
  return map[profileData.gender] ?? '保密'
})

const infoRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_r: unknown, value: string, callback: (error?: Error) => void) => {
        value !== pwdForm.newPassword ? callback(new Error('两次密码不一致')) : callback()
      },
      trigger: 'blur'
    }
  ]
}

async function fetchProfile() {
  try {
    const { data } = await getProfile()
    Object.assign(profileData, data)
    Object.assign(infoForm, {
      nickname: data.nickname,
      email: data.email,
      phone: data.phone,
      gender: data.gender
    })
  } catch {
    // 降级到 store 数据
    if (userStore.userInfo) {
      Object.assign(profileData, userStore.userInfo)
      Object.assign(infoForm, {
        nickname: userStore.userInfo.nickname,
        email: userStore.userInfo.email,
        phone: userStore.userInfo.phone,
        gender: userStore.userInfo.gender
      })
    }
  }
}

async function handleUpdateInfo() {
  const valid = await infoFormRef.value?.validate().catch(() => false)
  if (!valid) return
  infoLoading.value = true
  try {
    await updateProfile({
      nickname: infoForm.nickname,
      email: infoForm.email,
      phone: infoForm.phone,
      gender: infoForm.gender
    })
    Object.assign(profileData, infoForm)
    userStore.updateLocalProfile({
      nickname: infoForm.nickname,
      email: infoForm.email,
      phone: infoForm.phone,
      gender: infoForm.gender
    })
    ElMessage.success('资料更新成功')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    infoLoading.value = false
  }
}

async function handleUpdatePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch {
    // Axios 拦截器已处理错误提示
  } finally {
    pwdLoading.value = false
  }
}

onMounted(fetchProfile)
</script>
