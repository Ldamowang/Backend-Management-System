import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TableToolbar from '../TableToolbar.vue'

describe('TableToolbar', () => {
  const stubs = {
    'el-button': { template: '<button @click="$emit(\'click\')"><slot /></button>', props: ['icon', 'circle', 'size', 'text', 'type'] },
    'el-popover': { template: '<div><slot name="reference" /><slot /></div>' },
    'el-checkbox': { template: '<label><slot /></label>', props: ['modelValue'] },
    'el-radio-group': { template: '<div><slot /></div>', props: ['modelValue'] },
    'el-radio-button': { template: '<span><slot /></span>', props: ['label'] },
    'el-icon': { template: '<i><slot /></i>' },
    'el-tooltip': { template: '<span><slot /></span>', props: ['content'] },
    'el-divider': { template: '<hr />' }
  }

  it('渲染工具栏', () => {
    const wrapper = mount(TableToolbar, {
      props: {
        columns: [
          { key: 'name', label: '姓名', visible: true },
          { key: 'email', label: '邮箱', visible: true }
        ],
        density: 'default'
      },
      global: { stubs }
    })
    expect(wrapper.find('.table-toolbar').exists()).toBe(true)
  })

  it('显示所有列设置项', () => {
    const wrapper = mount(TableToolbar, {
      props: {
        columns: [
          { key: 'name', label: '姓名', visible: true },
          { key: 'email', label: '邮箱', visible: true },
          { key: 'phone', label: '电话', visible: false }
        ],
        density: 'default'
      },
      global: { stubs }
    })
    const items = wrapper.findAll('.column-item')
    expect(items).toHaveLength(3)
  })

  it('点击刷新按钮触发 refresh 事件', async () => {
    const wrapper = mount(TableToolbar, {
      props: {
        columns: [{ key: 'name', label: '姓名', visible: true }],
        density: 'default'
      },
      global: { stubs }
    })
    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    expect(wrapper.emitted('refresh')).toBeTruthy()
  })
})
