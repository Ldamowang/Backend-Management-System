import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem } from '@/types/menu'

// 页面组件映射
const viewModules = import.meta.glob('@/views/**/*.vue')

/** 将后端菜单转为 Vue Router 路由 */
function generateRoute(menu: MenuItem): RouteRecordRaw | null {
  // 按钮类型不生成路由
  if (menu.menuType === 3) return null

  const route: RouteRecordRaw = {
    path: menu.path,
    name: menu.path.replace(/\//g, '-').replace(/^-/, ''),
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      permission: menu.permission,
      hidden: menu.visible === 0
    },
    children: []
  }

  // 菜单类型，有具体组件
  if (menu.menuType === 2 && menu.component) {
    const componentPath = `/src/views/${menu.component}.vue`
    route.component = viewModules[componentPath]
  }

  // 目录类型，递归子菜单
  if (menu.children?.length) {
    const children = menu.children
      .map(generateRoute)
      .filter(Boolean) as RouteRecordRaw[]
    route.children = children
  }

  return route
}

export const usePermissionStore = defineStore('permission', () => {
  const dynamicRoutes = ref<RouteRecordRaw[]>([])

  function generateRoutes(menus: MenuItem[]): RouteRecordRaw[] {
    const routes = menus
      .filter(m => m.path !== '/dashboard')
      .map(generateRoute)
      .filter(Boolean) as RouteRecordRaw[]
    dynamicRoutes.value = routes
    return routes
  }

  function resetRoutes() {
    dynamicRoutes.value = []
  }

  return { dynamicRoutes, generateRoutes, resetRoutes }
})
