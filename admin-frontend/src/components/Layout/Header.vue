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
        <button class="action-btn" @click="searchDialog?.search.open()">
          <el-icon><Search /></el-icon>
        </button>
      </el-tooltip>

      <el-badge :value="noticeStore.unreadCount" :hidden="noticeStore.unreadCount === 0" :max="99" class="header-icon-btn">
        <button class="action-btn" @click="router.push('/profile')">
          <el-icon><Bell /></el-icon>
        </button>
      </el-badge>

      <el-dropdown @command="handleLocaleChange" class="locale-switcher">
        <button class="action-btn locale-text">{{ currentLocaleName }}</button>
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

      <button class="action-btn" @click="settingsVisible = true">
        <el-icon><Setting /></el-icon>
      </button>

      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <div class="user-avatar">
            {{ (userStore.userInfo?.nickname || 'A').charAt(0).toUpperCase() }}
          </div>
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
import { useNoticeStore } from '@/stores/modules/notice'
import { loadLanguage, availableLocales } from '@/locales'
import Breadcrumb from './Breadcrumb.vue'
import SettingsDrawer from './SettingsDrawer.vue'
import SearchDialog from '@/components/SearchDialog/index.vue'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const noticeStore = useNoticeStore()
const { t, locale } = useI18n()
const settingsVisible = ref(false)
const searchDialog = ref<InstanceType<typeof SearchDialog>>()

const currentLocaleName = computed(() => {
  return availableLocales.find(l => l.code === locale.value)?.name || '中文'
})

async function handleLocaleChange(code: string) {
  await loadLanguage(code)
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
  if (userStore.token) noticeStore.fetchUnreadCount()
  searchDialog.value?.search?.setupShortcut()
})
</script>

<style scoped lang="scss">
.header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: $text-secondary;
  padding: 6px;
  border-radius: $border-radius-sm;
  transition: all $transition-base;

  &:hover {
    color: $primary-color;
    background: rgba(99, 102, 241, 0.06);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-icon-btn {
  display: flex;
  align-items: center;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  font-size: 18px;
  cursor: pointer;
  color: $text-secondary;
  border-radius: $border-radius-sm;
  transition: all $transition-base;

  &:hover {
    background: rgba(99, 102, 241, 0.06);
    color: $primary-color;
  }
}

.locale-text {
  width: auto;
  padding: 0 10px;
  font-size: $font-size-sm;
  font-family: $font-family-body;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: $border-radius-base;
  margin-left: 4px;
  transition: all $transition-base;

  &:hover {
    background: rgba(99, 102, 241, 0.06);
  }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: $border-radius-sm;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: $font-family-heading;
  font-size: $font-size-sm;
  font-weight: 600;
}

.username {
  font-size: $font-size-sm;
  font-weight: 500;
  color: $text-primary;
}

.arrow-down {
  font-size: 12px;
  color: $text-secondary;
}
</style>
