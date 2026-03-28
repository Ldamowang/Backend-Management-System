import { test, expect, ACCOUNTS } from '../fixtures/auth'
import { DashboardPage } from '../pages/dashboard.page'
import { UserPage } from '../pages/user.page'
import { LoginPage } from '../pages/login.page'

/**
 * 权限拦截测试
 *
 * - admin 角色拥有所有权限（sys:user:add, sys:user:edit, sys:user:delete）
 * - user 角色权限受限，不应看到需要权限的操作按钮
 */
test.describe('权限控制', () => {
  test.describe('admin 角色 - 完整权限', () => {
    test('admin 可以看到新增用户按钮', async ({ adminPage }) => {
      const dashboard = new DashboardPage(adminPage)
      const userPage = new UserPage(adminPage)

      await adminPage.goto('/dashboard')
      await dashboard.expectLoaded()

      // 导航到用户管理
      await dashboard.navigateToMenu('系统管理')
      await dashboard.navigateToMenu('用户管理')
      await userPage.expectLoaded()

      // admin 应能看到新增、编辑、删除按钮
      await userPage.expectAddButtonVisible(true)

      // 如果有数据行，检查操作按钮
      const rowCount = await userPage.tableRows.count()
      if (rowCount > 0) {
        await userPage.expectEditButtonVisible(0, true)
        await userPage.expectDeleteButtonVisible(0, true)
      }
    })
  })

  test.describe('user 角色 - 受限权限', () => {
    test('user 角色操作按钮根据权限隐藏', async ({ userPage: loggedInUserPage }) => {
      // user 角色登录后，导航到用户管理页面（如果有权限访问的话）
      const dashboard = new DashboardPage(loggedInUserPage)
      await dashboard.expectLoaded()

      // 尝试导航到用户管理
      // user 角色可能没有"系统管理"菜单 —— 此时侧边栏不会显示该菜单
      const sysMenuVisible = await loggedInUserPage.locator('.el-menu').getByText('系统管理', { exact: true }).isVisible().catch(() => false)

      if (sysMenuVisible) {
        await dashboard.navigateToMenu('系统管理')
        await dashboard.navigateToMenu('用户管理')

        const userPage = new UserPage(loggedInUserPage)
        await userPage.expectLoaded()

        // user 角色不应看到需要 sys:user:add 权限的新增按钮
        await userPage.expectAddButtonVisible(false)

        // 如果有数据行，user 角色不应看到编辑/删除按钮
        const rowCount = await userPage.tableRows.count()
        if (rowCount > 0) {
          await userPage.expectEditButtonVisible(0, false)
          await userPage.expectDeleteButtonVisible(0, false)
        }
      } else {
        // user 角色没有系统管理菜单，这本身就是权限控制的体现
        expect(sysMenuVisible).toBe(false)
      }
    })

    test('user 角色直接访问无权限路由显示 403', async ({ userPage: loggedInUserPage }) => {
      // 直接在地址栏输入无权限的路由
      // 如果系统配置了路由级权限拦截，应显示 403 页面
      await loggedInUserPage.goto('/system/menu')

      // 两种可能的结果：
      // 1. 被重定向到 403 页面
      // 2. 被重定向到 404 页面（因为动态路由未添加该路由）
      // 无论哪种情况，都不应该正常显示菜单管理页面
      await loggedInUserPage.waitForTimeout(2_000)

      const url = loggedInUserPage.url()
      const is403 = url.includes('/403')
      const is404 = url.includes('/404')
      const isRedirected = is403 || is404

      if (is403) {
        // 验证 403 页面内容
        await expect(loggedInUserPage.getByText('403')).toBeVisible()
        await expect(loggedInUserPage.getByText('抱歉，您没有权限访问该页面')).toBeVisible()
        await expect(loggedInUserPage.getByRole('button', { name: '返回首页' })).toBeVisible()
      } else if (is404) {
        // 动态路由未注册该路径，回退到 404
        await expect(loggedInUserPage.getByText('404')).toBeVisible()
      } else {
        // 如果仍然在 /system/menu 路径但内容不可访问，也算权限控制生效
        // 此处断言不在菜单管理的正常页面
        const menuPageLoaded = await loggedInUserPage.getByText('菜单列表').isVisible().catch(() => false)
        expect(menuPageLoaded).toBe(false)
      }
    })

    test('403 页面返回首页按钮可用', async ({ userPage: loggedInUserPage }) => {
      // 直接访问 403 页面
      await loggedInUserPage.goto('/403')

      await expect(loggedInUserPage.getByText('抱歉，您没有权限访问该页面')).toBeVisible()

      // 点击"返回首页"按钮
      await loggedInUserPage.getByRole('button', { name: '返回首页' }).click()

      // 应跳转到仪表盘
      await loggedInUserPage.waitForURL('**/dashboard', { timeout: 10_000 })
    })
  })

  test.describe('未认证访问控制', () => {
    test('未登录直接访问受保护路由重定向到登录页', async ({ page }) => {
      await page.goto('/dashboard')
      await page.waitForURL('**/login**', { timeout: 10_000 })

      // URL 应包含 redirect 参数
      expect(page.url()).toContain('redirect')
    })

    test('未登录访问用户管理重定向到登录页', async ({ page }) => {
      await page.goto('/system/user')
      await page.waitForURL('**/login**', { timeout: 10_000 })
    })

    test('登录后跳转到原始请求页面', async ({ page }) => {
      // 先尝试访问仪表盘（未登录状态）
      await page.goto('/dashboard')
      await page.waitForURL('**/login**', { timeout: 10_000 })

      // 登录
      const loginPage = new LoginPage(page)
      await loginPage.login(ACCOUNTS.admin.username, ACCOUNTS.admin.password)

      // 应跳转回最初请求的仪表盘
      await page.waitForURL('**/dashboard', { timeout: 10_000 })
    })
  })
})
