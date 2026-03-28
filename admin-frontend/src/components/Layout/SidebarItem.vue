<template>
  <!-- 有子菜单 -->
  <el-sub-menu v-if="item.children?.length" :index="item.path">
    <template #title>
      <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
      <span>{{ item.meta?.title }}</span>
    </template>
    <template v-for="child in item.children" :key="child.path">
      <SidebarItem v-if="child.children?.length" :item="child" />
      <el-menu-item v-else :index="child.path">
        <el-icon v-if="child.meta?.icon"><component :is="child.meta.icon" /></el-icon>
        <template #title>{{ child.meta?.title }}</template>
      </el-menu-item>
    </template>
  </el-sub-menu>

  <!-- 无子菜单 -->
  <el-menu-item v-else :index="item.path">
    <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
    <template #title>{{ item.meta?.title }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router'

defineProps<{ item: RouteRecordRaw }>()
</script>
