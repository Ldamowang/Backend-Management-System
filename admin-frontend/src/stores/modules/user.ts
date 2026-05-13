import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/modules/auth'
import { getToken, setToken, setRefreshToken, clearAuth } from '@/utils/auth'
import type { LoginForm, UserInfo } from '@/types/user'
import type { MenuItem } from '@/types/menu'
import { useDictStore } from './dict'
import { useNoticeStore } from './notice'
import { useWebSocket } from '@/composables/useWebSocket'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<MenuItem[]>([])

  async function login(form: LoginForm) {
    const { data } = await loginApi(form)
    if (data.requiresTwoFactor) {
      return { passwordExpired: false, requiresTwoFactor: true }
    }
    token.value = data.accessToken
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    return { passwordExpired: data.passwordExpired ?? false, requiresTwoFactor: false }
  }

  async function fetchUserInfo() {
    const { data } = await getUserInfoApi()
    userInfo.value = data.user
    roles.value = data.roles
    permissions.value = data.permissions
    menus.value = data.menus

    const noticeStore = useNoticeStore()
    noticeStore.fetchUnreadCount()
    noticeStore.initWebSocket(data.user.id)

    // Subscribe to kick-out notification (concurrent login control)
    const { subscribe, connected } = useWebSocket()
    const subscribeKick = () => {
      if (connected.value) {
        subscribe('/user/queue/kick', (msg) => {
          const payload = msg as { message?: string }
          ElMessageBox.alert(
            payload.message || '您的账号在其他设备登录，当前会话已被强制下线',
            '下线通知',
            {
              confirmButtonText: '重新登录',
              type: 'warning',
              showClose: false,
              closeOnClickModal: false,
              callback: () => {
                resetState()
                router.push('/login')
              }
            }
          )
        })
      } else {
        setTimeout(subscribeKick, 500)
      }
    }
    subscribeKick()

    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      resetState()
    }
  }

  function resetState() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    clearAuth()
    useDictStore().clearAll()
    useNoticeStore().cleanup()
  }

  function updateLocalProfile(patch: Partial<UserInfo>) {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...patch }
    }
  }

  function hasPermission(perm: string): boolean {
    if (roles.value.includes('admin')) return true
    return permissions.value.includes(perm)
  }

  return {
    token,
    userInfo,
    roles,
    permissions,
    menus,
    login,
    fetchUserInfo,
    logout,
    resetState,
    updateLocalProfile,
    hasPermission
  }
})
