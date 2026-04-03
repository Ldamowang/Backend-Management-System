<template>
  <div class="shortcut-grid">
    <div v-for="link in links" :key="link.path" class="shortcut-item" @click="router.push(link.path)">
      <el-icon :size="24" :color="link.color"><component :is="link.icon" /></el-icon>
      <span>{{ link.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const links = [
  { path: '/system/user', label: '用户管理', icon: 'User', color: '#409EFF' },
  { path: '/system/role', label: '角色管理', icon: 'UserFilled', color: '#67C23A' },
  { path: '/system/menu', label: '菜单管理', icon: 'Menu', color: '#E6A23C' },
  { path: '/system/notice', label: '通知公告', icon: 'Bell', color: '#F56C6C' }
]
</script>

<style scoped lang="scss">
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  &:hover { background-color: #f5f7fa; }
  span { font-size: 13px; color: #606266; }
}
</style>
