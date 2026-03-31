import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ForbiddenPage from '../403.vue'

const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ path: '/403' })
}))

describe('403 无权限页面', () => {
  it('显示 403 错误码', () => {
    const wrapper = mount(ForbiddenPage, {
      global: {
        stubs: {
          'el-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            emits: ['click']
          }
        }
      }
    })

    expect(wrapper.find('h1').text()).toBe('403')
  })

  it('显示无权限提示文字', () => {
    const wrapper = mount(ForbiddenPage, {
      global: {
        stubs: {
          'el-button': { template: '<button><slot /></button>' }
        }
      }
    })

    expect(wrapper.find('p').text()).toContain('没有权限访问')
  })

  it('包含返回首页按钮', () => {
    const wrapper = mount(ForbiddenPage, {
      global: {
        stubs: {
          'el-button': { template: '<button class="back-btn"><slot /></button>' }
        }
      }
    })

    expect(wrapper.find('.back-btn').text()).toContain('返回首页')
  })
})
