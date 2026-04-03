<template>
  <el-container class="layout-container">
    <el-aside class="layout-aside" :width="appStore.sidebarCollapsed ? '64px' : '220px'">
      <Sidebar />
    </el-aside>
    <el-container class="layout-main">
      <el-header class="layout-header" height="60px">
        <Header />
      </el-header>
      <TabBar v-if="appStore.showTagsView" />
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <keep-alive :include="tabStore.cachedViews">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import TabBar from './TabBar.vue'
import { useAppStore } from '@/stores/modules/app'
import { useTabStore } from '@/stores/modules/tab'

const appStore = useAppStore()
const tabStore = useTabStore()
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: var(--sidebar-bg, $sidebar-bg);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.layout-main {
  display: flex;
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: center;
  padding: 0;
  background-color: var(--header-bg, $header-bg);
  border-bottom: 1px solid var(--header-border, #{$border-light});
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
  z-index: 5;
}

.layout-content {
  background-color: var(--content-bg, $content-bg);
  padding: 24px;
  overflow-y: auto;
}

// 页面切换动画
.page-enter-active {
  animation: fadeInUp 0.3s ease both;
}

.page-leave-active {
  animation: fadeIn 0.15s ease reverse both;
}
</style>
