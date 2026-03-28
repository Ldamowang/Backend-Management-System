import { test as base, type Page } from '@playwright/test'
import { LoginPage } from '../pages/login.page'

/** 默认测试账号 */
export const ACCOUNTS = {
  admin: { username: 'admin', password: 'admin123' },
  user: { username: 'user', password: 'user123' },
} as const

type AuthFixtures = {
  loginPage: LoginPage
  adminPage: Page
  userPage: Page
}

/**
 * 扩展 Playwright test，提供登录相关 fixtures。
 *
 * - loginPage: 未登录状态的 LoginPage 实例
 * - adminPage: 已用 admin 账号登录的 Page
 * - userPage: 已用 user 账号登录的 Page
 */
export const test = base.extend<AuthFixtures>({
  loginPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await use(loginPage)
  },

  adminPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login(ACCOUNTS.admin.username, ACCOUNTS.admin.password)
    await loginPage.expectLoginSuccess()
    await use(page)
  },

  userPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login(ACCOUNTS.user.username, ACCOUNTS.user.password)
    await loginPage.expectLoginSuccess()
    await use(page)
  },
})

export { expect } from '@playwright/test'
