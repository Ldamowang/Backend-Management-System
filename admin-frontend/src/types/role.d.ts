export interface RoleInfo {
  id: number
  roleName: string
  roleKey: string
  sortOrder: number
  status: number
  description: string
  menuIds?: number[]
  createdTime: string
}

export interface RoleForm {
  id?: number
  roleName: string
  roleKey: string
  sortOrder: number
  status: number
  description: string
  menuIds: number[]
}
