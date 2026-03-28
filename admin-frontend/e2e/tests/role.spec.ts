import { test, expect } from '../fixtures/auth'
import { DashboardPage } from '../pages/dashboard.page'
import { RolePage } from '../pages/role.page'

/**
 * 角色管理 E2E 测试
 *
 * 前提条件：使用 admin 账号登录（拥有完整 sys:role:* 权限）
 */
test.describe('角色管理', () => {
  let dashboard: DashboardPage
  let rolePage: RolePage

  test.beforeEach(async ({ adminPage }) => {
    dashboard = new DashboardPage(adminPage)
    rolePage = new RolePage(adminPage)

    // 导航到角色管理页面
    await adminPage.goto('/dashboard')
    await dashboard.expectLoaded()
    await dashboard.navigateToMenu('系统管理')
    await dashboard.navigateToMenu('角色管理')
    await rolePage.expectLoaded()
  })

  test('角色列表正确加载', async () => {
    await expect(rolePage.table).toBeVisible()
    // 默认应存在 admin 角色
    await rolePage.expectTableContainsRole('管理员')
  })

  test('角色表格显示必要列', async () => {
    // 验证表头包含关键列
    const headers = rolePage.page.locator('.el-table__header-wrapper')
    await expect(headers.getByText('角色名称')).toBeVisible()
    await expect(headers.getByText('角色标识')).toBeVisible()
    await expect(headers.getByText('状态')).toBeVisible()
    await expect(headers.getByText('操作')).toBeVisible()
  })

  test('新增角色', async () => {
    const testRole = {
      roleName: `E2E测试角色_${Date.now()}`,
      roleKey: `e2e_role_${Date.now()}`,
      description: 'E2E 自动化测试创建的角色',
    }

    await rolePage.createRole(testRole)

    // 验证新角色出现在列表中
    await rolePage.expectTableContainsRole(testRole.roleName)
  })

  test('新增角色 - 表单校验（角色名称必填）', async () => {
    await rolePage.openAddDialog()
    // 不填角色名称，直接提交
    await rolePage.formRoleKey.fill('test_key')
    await rolePage.submitForm()
    // 应显示校验提示
    await expect(rolePage.page.getByText('请输入角色名称')).toBeVisible()
  })

  test('新增角色 - 表单校验（角色标识必填）', async () => {
    await rolePage.openAddDialog()
    // 不填角色标识，直接提交
    await rolePage.formRoleName.fill('测试角色')
    await rolePage.submitForm()
    // 应显示校验提示
    await expect(rolePage.page.getByText('请输入角色标识')).toBeVisible()
  })

  test('编辑角色', async () => {
    // 先创建一个测试角色
    const testRole = {
      roleName: `E2E编辑测试_${Date.now()}`,
      roleKey: `e2e_edit_${Date.now()}`,
    }
    await rolePage.createRole(testRole)
    await rolePage.expectTableContainsRole(testRole.roleName)

    // 找到该角色所在行并编辑
    const row = rolePage.page.locator('.el-table__row', { hasText: testRole.roleName })
    await row.getByRole('button', { name: '编辑' }).click()
    await expect(rolePage.dialogTitle).toHaveText('编辑角色')

    // 修改角色名称
    const newName = `E2E已编辑_${Date.now()}`
    await rolePage.formRoleName.fill(newName)
    await rolePage.submitForm()
    await expect(rolePage.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })

    // 验证编辑成功
    await rolePage.expectTableContainsRole(newName)
  })

  test('删除角色', async () => {
    // 先创建一个测试角色
    const testRole = {
      roleName: `E2E待删除_${Date.now()}`,
      roleKey: `e2e_del_${Date.now()}`,
    }
    await rolePage.createRole(testRole)
    await rolePage.expectTableContainsRole(testRole.roleName)

    // 找到该角色所在行并删除
    const row = rolePage.page.locator('.el-table__row', { hasText: testRole.roleName })
    await row.getByRole('button', { name: '删除' }).click()
    // 确认删除
    await rolePage.page.getByRole('button', { name: '确定' }).click()
    await expect(rolePage.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })

    // 验证角色已被删除
    await rolePage.expectTableNotContainsRole(testRole.roleName)
  })

  test('分配菜单权限对话框正常打开', async () => {
    // 点击第一行的分配权限按钮
    const rowCount = await rolePage.tableRows.count()
    if (rowCount > 0) {
      await rolePage.clickAssignMenuOnRow(0)
      // 验证菜单树可见
      await expect(rolePage.menuTree).toBeVisible()
      // 关闭对话框
      await rolePage.menuDialog.getByRole('button', { name: '取消' }).click()
    }
  })

  test('admin 可以看到所有操作按钮', async () => {
    await rolePage.expectAddButtonVisible(true)

    const rowCount = await rolePage.tableRows.count()
    if (rowCount > 0) {
      await rolePage.expectEditButtonVisible(0, true)
    }
  })
})
