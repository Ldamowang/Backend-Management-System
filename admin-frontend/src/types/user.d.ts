import type { PageQuery } from './api'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  gender: number
  status: number
  roles: string[]
  permissions: string[]
  createdTime: string
}

export interface UserForm {
  id?: number
  username: string
  password?: string
  nickname: string
  email: string
  phone: string
  gender: number
  status: number
  roleIds: number[]
}

export interface UserQuery extends PageQuery {
  username?: string
  email?: string
  status?: number | string
}

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface UserInfoResult {
  user: UserInfo
  roles: string[]
  permissions: string[]
  menus: MenuItem[]
}

import type { MenuItem } from './menu'
