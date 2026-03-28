import { test, expect } from '../fixtures/auth'
import { DashboardPage } from '../pages/dashboard.page'
import { MenuPage } from '../pages/menu.page'

/**
 * 菜单管理 E2E 测试
 *
 * 前提条件：使用 admin 账号登录（拥有完整 sys:menu:* 权限）
 */
test.describe('菜单管理', () => {
  let dashboard: DashboardPage
  let menuPage: MenuPage

  test.beforeEach(async ({ adminPage }) => {
    dashboard = new DashboardPage(adminPage)
    menuPage = new MenuPage(adminPage)

    // 导航到菜单管理页面
    await adminPage.goto('/dashboard')
    await dashboard.expectLoaded()
    await dashboard.navigateToMenu('系统管理')
    await dashboard.navigateToMenu('菜单管理')
    await menuPage.expectLoaded()
  })

  test('菜单列表正确加载（树形结构）', async () => {
    await expect(menuPage.table).toBeVisible()
    // 验证默认菜单存在
    await menuPage.expectTableContainsMenu('系统管理')
  })

  test('菜单表格显示必要列', async () => {
    const headers = menuPage.page.locator('.el-table__header-wrapper')
    await expect(headers.getByText('菜单名称')).toBeVisible()
    await expect(headers.getByText('类型')).toBeVisible()
    await expect(headers.getByText('路由路径')).toBeVisible()
    await expect(headers.getByText('权限标识')).toBeVisible()
    await expect(headers.getByText('状态')).toBeVisible()
    await expect(headers.getByText('操作')).toBeVisible()
  })

  test('菜单类型标签正确显示', async () => {
    // 菜单树应该包含"目录"和"菜单"类型的标签
    await menuPage.expectMenuTypeVisible('目录')
    await menuPage.expectMenuTypeVisible('菜单')
  })

  test('新增目录类型菜单', async () => {
    const testDir = {
      menuName: `E2E测试目录_${Date.now()}`,
      path: `/e2e-test-${Date.now()}`,
    }

    await menuPage.createDirectory(testDir)

    // 验证新菜单出现在列表中
    await menuPage.expectTableContainsMenu(testDir.menuName)
  })

  test('新增菜单类型', async () => {
    const testMenu = {
      menuName: `E2E测试菜单_${Date.now()}`,
      path: `/e2e-menu-${Date.now()}`,
      component: 'system/test/index',
      permission: `e2e:test:list_${Date.now()}`,
    }

    await menuPage.createMenu(testMenu)

    // 验证新菜单出现在列表中
    await menuPage.expectTableContainsMenu(testMenu.menuName)
  })

  test('新增按钮类型', async () => {
    const testBtn = {
      menuName: `E2E测试按钮_${Date.now()}`,
      permission: `e2e:test:btn_${Date.now()}`,
    }

    await menuPage.createButton(testBtn)

    // 验证新按钮出现在列表中
    await menuPage.expectTableContainsMenu(testBtn.menuName)
  })

  test('新增菜单 - 表单校验（菜单名称必填）', async () => {
    await menuPage.openAddDialog()
    // 不填菜单名称，直接提交
    await menuPage.dialogConfirmBtn.click()
    // 应显示校验提示
    await expect(menuPage.page.getByText('请输入菜单名称')).toBeVisible()
  })

  test('编辑菜单', async () => {
    // 先创建一个测试菜单
    const testDir = {
      menuName: `E2E编辑测试_${Date.now()}`,
      path: `/e2e-edit-${Date.now()}`,
    }
    await menuPage.createDirectory(testDir)
    await menuPage.expectTableContainsMenu(testDir.menuName)

    // 找到该菜单所在行并编辑
    const row = menuPage.page.locator('.el-table__row', { hasText: testDir.menuName })
    await row.getByRole('button', { name: '编辑' }).click()
    await expect(menuPage.dialogTitle).toHaveText('编辑菜单')

    // 修改菜单名称
    const newName = `E2E已编辑_${Date.now()}`
    await menuPage.formMenuName.fill(newName)
    await menuPage.dialogConfirmBtn.click()
    await expect(menuPage.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })

    // 验证编辑成功
    await menuPage.expectTableContainsMenu(newName)
  })

  test('删除菜单', async () => {
    // 先创建一个测试菜单
    const testDir = {
      menuName: `E2E待删除_${Date.now()}`,
      path: `/e2e-del-${Date.now()}`,
    }
    await menuPage.createDirectory(testDir)
    await menuPage.expectTableContainsMenu(testDir.menuName)

    // 找到该菜单所在行并删除
    const row = menuPage.page.locator('.el-table__row', { hasText: testDir.menuName })
    await row.getByRole('button', { name: '删除' }).click()
    // 确认删除
    await menuPage.page.getByRole('button', { name: '确定' }).click()
    await expect(menuPage.page.locator('.el-message--success')).toBeVisible({ timeout: 10_000 })

    // 验证菜单已被删除
    await menuPage.expectTableNotContainsMenu(testDir.menuName)
  })

  test('子级新增按钮可用（非按钮类型行显示新增按钮）', async () => {
    // 找到一个非按钮类型的行（目录或菜单类型）
    const dirRow = menuPage.page.locator('.el-table__row').filter({
      has: menuPage.page.locator('.el-tag', { hasText: '目录' })
    }).first()

    const addChildBtn = dirRow.getByRole('button', { name: '新增' })
    if (await dirRow.isVisible()) {
      await expect(addChildBtn).toBeVisible()
    }
  })

  test('admin 可以看到所有操作按钮', async () => {
    await menuPage.expectAddButtonVisible(true)

    const rowCount = await menuPage.tableRows.count()
    if (rowCount > 0) {
      // 第一行应有编辑和删除按钮
      const firstRow = menuPage.tableRows.first()
      await expect(firstRow.getByRole('button', { name: '编辑' })).toBeVisible()
      await expect(firstRow.getByRole('button', { name: '删除' })).toBeVisible()
    }
  })
})
