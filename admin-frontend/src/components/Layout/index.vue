<template>
  <el-container class="layout-container">
    <el-aside class="layout-aside" :width="appStore.sidebarCollapsed ? '64px' : '200px'">
      <Sidebar />
    </el-aside>
    <el-container class="layout-main">
      <el-header class="layout-header" height="60px">
        <Header />
      </el-header>
      <el-main class="layout-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import { useAppStore } from '@/stores/modules/app'

const appStore = useAppStore()
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: $sidebar-bg;
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout-main {
  display: flex;
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: center;
  padding: 0;
  background-color: $header-bg;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.layout-content {
  background-color: $content-bg;
  padding: 20px;
  overflow-y: auto;
}
</style>
