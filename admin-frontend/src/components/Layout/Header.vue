<template>
  <div class="header">
    <div class="header-left">
      <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <Breadcrumb />
    </div>

    <div class="header-right">
      <el-tooltip content="Ctrl+K" placement="bottom">
        <el-icon class="action-icon" @click="searchDialog?.search.open()"><Search /></el-icon>
      </el-tooltip>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="header-icon-btn">
        <el-icon class="action-icon" @click="router.push('/profile')"><Bell /></el-icon>
      </el-badge>
      <el-dropdown @command="handleLocaleChange" class="locale-switcher">
        <span class="action-icon locale-text">{{ currentLocaleName }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="loc in availableLocales"
              :key="loc.code"
              :command="loc.code"
              :disabled="locale === loc.code"
            >
              {{ loc.name }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-icon class="action-icon" @click="settingsVisible = true"><Setting /></el-icon>
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :icon="User" />
          <span class="username">{{ userStore.userInfo?.nickname || $t('system.user.username') }}</span>
          <el-icon class="arrow-down"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>{{ $t('common.header.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>{{ $t('common.header.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <SettingsDrawer v-model="settingsVisible" />
    <SearchDialog ref="searchDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/modules/app'
import { useUserStore } from '@/stores/modules/user'
import { getUnreadCount } from '@/api/modules/notice'
import { loadLanguage, availableLocales } from '@/locales'
import Breadcrumb from './Breadcrumb.vue'
import SettingsDrawer from './SettingsDrawer.vue'
import SearchDialog from '@/components/SearchDialog/index.vue'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const { t, locale } = useI18n()
const settingsVisible = ref(false)
const unreadCount = ref(0)
const searchDialog = ref<InstanceType<typeof SearchDialog>>()

const currentLocaleName = computed(() => {
  return availableLocales.find(l => l.code === locale.value)?.name || '中文'
})

async function handleLocaleChange(code: string) {
  await loadLanguage(code)
}

async function fetchUnreadCount() {
  try {
    const { data } = await getUnreadCount()
    unreadCount.value = data
  } catch { /* ignore */ }
}

async function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm(t('common.header.logoutConfirm'), t('common.message.tip'), {
        confirmButtonText: t('common.action.confirm'),
        cancelButtonText: t('common.action.cancel'),
        type: 'warning'
      })
      await userStore.logout()
      router.push('/login')
    } catch {
      // 取消
    }
  }
}

onMounted(() => {
  if (userStore.token) fetchUnreadCount()
  searchDialog.value?.search?.setupShortcut()
})
</script>

<style scoped lang="scss">
.header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: #606266;
  &:hover { color: $primary-color; }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-btn {
  display: flex;
  align-items: center;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  color: #606266;
  padding: 6px;
  border-radius: 4px;
  &:hover { background-color: #f5f7fa; color: var(--el-color-primary); }
}

.locale-text {
  font-size: 13px;
  cursor: pointer;
  color: #606266;
  padding: 6px 8px;
  border-radius: 4px;
  &:hover { background-color: #f5f7fa; color: var(--el-color-primary); }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  &:hover { background-color: #f5f7fa; }
}

.username {
  font-size: 14px;
  color: #606266;
}

.arrow-down {
  font-size: 12px;
  color: #909399;
}
</style>
