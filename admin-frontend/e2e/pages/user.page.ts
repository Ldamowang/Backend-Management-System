import { type Page, type Locator, expect } from '@playwright/test'

export class UserPage {
  readonly page: Page
  readonly searchUsername: Locator
  readonly searchEmail: Locator
  readonly searchStatus: Locator
  readonly searchButton: Locator
  readonly resetButton: Locator
  readonly addButton: Locator
  readonly table: Locator
  readonly tableRows: Locator
  readonly pagination: Locator

  /** 对话框相关 */
  readonly dialog: Locator
  readonly dialogTitle: Locator
  readonly formUsername: Locator
  readonly formPassword: Locator
  readonly formNickname: Locator
  readonly formEmail: Locator
  readonly formPhone: Locator
  readonly dialogConfirmBtn: Locator
  readonly dialogCancelBtn: Locator

  constructor(page: Page) {
    this.page = page

    // 搜索区域
    this.searchUsername = page.locator('.search-card').getByPlaceholder('请输入用户名')
    this.searchEmail = page.locator('.search-card').getByPlaceholder('请输入邮箱')
    this.searchStatus = page.locator('.search-card').getByPlaceholder('请选择')
    this.searchButton = page.locator('.search-card').getByRole('button', { name: '搜索' })
    this.resetButton = page.locator('.search-card').getByRole('button', { name: '重置' })

    // 列表区域
    this.addButton = page.getByRole('button', { name: '新增用户' })
    this.table = page.locator('.el-table')
    this.tableRows = page.locator('.el-table__body-wrapper .el-table__row')
    this.pagination = page.locator('.el-pagination')

    // 对话框
    this.dialog = page.locator('.el-dialog')
    this.dialogTitle = page.locator('.el-dialog__title')
    this.formUsername = page.locator('.el-dialog').getByPlaceholder('请输入用户名')
    this.formPassword = page.locator('.el-dialog').getByPlaceholder('请输入密码')
    this.formNickname = page.locator('.el-dialog').getByPlaceholder('请输入昵称')
    this.formEmail = page.locator('.el-dialog').getByPlaceholder('请输入邮箱')
    this.formPhone = page.locator('.el-dialog').getByPlaceholder('请输入手机号')
    this.dialogConfirmBtn = page.locator('.el-dialog').getByRole('button', { name: '确定' })
    this.dialogCancelBtn = page.locator('.el-dialog').getByRole('button', { name: '取消' })
  }

  /** 断言：用户管理页面已加载 */
  async expectLoaded() {
    await expect(this.page.getByText('用户列表')).toBeVisible({ timeout: 10_000 })
    await expect(this.table).toBeVisible({ timeout: 10_000 })
  }

  // ──────────── 搜索操作 ────────────

  /** 按用户名搜索 */
  async searchByUsername(username: string) {
    await this.searchUsername.fill(username)
    await this.searchButton.click()
    // 等待表格数据刷新
    await this.page.waitForTimeout(500)
  }

  /** 重置搜索 */
  async resetSearch() {
    await this.resetButton.click()
    await this.page.waitForTimeout(500)
  }

  // ──────────── CRUD 操作 ────────────

  /** 打开新增用户对话框 */
  async openAddDialog() {
    await this.addButton.click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('新增用户')
  }

  /** 填写用户表单（新增） */
  async fillCreateForm(data: {
    username: string
    password: string
    nickname?: string
    email?: string
    phone?: string
  }) {
    await this.formUsername.fill(data.username)
    await this.formPassword.fill(data.password)
    if (data.nickname) await this.formNickname.fill(data.nickname)
    if (data.email) await this.formEmail.fill(data.email)
    if (data.phone) await this.formPhone.fill(data.phone)
  }

  /** 提交对话框表单 */
  async submitForm() {
    await this.dialogConfirmBtn.click()
  }

  /** 新增用户完整流程 */
  async createUser(data: {
    username: string
    password: string
    nickname?: string
    email?: string
    phone?: string
  }) {
    await this.openAddDialog()
    await this.fillCreateForm(data)
    await this.submitForm()
    // 等待成功提示
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 点击某行的编辑按钮 */
  async clickEditOnRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '编辑' }).click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('编辑用户')
  }

  /** 编辑用户昵称 */
  async editNickname(rowIndex: number, newNickname: string) {
    await this.clickEditOnRow(rowIndex)
    await this.formNickname.fill(newNickname)
    await this.submitForm()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 点击某行的删除按钮并确认 */
  async deleteRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '删除' }).click()
    // 确认弹窗
    await this.page.getByRole('button', { name: '确定' }).click()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  // ──────────── 断言 ────────────

  /** 断言：表格包含指定用户名 */
  async expectTableContainsUsername(username: string) {
    await expect(this.table.getByText(username, { exact: true })).toBeVisible({ timeout: 10_000 })
  }

  /** 断言：表格不包含指定用户名 */
  async expectTableNotContainsUsername(username: string) {
    await expect(this.table.getByText(username, { exact: true })).not.toBeVisible({ timeout: 5_000 })
  }

  /** 断言：表格行数 */
  async expectRowCount(count: number) {
    await expect(this.tableRows).toHaveCount(count, { timeout: 10_000 })
  }

  /** 断言：新增用户按钮是否可见 */
  async expectAddButtonVisible(visible: boolean) {
    if (visible) {
      await expect(this.addButton).toBeVisible()
    } else {
      await expect(this.addButton).not.toBeVisible()
    }
  }

  /** 断言：某行编辑按钮是否可见 */
  async expectEditButtonVisible(rowIndex: number, visible: boolean) {
    const row = this.tableRows.nth(rowIndex)
    const editBtn = row.getByRole('button', { name: '编辑' })
    if (visible) {
      await expect(editBtn).toBeVisible()
    } else {
      await expect(editBtn).not.toBeVisible()
    }
  }

  /** 断言：某行删除按钮是否可见 */
  async expectDeleteButtonVisible(rowIndex: number, visible: boolean) {
    const row = this.tableRows.nth(rowIndex)
    const deleteBtn = row.getByRole('button', { name: '删除' })
    if (visible) {
      await expect(deleteBtn).toBeVisible()
    } else {
      await expect(deleteBtn).not.toBeVisible()
    }
  }
}
