import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/modules/auth'
import { getToken, setToken, setRefreshToken, clearAuth } from '@/utils/auth'
import type { LoginForm, UserInfo } from '@/types/user'
import type { MenuItem } from '@/types/menu'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<MenuItem[]>([])

  async function login(form: LoginForm) {
    const { data } = await loginApi(form)
    token.value = data.accessToken
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
  }

  async function fetchUserInfo() {
    const { data } = await getUserInfoApi()
    userInfo.value = data.user
    roles.value = data.roles
    permissions.value = data.permissions
    menus.value = data.menus
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
