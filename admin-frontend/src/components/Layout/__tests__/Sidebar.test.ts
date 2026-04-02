import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import Sidebar from '../Sidebar.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/dashboard' })
}))

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

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  messages: {
    'zh-CN': {
      login: { title: '管理系统' },
      menu: { dashboard: '仪表盘' }
    }
  }
})

describe('Sidebar 侧边栏', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const stubs = {
    'el-scrollbar': { template: '<div class="scrollbar"><slot /></div>' },
    'el-menu': { template: '<nav class="menu"><slot /></nav>', props: ['defaultActive', 'collapse', 'collapseTransition', 'router'] },
    'el-menu-item': { template: '<div class="menu-item"><slot /><slot name="title" /></div>', props: ['index'] },
    'el-icon': { template: '<i><slot /></i>' },
    SidebarItem: { template: '<div class="sidebar-item" />', props: ['item'] },
    Monitor: { template: '<span />' },
    House: { template: '<span />' }
  }

  it('正确渲染侧边栏', () => {
    const wrapper = mount(Sidebar, {
      global: { stubs, plugins: [i18n] }
    })

    expect(wrapper.find('.sidebar').exists()).toBe(true)
    expect(wrapper.find('.sidebar-header').exists()).toBe(true)
  })

  it('未折叠时显示系统名称', () => {
    const wrapper = mount(Sidebar, {
      global: { stubs, plugins: [i18n] }
    })

    expect(wrapper.find('.logo').text()).toBe('管理系统')
  })

  it('包含仪表盘静态菜单', () => {
    const wrapper = mount(Sidebar, {
      global: { stubs, plugins: [i18n] }
    })

    const menuItems = wrapper.findAll('.menu-item')
    expect(menuItems.length).toBeGreaterThanOrEqual(1)
    expect(wrapper.text()).toContain('仪表盘')
  })
})
