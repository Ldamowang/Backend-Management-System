import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePermissionStore } from '../modules/permission'
import type { MenuItem } from '@/types/menu'

// mock import.meta.glob
vi.mock('@/views/**/*.vue', () => ({}))

const mockMenus: MenuItem[] = [
  {
    id: 2,
    parentId: 0,
    menuName: '系统管理',
    menuType: 1, // 目录
    path: '/system',
    component: '',
    icon: 'Setting',
    sortOrder: 2,
    permission: '',
    visible: 1,
    status: 1,
    children: [
      {
        id: 201,
        parentId: 2,
        menuName: '用户管理',
        menuType: 2, // 菜单
        path: 'user',
        component: 'system/user/index',
        icon: 'User',
        sortOrder: 1,
        permission: 'sys:user:list',
        visible: 1,
        status: 1,
        children: [
          {
            id: 2011,
            parentId: 201,
            menuName: '新增用户',
            menuType: 3, // 按钮
            path: '',
            component: '',
            icon: '',
            sortOrder: 1,
            permission: 'sys:user:add',
            visible: 1,
            status: 1
          }
        ]
      },
      {
        id: 202,
        parentId: 2,
        menuName: '角色管理',
        menuType: 2,
        path: 'role',
        component: 'system/role/index',
        icon: 'UserFilled',
        sortOrder: 2,
        permission: 'sys:role:list',
        visible: 0, // 隐藏
        status: 1
      }
    ]
  }
]

describe('permission store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('初始状态', () => {
    it('dynamicRoutes 为空数组', () => {
      const store = usePermissionStore()
      expect(store.dynamicRoutes).toEqual([])
    })
  })

  describe('generateRoutes', () => {
    it('将菜单树转换为路由', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)

      expect(routes).toHaveLength(1) // 1 个顶级目录
      expect(routes[0].path).toBe('/system')
    })

    it('目录类型包含子路由', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)
      const systemRoute = routes[0]

      // 用户管理 + 角色管理 = 2（按钮类型被过滤）
      expect(systemRoute.children).toHaveLength(2)
      expect(systemRoute.children![0].path).toBe('user')
      expect(systemRoute.children![1].path).toBe('role')
    })

    it('按钮类型 (menuType=3) 不生成路由', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)
      const userRoute = routes[0].children![0]

      // 按钮被过滤，用户管理下无子路由
      expect(userRoute.children).toHaveLength(0)
    })

    it('设置正确的 meta 信息', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)
      const userRoute = routes[0].children![0]

      expect(userRoute.meta?.title).toBe('用户管理')
      expect(userRoute.meta?.icon).toBe('User')
      expect(userRoute.meta?.permission).toBe('sys:user:list')
      expect(userRoute.meta?.hidden).toBe(false) // visible=1
    })

    it('visible=0 的菜单 meta.hidden 为 true', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)
      const roleRoute = routes[0].children![1]

      expect(roleRoute.meta?.hidden).toBe(true)
    })

    it('生成正确的 name（path 转换）', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes(mockMenus)

      expect(routes[0].name).toBe('system')
    })

    it('更新 dynamicRoutes 状态', () => {
      const store = usePermissionStore()
      store.generateRoutes(mockMenus)

      expect(store.dynamicRoutes).toHaveLength(1)
    })

    it('空菜单列表返回空数组', () => {
      const store = usePermissionStore()
      const routes = store.generateRoutes([])

      expect(routes).toEqual([])
      expect(store.dynamicRoutes).toEqual([])
    })
  })

  describe('resetRoutes', () => {
    it('清空动态路由', () => {
      const store = usePermissionStore()
      store.generateRoutes(mockMenus)
      expect(store.dynamicRoutes).toHaveLength(1)

      store.resetRoutes()
      expect(store.dynamicRoutes).toEqual([])
    })
  })
})
