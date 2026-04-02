import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PasswordStrength from '../PasswordStrength.vue'

describe('PasswordStrength 密码强度指示器', () => {
  it('空密码不显示强度条', () => {
    const wrapper = mount(PasswordStrength, {
      props: { password: '' }
    })

    expect(wrapper.find('.password-strength').exists()).toBe(false)
  })

  it('弱密码显示 strength-weak', () => {
    const wrapper = mount(PasswordStrength, {
      props: { password: 'abc' }
    })

    expect(wrapper.find('.password-strength').exists()).toBe(true)
    expect(wrapper.find('.strength-text').classes()).toContain('strength-weak')
    expect(wrapper.find('.strength-text').text()).toBe('弱')

    // Only 1 bar should have the weak class
    const bars = wrapper.findAll('.strength-bar')
    expect(bars).toHaveLength(3)
    expect(bars[0].classes()).toContain('strength-weak')
    expect(bars[1].classes()).toContain('strength-empty')
    expect(bars[2].classes()).toContain('strength-empty')
  })

  it('中等密码显示 strength-medium', () => {
    // 'Abcdef1' — has upper+lower (+1), has digit (+1) = score 2 => level 2
    const wrapper = mount(PasswordStrength, {
      props: { password: 'Abcdef1' }
    })

    expect(wrapper.find('.strength-text').classes()).toContain('strength-medium')
    expect(wrapper.find('.strength-text').text()).toBe('中')

    const bars = wrapper.findAll('.strength-bar')
    expect(bars[0].classes()).toContain('strength-medium')
    expect(bars[1].classes()).toContain('strength-medium')
    expect(bars[2].classes()).toContain('strength-empty')
  })

  it('强密码显示 strength-strong', () => {
    // 'Abcdef1!' — length>=8 (+1), upper+lower (+1), digit (+1), special (+1) = score 4 => level 3
    const wrapper = mount(PasswordStrength, {
      props: { password: 'Abcdef1!' }
    })

    expect(wrapper.find('.strength-text').classes()).toContain('strength-strong')
    expect(wrapper.find('.strength-text').text()).toBe('强')

    const bars = wrapper.findAll('.strength-bar')
    expect(bars[0].classes()).toContain('strength-strong')
    expect(bars[1].classes()).toContain('strength-strong')
    expect(bars[2].classes()).toContain('strength-strong')
  })
})
