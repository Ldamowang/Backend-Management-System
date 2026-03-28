import { type Page, type Locator, expect } from '@playwright/test'

export class RolePage {
  readonly page: Page
  readonly table: Locator
  readonly tableRows: Locator
  readonly addButton: Locator

  /** 新增/编辑对话框 */
  readonly dialog: Locator
  readonly dialogTitle: Locator
  readonly formRoleName: Locator
  readonly formRoleKey: Locator
  readonly formDescription: Locator
  readonly dialogConfirmBtn: Locator
  readonly dialogCancelBtn: Locator

  /** 分配菜单权限对话框 */
  readonly menuDialog: Locator
  readonly menuDialogConfirmBtn: Locator
  readonly menuTree: Locator

  constructor(page: Page) {
    this.page = page

    this.table = page.locator('.el-table')
    this.tableRows = page.locator('.el-table__body-wrapper .el-table__row')
    this.addButton = page.getByRole('button', { name: '新增角色' })

    // 对话框 - 使用第一个可见的 el-dialog
    this.dialog = page.locator('.el-dialog').first()
    this.dialogTitle = page.locator('.el-dialog__title').first()
    this.formRoleName = page.locator('.el-dialog').first().getByPlaceholder('请输入角色名称')
    this.formRoleKey = page.locator('.el-dialog').first().getByPlaceholder('请输入角色标识')
    this.formDescription = page.locator('.el-dialog').first().getByPlaceholder('请输入描述')
    this.dialogConfirmBtn = page.locator('.el-dialog').first().getByRole('button', { name: '确定' })
    this.dialogCancelBtn = page.locator('.el-dialog').first().getByRole('button', { name: '取消' })

    // 分配权限对话框
    this.menuDialog = page.locator('.el-dialog:has-text("分配菜单权限")')
    this.menuDialogConfirmBtn = this.menuDialog.getByRole('button', { name: '确定' })
    this.menuTree = this.menuDialog.locator('.el-tree')
  }

  /** 断言：角色管理页面已加载 */
  async expectLoaded() {
    await expect(this.page.getByText('角色管理')).toBeVisible({ timeout: 10_000 })
    await expect(this.table).toBeVisible({ timeout: 10_000 })
  }

  // ──────────── CRUD 操作 ────────────

  /** 打开新增角色对话框 */
  async openAddDialog() {
    await this.addButton.click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('新增角色')
  }

  /** 填写角色表单 */
  async fillForm(data: {
    roleName: string
    roleKey?: string
    description?: string
  }) {
    await this.formRoleName.fill(data.roleName)
    if (data.roleKey) await this.formRoleKey.fill(data.roleKey)
    if (data.description) await this.formDescription.fill(data.description)
  }

  /** 提交表单 */
  async submitForm() {
    await this.dialogConfirmBtn.click()
  }

  /** 新增角色完整流程 */
  async createRole(data: {
    roleName: string
    roleKey: string
    description?: string
  }) {
    await this.openAddDialog()
    await this.fillForm(data)
    await this.submitForm()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  /** 点击某行的编辑按钮 */
  async clickEditOnRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '编辑' }).click()
    await expect(this.dialog).toBeVisible()
    await expect(this.dialogTitle).toHaveText('编辑角色')
  }

  /** 编辑角色名称 */
  async editRoleName(rowIndex: number, newName: string) {
    await this.clickEditOnRow(rowIndex)
    await this.formRoleName.fill(newName)
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

  /** 点击某行的分配权限按钮 */
  async clickAssignMenuOnRow(rowIndex: number) {
    const row = this.tableRows.nth(rowIndex)
    await row.getByRole('button', { name: '分配权限' }).click()
    await expect(this.menuDialog).toBeVisible({ timeout: 10_000 })
  }

  /** 提交菜单权限分配 */
  async submitMenuAssign() {
    await this.menuDialogConfirmBtn.click()
    await expect(this.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })
  }

  // ──────────── 断言 ────────────

  /** 断言：表格包含指定角色名 */
  async expectTableContainsRole(roleName: string) {
    await expect(this.table.getByText(roleName, { exact: true })).toBeVisible({ timeout: 10_000 })
  }

  /** 断言：表格不包含指定角色名 */
  async expectTableNotContainsRole(roleName: string) {
    await expect(this.table.getByText(roleName, { exact: true })).not.toBeVisible({ timeout: 5_000 })
  }

  /** 断言：新增角色按钮是否可见 */
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
}
