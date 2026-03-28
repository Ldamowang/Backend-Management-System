import { createRouter, createWebHistory } from 'vue-router'
import { staticRoutes } from './static-routes'
import { setupRouterGuard } from './guard'

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 })
})

setupRouterGuard(router)

export default router
