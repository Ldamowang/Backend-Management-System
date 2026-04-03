<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <div class="logo-wrapper">
        <svg class="logo-mark" width="28" height="28" viewBox="0 0 36 36" fill="none">
          <rect width="36" height="36" rx="10" fill="url(#sidebar-logo-gradient)"/>
          <path d="M10 18L16 12L22 18L16 24L10 18Z" fill="white" opacity="0.9"/>
          <path d="M16 18L22 12L28 18L22 24L16 18Z" fill="white" opacity="0.6"/>
          <defs>
            <linearGradient id="sidebar-logo-gradient" x1="0" y1="0" x2="36" y2="36">
              <stop stop-color="#6366F1"/>
              <stop offset="1" stop-color="#8B5CF6"/>
            </linearGradient>
          </defs>
        </svg>
        <h2 v-if="!appStore.sidebarCollapsed" class="logo-text">{{ $t('login.title') }}</h2>
      </div>
    </div>

    <el-scrollbar>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="transparent"
        text-color="#94A3B8"
        active-text-color="#fff"
      >
        <!-- 静态菜单: 仪表盘 -->
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>{{ $t('menu.dashboard') }}</template>
        </el-menu-item>

        <!-- 动态菜单 -->
        <template v-for="menu in permissionStore.dynamicRoutes" :key="menu.path">
          <SidebarItem :item="menu" />
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/modules/app'
import { usePermissionStore } from '@/stores/modules/permission'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()
</script>

<style scoped lang="scss">
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #0F172A 0%, #1E293B 100%);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  padding: 0 16px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(99, 102, 241, 0.08);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}

.logo-mark {
  flex-shrink: 0;
}

.logo-text {
  font-family: $font-family-heading;
  font-size: 17px;
  font-weight: 700;
  color: #F1F5F9;
  margin: 0;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

// ===== 菜单样式覆盖 =====
.el-menu {
  border-right: none;
  padding: 8px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 42px;
    line-height: 42px;
    margin-bottom: 2px;
    border-radius: $border-radius-base;
    font-size: $font-size-sm;
    transition: all $transition-base;

    .el-icon {
      font-size: 18px;
    }

    &:hover {
      background: rgba(99, 102, 241, 0.08) !important;
      color: #E2E8F0 !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.15)) !important;
    color: #fff !important;
    font-weight: 500;
    box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      background: $primary-color;
      border-radius: 0 4px 4px 0;
    }
  }

  // 折叠状态
  &.el-menu--collapse {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      padding: 0 !important;
      justify-content: center;
    }
  }
}
</style>
