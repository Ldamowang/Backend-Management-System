import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import Breadcrumb from '../Breadcrumb.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    path: '/system/user',
    matched: [
      { path: '/', meta: { title: '首页' } },
      { path: '/system', meta: { title: '系统管理' } },
      { path: '/system/user', meta: { title: '用户管理' } }
    ]
  })
}))

describe('Breadcrumb 面包屑导航', () => {
  const stubs = {
    'el-breadcrumb': { template: '<nav class="breadcrumb"><slot /></nav>', props: ['separator'] },
    'el-breadcrumb-item': {
      template: '<span class="breadcrumb-item"><slot /></span>',
      props: ['to']
    }
  }

  it('正确渲染面包屑', () => {
    const wrapper = mount(Breadcrumb, {
      global: { stubs }
    })

    expect(wrapper.find('.breadcrumb').exists()).toBe(true)
  })

  it('首项始终为首页', () => {
    const wrapper = mount(Breadcrumb, {
      global: { stubs }
    })

    const items = wrapper.findAll('.breadcrumb-item')
    expect(items[0].text()).toBe('首页')
  })

  it('显示路由匹配的面包屑项', () => {
    const wrapper = mount(Breadcrumb, {
      global: { stubs }
    })

    const items = wrapper.findAll('.breadcrumb-item')
    // 首页 + 系统管理 + 用户管理 (首页和/dashboard被过滤, 但这里/不是/dashboard)
    expect(items.length).toBeGreaterThanOrEqual(2)
  })
})
