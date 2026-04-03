<template>
  <div class="shortcut-grid">
    <button
      v-for="(link, index) in links"
      :key="link.path"
      class="shortcut-item"
      :class="`stagger-${index + 1}`"
      @click="router.push(link.path)"
    >
      <div class="shortcut-icon" :style="{ background: link.gradient }">
        <el-icon :size="22" color="#fff"><component :is="link.icon" /></el-icon>
      </div>
      <span class="shortcut-label">{{ link.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const links = [
  {
    path: '/system/user', label: '用户管理', icon: 'User',
    gradient: 'linear-gradient(135deg, #6366F1, #818CF8)'
  },
  {
    path: '/system/role', label: '角色管理', icon: 'UserFilled',
    gradient: 'linear-gradient(135deg, #10B981, #34D399)'
  },
  {
    path: '/system/menu', label: '菜单管理', icon: 'Menu',
    gradient: 'linear-gradient(135deg, #F59E0B, #FBBF24)'
  },
  {
    path: '/system/notice', label: '通知公告', icon: 'Bell',
    gradient: 'linear-gradient(135deg, #EF4444, #F87171)'
  }
]
</script>

<style scoped lang="scss">
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  border-radius: $border-radius-base;
  cursor: pointer;
  border: 1px solid transparent;
  background: transparent;
  transition: all $transition-base;

  &:hover {
    background: $border-lighter;
    border-color: $border-light;
    transform: translateY(-2px);

    .shortcut-icon {
      transform: scale(1.1);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.08);
  transition: all $transition-base;
}

.shortcut-label {
  font-size: $font-size-sm;
  font-weight: 500;
  color: $text-regular;
}
</style>
