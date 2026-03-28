import type { FormItemRule } from 'element-plus'

export const requiredRule = (message: string): FormItemRule => ({
  required: true,
  message,
  trigger: 'blur'
})

export const emailRule: FormItemRule = {
  type: 'email',
  message: '请输入有效的邮箱地址',
  trigger: ['blur', 'change']
}

export const phoneRule: FormItemRule = {
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入有效的手机号',
  trigger: ['blur', 'change']
}

export const usernameRule: FormItemRule = {
  min: 3,
  max: 20,
  message: '长度在 3 到 20 个字符',
  trigger: 'blur'
}

export const passwordRule: FormItemRule = {
  min: 6,
  max: 20,
  message: '长度在 6 到 20 个字符',
  trigger: 'blur'
}
