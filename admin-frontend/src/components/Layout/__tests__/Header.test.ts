import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import Header from '../Header.vue'

const mockPush = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal() as object
  return {
    ...actual,
    useRouter: () => ({ push: mockPush, currentRoute: { value: { path: '/dashboard' } } }),
    useRoute: () => ({
      path: '/dashboard',
      matched: [{ path: '/dashboard', meta: { title: '仪表盘' } }]
    })
  }
})

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual as object,
    ElMessageBox: { confirm: vi.fn().mockResolvedValue('confirm') }
  }
})

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  setRefreshToken: vi.fn(),
  clearAuth: vi.fn()
}))

vi.mock('@/api/modules/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getUserInfo: vi.fn()
}))

vi.mock('@/api/modules/notice', () => ({
  getUnreadCount: vi.fn().mockResolvedValue({ data: 0 }),
  markNoticeRead: vi.fn().mockResolvedValue({ data: null }),
  markAllNoticesRead: vi.fn().mockResolvedValue({ data: null })
}))

vi.mock('@/composables/useWebSocket', () => ({
  useWebSocket: vi.fn(() => ({
    connect: vi.fn(),
    subscribe: vi.fn(),
    disconnect: vi.fn(),
    connected: { value: false },
    client: { value: null }
  }))
}))

vi.mock('@/api/modules/user', () => ({
  getUserList: vi.fn().mockResolvedValue({ data: { list: [] } })
}))
vi.mock('@/api/modules/role', () => ({
  getRoleList: vi.fn().mockResolvedValue({ data: [] })
}))
vi.mock('@/api/modules/config', () => ({
  getConfigs: vi.fn().mockResolvedValue({ data: [] })
}))
vi.mock('@/api/modules/dict', () => ({
  getDictTypes: vi.fn().mockResolvedValue({ data: [] })
}))

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  messages: {
    'zh-CN': {
      common: {
        action: { confirm: '确定', cancel: '取消' },
        header: { profile: '个人中心', logout: '退出登录', logoutConfirm: '确定要退出登录吗？' },
        message: { tip: '提示' },
        search: { placeholder: '搜索...' }
      },
      system: { user: { username: '用户' } }
    }
  }
})

describe('Header 顶部栏', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const stubs = {
    'el-icon': { template: '<i><slot /></i>' },
    'el-dropdown': { template: '<div class="dropdown"><slot /><slot name="dropdown" /></div>' },
    'el-dropdown-menu': { template: '<div><slot /></div>' },
    'el-dropdown-item': { template: '<div class="dropdown-item"><slot /></div>', props: ['command'] },
    'el-avatar': { template: '<span class="avatar" />', props: ['size', 'icon'] },
    'el-badge': { template: '<span class="badge"><slot /></span>' },
    'el-tooltip': { template: '<span><slot /></span>' },
    Breadcrumb: { template: '<nav class="breadcrumb" />' },
    SettingsDrawer: { template: '<div />' },
    SearchDialog: { template: '<div />' },
    Fold: { template: '<span>Fold</span>' },
    Expand: { template: '<span>Expand</span>' },
    ArrowDown: { template: '<span />' },
    User: { template: '<span />' },
    SwitchButton: { template: '<span />' },
    Bell: { template: '<span />' },
    Setting: { template: '<span />' },
    Search: { template: '<span />' }
  }

  it('正确渲染 Header 组件', () => {
    const wrapper = mount(Header, {
      global: { stubs, plugins: [i18n] }
    })

    expect(wrapper.find('.header').exists()).toBe(true)
    expect(wrapper.find('.header-left').exists()).toBe(true)
    expect(wrapper.find('.header-right').exists()).toBe(true)
  })

  it('显示用户昵称或默认值', () => {
    const wrapper = mount(Header, {
      global: { stubs, plugins: [i18n] }
    })

    expect(wrapper.find('.username').exists()).toBe(true)
  })

  it('包含折叠按钮', () => {
    const wrapper = mount(Header, {
      global: { stubs, plugins: [i18n] }
    })

    expect(wrapper.find('.collapse-btn').exists()).toBe(true)
  })

  it('包含个人中心和退出登录菜单项', () => {
    const wrapper = mount(Header, {
      global: { stubs, plugins: [i18n] }
    })

    const items = wrapper.findAll('.dropdown-item')
    expect(items.length).toBeGreaterThanOrEqual(2)
  })
})
