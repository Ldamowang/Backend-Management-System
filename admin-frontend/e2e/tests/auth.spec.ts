import { test, expect, ACCOUNTS } from '../fixtures/auth'
import { LoginPage } from '../pages/login.page'
import { DashboardPage } from '../pages/dashboard.page'

test.describe('认证流程', () => {
  test.describe('登录', () => {
    test('admin 账号登录成功并跳转到仪表盘', async ({ loginPage }) => {
      await loginPage.login(ACCOUNTS.admin.username, ACCOUNTS.admin.password)
      await loginPage.expectLoginSuccess()

      const dashboard = new DashboardPage(loginPage.page)
      await dashboard.expectLoaded()
      await dashboard.expectStatLabels(['用户总数', '角色数量', '登录总数', '系统状态'])
    })

    test('user 账号登录成功', async ({ loginPage }) => {
      await loginPage.login(ACCOUNTS.user.username, ACCOUNTS.user.password)
      await loginPage.expectLoginSuccess()

      const dashboard = new DashboardPage(loginPage.page)
      await dashboard.expectLoaded()
    })

    test('错误密码登录失败', async ({ loginPage }) => {
      await loginPage.login(ACCOUNTS.admin.username, 'wrongpassword')
      await loginPage.expectLoginFailed()
      await loginPage.expectStillOnLoginPage()
    })

    test('空用户名无法提交（表单校验）', async ({ loginPage }) => {
      await loginPage.login('', ACCOUNTS.admin.password)
      // 应触发表单校验，显示"请输入用户名"提示
      await expect(loginPage.page.getByText('请输入用户名')).toBeVisible()
      await loginPage.expectStillOnLoginPage()
    })

    test('空密码无法提交（表单校验）', async ({ loginPage }) => {
      await loginPage.login(ACCOUNTS.admin.username, '')
      // 应触发表单校验，显示"请输入密码"提示
      await expect(loginPage.page.getByText('请输入密码')).toBeVisible()
      await loginPage.expectStillOnLoginPage()
    })
  })

  test.describe('登出', () => {
    test('admin 登出后跳转到登录页', async ({ adminPage }) => {
      const dashboard = new DashboardPage(adminPage)
      await dashboard.expectLoaded()

      await dashboard.logout()

      // 验证已到达登录页
      const loginPage = new LoginPage(adminPage)
      await loginPage.expectStillOnLoginPage()
    })

    test('登出后访问仪表盘重定向到登录页', async ({ adminPage }) => {
      const dashboard = new DashboardPage(adminPage)
      await dashboard.logout()

      // 尝试直接访问仪表盘
      await adminPage.goto('/dashboard')
      // 应被路由守卫拦截，跳转到登录页
      await adminPage.waitForURL('**/login**', { timeout: 10_000 })
    })
  })

  test.describe('路由守卫', () => {
    test('未登录访问仪表盘重定向到登录页', async ({ page }) => {
      await page.goto('/dashboard')
      await page.waitForURL('**/login**', { timeout: 10_000 })
    })

    test('已登录访问登录页重定向到仪表盘', async ({ adminPage }) => {
      await adminPage.goto('/login')
      await adminPage.waitForURL('**/dashboard', { timeout: 10_000 })
    })
  })
})
