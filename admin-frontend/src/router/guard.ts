import type { Router } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/modules/user'
import { usePermissionStore } from '@/stores/modules/permission'
import { useTabStore } from '@/stores/modules/tab'
import { notFoundRoute } from './static-routes'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/403', '/404']

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    NProgress.start()
    document.title = `${to.meta.title || ''} - 后台管理系统`

    const token = getToken()

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' })
        return
      }

      const userStore = useUserStore()
      if (userStore.userInfo) {
        next()
        return
      }

      // 已登录但没有用户信息，获取信息并生成动态路由
      try {
        const data = await userStore.fetchUserInfo()
        const permissionStore = usePermissionStore()
        const routes = permissionStore.generateRoutes(data.menus)

        // 添加动态路由
        routes.forEach(route => {
          router.addRoute('Layout', route)
        })
        // 添加 404 兜底
        router.addRoute(notFoundRoute)

        // 重新导航到目标页面
        next({ ...to, replace: true })
      } catch {
        userStore.resetState()
        next(`/login?redirect=${to.path}`)
      }
    } else {
      if (whiteList.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })

  router.afterEach((to) => {
    NProgress.done()

    // 自动添加标签页（排除白名单和隐藏路由）
    if (!whiteList.includes(to.path) && !to.meta.hidden) {
      const tabStore = useTabStore()
      tabStore.addTab({
        path: to.path,
        title: (to.meta.title as string) || '',
        icon: (to.meta.icon as string) || '',
        name: (to.name as string) || ''
      })
    }
  })
}
