import { describe, it, expect } from 'vitest'
import { requiredRule, emailRule, phoneRule, usernameRule, passwordRule } from '../validate'

describe('validate utils', () => {
  describe('requiredRule', () => {
    it('生成必填规则', () => {
      const rule = requiredRule('请输入用户名')
      expect(rule.required).toBe(true)
      expect(rule.message).toBe('请输入用户名')
      expect(rule.trigger).toBe('blur')
    })

    it('不同消息生成不同规则', () => {
      const rule1 = requiredRule('请输入密码')
      const rule2 = requiredRule('请输入邮箱')
      expect(rule1.message).toBe('请输入密码')
      expect(rule2.message).toBe('请输入邮箱')
    })
  })

  describe('emailRule', () => {
    it('类型为 email', () => {
      expect(emailRule.type).toBe('email')
    })

    it('包含错误提示信息', () => {
      expect(emailRule.message).toBe('请输入有效的邮箱地址')
    })

    it('trigger 包含 blur 和 change', () => {
      expect(emailRule.trigger).toEqual(['blur', 'change'])
    })
  })

  describe('phoneRule', () => {
    it('定义了手机号正则', () => {
      expect(phoneRule.pattern).toBeInstanceOf(RegExp)
    })

    it('匹配有效手机号', () => {
      const pattern = phoneRule.pattern as RegExp
      expect(pattern.test('13800138000')).toBe(true)
      expect(pattern.test('15912345678')).toBe(true)
      expect(pattern.test('18699887766')).toBe(true)
    })

    it('不匹配无效手机号', () => {
      const pattern = phoneRule.pattern as RegExp
      expect(pattern.test('12345678901')).toBe(false)  // 不以 13-19 开头
      expect(pattern.test('1380013800')).toBe(false)   // 位数不够
      expect(pattern.test('138001380001')).toBe(false)  // 位数过多
      expect(pattern.test('abcdefghijk')).toBe(false)   // 非数字
    })
  })

  describe('usernameRule', () => {
    it('长度限制 3-20', () => {
      expect(usernameRule.min).toBe(3)
      expect(usernameRule.max).toBe(20)
    })

    it('包含提示信息', () => {
      expect(usernameRule.message).toBe('长度在 3 到 20 个字符')
    })
  })

  describe('passwordRule', () => {
    it('长度限制 6-20', () => {
      expect(passwordRule.min).toBe(6)
      expect(passwordRule.max).toBe(20)
    })

    it('包含提示信息', () => {
      expect(passwordRule.message).toBe('长度在 6 到 20 个字符')
    })
  })
})
