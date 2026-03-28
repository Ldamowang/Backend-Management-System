<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <h2 v-if="!appStore.sidebarCollapsed" class="logo">管理系统</h2>
      <el-icon v-else class="logo-icon"><House /></el-icon>
    </div>

    <el-scrollbar>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <!-- 静态菜单: 仪表盘 -->
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>仪表盘</template>
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
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  background-color: $sidebar-header-bg;
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin: 0;
  white-space: nowrap;
}

.logo-icon {
  font-size: 24px;
  color: #fff;
}

.el-menu {
  border-right: none;
}
</style>
