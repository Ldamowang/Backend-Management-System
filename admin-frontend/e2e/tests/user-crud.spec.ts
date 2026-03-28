import { test, expect } from '../fixtures/auth'
import { DashboardPage } from '../pages/dashboard.page'
import { UserPage } from '../pages/user.page'

/**
 * 用户管理 CRUD 测试
 *
 * 前提条件：使用 admin 账号登录（拥有完整 sys:user:* 权限）
 */
test.describe('用户管理 CRUD', () => {
  let dashboard: DashboardPage
  let userPage: UserPage

  test.beforeEach(async ({ adminPage }) => {
    dashboard = new DashboardPage(adminPage)
    userPage = new UserPage(adminPage)

    // 导航到用户管理页面
    // 通过侧边栏菜单或直接访问
    await adminPage.goto('/dashboard')
    await dashboard.expectLoaded()

    // 点击侧边栏 "系统管理" -> "用户管理"
    await dashboard.navigateToMenu('系统管理')
    await dashboard.navigateToMenu('用户管理')
    await userPage.expectLoaded()
  })

  test('用户列表正确加载', async () => {
    // 表格应可见且包含数据
    await expect(userPage.table).toBeVisible()
    // 至少存在 admin 和 user 两个默认账号
    await userPage.expectTableContainsUsername('admin')
  })

  test('新增用户', async () => {
    const testUser = {
      username: `e2e_test_${Date.now()}`,
      password: 'Test@12345',
      nickname: 'E2E测试用户',
      email: 'e2e@test.com',
      phone: '13800138000',
    }

    await userPage.createUser(testUser)

    // 验证新用户出现在列表中（可能需要搜索）
    await userPage.searchByUsername(testUser.username)
    await userPage.expectTableContainsUsername(testUser.username)
  })

  test('编辑用户', async ({ adminPage }) => {
    // 先创建一个测试用户
    const testUser = {
      username: `e2e_edit_${Date.now()}`,
      password: 'Test@12345',
      nickname: '待编辑用户',
    }
    await userPage.createUser(testUser)

    // 搜索刚创建的用户
    await userPage.searchByUsername(testUser.username)
    await userPage.expectTableContainsUsername(testUser.username)

    // 编辑昵称
    const newNickname = '已编辑用户'
    await userPage.editNickname(0, newNickname)

    // 验证编辑成功：搜索该用户并检查昵称
    await userPage.searchByUsername(testUser.username)
    await expect(userPage.table.getByText(newNickname)).toBeVisible()
  })

  test('删除用户', async ({ adminPage }) => {
    // 先创建一个测试用户
    const testUser = {
      username: `e2e_del_${Date.now()}`,
      password: 'Test@12345',
      nickname: '待删除用户',
    }
    await userPage.createUser(testUser)

    // 搜索并确认存在
    await userPage.searchByUsername(testUser.username)
    await userPage.expectTableContainsUsername(testUser.username)

    // 执行删除
    await userPage.deleteRow(0)

    // 验证用户已被删除
    await userPage.searchByUsername(testUser.username)
    await userPage.expectTableNotContainsUsername(testUser.username)
  })

  test('搜索用户', async () => {
    // 搜索 admin 用户
    await userPage.searchByUsername('admin')
    await userPage.expectTableContainsUsername('admin')

    // 搜索不存在的用户
    await userPage.searchByUsername('nonexistent_user_xyz')
    // 表格应显示空或无匹配行
    const rowCount = await userPage.tableRows.count()
    expect(rowCount).toBe(0)

    // 重置搜索恢复所有数据
    await userPage.resetSearch()
    const resetRowCount = await userPage.tableRows.count()
    expect(resetRowCount).toBeGreaterThan(0)
  })

  test('搜索后重置恢复完整列表', async () => {
    // 记录重置前的行数
    const initialCount = await userPage.tableRows.count()

    // 搜索 admin
    await userPage.searchByUsername('admin')
    const filteredCount = await userPage.tableRows.count()
    expect(filteredCount).toBeLessThanOrEqual(initialCount)

    // 重置
    await userPage.resetSearch()
    const resetCount = await userPage.tableRows.count()
    expect(resetCount).toBe(initialCount)
  })
})
