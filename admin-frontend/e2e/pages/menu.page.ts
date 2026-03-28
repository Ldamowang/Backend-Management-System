import { type Page, type Locator, expect } from '@playwright/test'

export class MenuPage {
  readonly page: Page
  readonly table: Locator
  readonly tableRows: Locator
  readonly addButton: Locator

  /** 新增/编辑对话框 */
  readonly dialog: Locator
  readonly dialogTitle: Locator
  readonly formMenuName: Locator
  readonly formPath: Locator
  readonly formComponent: Locator
  readonly formPermission: Locator
  readonly formIcon: Locator
  readonly dialogConfirmBtn: Locator
  readonly dialogCancelBtn: Locator

  constructor(page: Page) {
    this.page = page

    this.table = page.locator('.el-table')
    this.tableRows = page.locator('.el-table__body-wrapper .el-table__row')
    this.addButton = page.getByRole('button', { name: '新增菜单' })

    // 对话框
    this.dialog = page.locator('.el-dialog')
    this.dialogTitle = page.locator('.el-dialog__title')
    this.formMenuName = page.locator('.el-dialog').getByPlaceholder('请输入菜单名称')
    this.formPath = page.locator('.el-dialog').getByPlaceholder('如 /system/user')
    this.formComponent = page.locator('.el-dialog').getByPlaceholder('如 system/user/index')
    this.formPermission = page.locator('.el-dialog').getByPlaceholder('如 sys:user:list')
    this.formIcon = page.locator('.el-dialog').getByPlaceholder('图标名称 (如 User, Setting)')
    this.dialogConfirmBtn = page.locator('.el-dialog').getByRole('button', { name: '确定' })
    this.dialogCancelBtn = page.locator('.el-dialog').getByRole('button', { name: '取消' })
  }

  /** 断言：菜单管理页面已加载 */
  async expectLoaded() {
    await expect(this.page.getByText('菜单管理')).toBeVisible({ timeout: 10_000 })
    await expect(this.table).toBeVisible({ timeout: 10_000 })
  }

  // ──────────── 菜单类型选择 ────────────

  /** 选择菜单类型 */
  async selectMenuType(type: '目录' | '菜单' | '按钮') {
    await this.page.locator('.el-dialog').getByText(type, { exact: true }).click()
  }

  // ──────────── CRUD 操作 ────────────

  /** 打开新增菜单对话框（顶级菜单） */
  async openAddDialog() {
    await this.addButton.click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('新增菜单')
  }

  /** 新增目录类型菜单 */
  async createDirectory(data: {
    menuName: string
    path: string
    icon?: string
  }) {
    await this.openAddDialog()
    await this.selectMenuType('目录')
    await this.formMenuName.fill(data.menuName)
    await this.formPath.fill(data.path)
    if (data.icon) await this.formIcon.fill(data.icon)
    await this.dialogConfirmBtn.click()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 新增菜单类型 */
  async createMenu(data: {
    menuName: string
    path: string
    component?: string
    permission?: string
    icon?: string
  }) {
    await this.openAddDialog()
    await this.selectMenuType('菜单')
    await this.formMenuName.fill(data.menuName)
    await this.formPath.fill(data.path)
    if (data.component) await this.formComponent.fill(data.component)
    if (data.permission) await this.formPermission.fill(data.permission)
    if (data.icon) await this.formIcon.fill(data.icon)
    await this.dialogConfirmBtn.click()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 新增按钮类型 */
  async createButton(data: {
    menuName: string
    permission: string
  }) {
    await this.openAddDialog()
    await this.selectMenuType('按钮')
    await this.formMenuName.fill(data.menuName)
    await this.formPermission.fill(data.permission)
    await this.dialogConfirmBtn.click()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 点击某行的编辑按钮 */
  async clickEditOnRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '编辑' }).click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('编辑菜单')
  }

  /** 编辑菜单名称 */
  async editMenuName(rowIndex: number, newName: string) {
    await this.clickEditOnRow(rowIndex)
    await this.formMenuName.fill(newName)
    await this.dialogConfirmBtn.click()
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

  /** 点击某行的子级新增按钮 */
  async clickAddChildOnRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '新增' }).click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('新增菜单')
  }

  // ──────────── 断言 ────────────

  /** 断言：表格包含指定菜单名 */
  async expectTableContainsMenu(menuName: string) {
    await expect(this.table.getByText(menuName, { exact: true })).toBeVisible({ timeout: 10_000 })
  }

  /** 断言：表格不包含指定菜单名 */
  async expectTableNotContainsMenu(menuName: string) {
    await expect(this.table.getByText(menuName, { exact: true })).not.toBeVisible({ timeout: 5_000 })
  }

  /** 断言：新增按钮是否可见 */
  async expectAddButtonVisible(visible: boolean) {
    if (visible) {
      await expect(this.addButton).toBeVisible()
    } else {
      await expect(this.addButton).not.toBeVisible()
    }
  }

  /** 断言：表格包含指定类型标签 */
  async expectMenuTypeVisible(type: '目录' | '菜单' | '按钮') {
    await expect(this.table.getByText(type).first()).toBeVisible()
  }
}
