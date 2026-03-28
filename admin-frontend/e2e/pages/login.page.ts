import { type Page, type Locator, expect } from '@playwright/test'

export class LoginPage {
  readonly page: Page
  readonly title: Locator
  readonly usernameInput: Locator
  readonly passwordInput: Locator
  readonly rememberCheckbox: Locator
  readonly loginButton: Locator

  constructor(page: Page) {
    this.page = page
    this.title = page.locator('.login-title')
    this.usernameInput = page.getByPlaceholder('请输入用户名')
    this.passwordInput = page.getByPlaceholder('请输入密码')
    this.rememberCheckbox = page.getByLabel('记住密码')
    this.loginButton = page.getByRole('button', { name: '登 录' })
  }

  /** 导航到登录页 */
  async goto() {
    await this.page.goto('/login')
    await this.title.waitFor({ state: 'visible' })
  }

  /** 填写表单并点击登录 */
  async login(username: string, password: string) {
    await this.usernameInput.fill(username)
    await this.passwordInput.fill(password)
    await this.loginButton.click()
  }

  /** 勾选记住密码后登录 */
  async loginWithRemember(username: string, password: string) {
    await this.usernameInput.fill(username)
    await this.passwordInput.fill(password)
    await this.rememberCheckbox.check()
    await this.loginButton.click()
  }

  /** 断言：登录成功后跳转到仪表盘 */
  async expectLoginSuccess() {
    // 等待 Element Plus 的成功消息
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
    // 等待 URL 跳转到仪表盘
    await this.page.waitForURL('**/dashboard', { timeout: 10_000 })
  }

  /** 断言：登录失败显示错误消息 */
  async expectLoginFailed(message?: string) {
    const errorMsg = this.page.locator('.el-message--error')
    await expect(errorMsg).toBeVisible({ timeout: 10_000 })
    if (message) {
      await expect(errorMsg).toContainText(message)
    }
  }

  /** 断言：仍在登录页 */
  async expectStillOnLoginPage() {
    await expect(this.page).toHaveURL(/\/login/)
    await expect(this.title).toBeVisible()
  }
}
