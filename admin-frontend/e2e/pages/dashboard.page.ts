import { type Page, type Locator, expect } from '@playwright/test'

export class DashboardPage {
  readonly page: Page
  readonly statCards: Locator
  readonly loginTrendChart: Locator
  readonly overviewChart: Locator
  readonly recentLogTable: Locator
  readonly userDropdown: Locator
  readonly sidebarMenu: Locator

  constructor(page: Page) {
    this.page = page
    this.statCards = page.locator('.stat-item')
    this.loginTrendChart = page.locator('text=近7日登录趋势').locator('..')
    this.overviewChart = page.locator('text=系统概览').locator('..')
    this.recentLogTable = page.locator('text=最近登录记录').locator('..')
    this.userDropdown = page.locator('.user-info')
    this.sidebarMenu = page.locator('.el-menu')
  }

  /** 导航到仪表盘 */
  async goto() {
    await this.page.goto('/dashboard')
    await this.page.waitForURL('**/dashboard', { timeout: 10_000 })
  }

  /** 断言：仪表盘页面正确加载 */
  async expectLoaded() {
    await expect(this.page).toHaveURL(/\/dashboard/)
    // 统计卡片至少有 4 个
    await expect(this.statCards.first()).toBeVisible({ timeout: 10_000 })
  }

  /** 断言：统计卡片包含预期标签 */
  async expectStatLabels(labels: string[]) {
    for (const label of labels) {
      await expect(this.page.locator('.stat-label', { hasText: label })).toBeVisible()
    }
  }

  /** 获取用户昵称 */
  async getUsername(): Promise<string> {
    const usernameEl = this.page.locator('.username')
    await expect(usernameEl).toBeVisible()
    return (await usernameEl.textContent()) ?? ''
  }

  /** 点击用户下拉菜单 */
  async openUserDropdown() {
    await this.userDropdown.click()
  }

  /** 执行退出登录流程 */
  async logout() {
    await this.openUserDropdown()
    // 点击"退出登录"菜单项
    await this.page.getByText('退出登录').click()
    // 确认退出对话框
    await this.page.getByRole('button', { name: '确定' }).click()
    // 等待跳转到登录页
    await this.page.waitForURL('**/login', { timeout: 10_000 })
  }

  /** 导航到个人中心 */
  async goToProfile() {
    await this.openUserDropdown()
    await this.page.getByText('个人中心').click()
    await this.page.waitForURL('**/profile', { timeout: 10_000 })
  }

  /** 通过侧边栏导航 */
  async navigateToMenu(menuText: string) {
    await this.sidebarMenu.getByText(menuText, { exact: true }).click()
  }
}
