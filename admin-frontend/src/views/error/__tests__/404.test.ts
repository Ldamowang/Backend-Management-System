import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NotFoundPage from '../404.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/404' })
}))

describe('404 页面不存在', () => {
  it('显示 404 错误码', () => {
    const wrapper = mount(NotFoundPage, {
      global: {
        stubs: {
          'el-button': { template: '<button><slot /></button>' }
        }
      }
    })

    expect(wrapper.find('h1').text()).toBe('404')
  })

  it('显示页面不存在提示', () => {
    const wrapper = mount(NotFoundPage, {
      global: {
        stubs: {
          'el-button': { template: '<button><slot /></button>' }
        }
      }
    })

    expect(wrapper.find('p').text()).toContain('页面不存在')
  })

  it('包含返回首页按钮', () => {
    const wrapper = mount(NotFoundPage, {
      global: {
        stubs: {
          'el-button': { template: '<button class="back-btn"><slot /></button>' }
        }
      }
    })

    expect(wrapper.find('.back-btn').text()).toContain('返回首页')
  })
})
