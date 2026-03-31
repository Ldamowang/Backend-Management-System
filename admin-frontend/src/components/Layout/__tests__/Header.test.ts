import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Header from '../Header.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({
    path: '/dashboard',
    matched: [{ path: '/dashboard', meta: { title: '仪表盘' } }]
  })
}))

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
    'el-breadcrumb': { template: '<nav><slot /></nav>' },
    'el-breadcrumb-item': { template: '<span><slot /></span>' },
    Breadcrumb: { template: '<nav class="breadcrumb" />' },
    Fold: { template: '<span>Fold</span>' },
    Expand: { template: '<span>Expand</span>' },
    ArrowDown: { template: '<span />' },
    User: { template: '<span />' },
    SwitchButton: { template: '<span />' }
  }

  it('正确渲染 Header 组件', () => {
    const wrapper = mount(Header, {
      global: { stubs }
    })

    expect(wrapper.find('.header').exists()).toBe(true)
    expect(wrapper.find('.header-left').exists()).toBe(true)
    expect(wrapper.find('.header-right').exists()).toBe(true)
  })

  it('显示用户昵称或默认值', () => {
    const wrapper = mount(Header, {
      global: { stubs }
    })

    expect(wrapper.find('.username').text()).toBe('用户')
  })

  it('包含折叠按钮', () => {
    const wrapper = mount(Header, {
      global: { stubs }
    })

    expect(wrapper.find('.collapse-btn').exists()).toBe(true)
  })

  it('包含个人中心和退出登录菜单项', () => {
    const wrapper = mount(Header, {
      global: { stubs }
    })

    const items = wrapper.findAll('.dropdown-item')
    const texts = items.map(i => i.text())
    expect(texts.some(t => t.includes('个人中心'))).toBe(true)
    expect(texts.some(t => t.includes('退出登录'))).toBe(true)
  })
})
