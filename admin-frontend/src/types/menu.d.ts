export interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path: string
  component: string
  icon: string
  sortOrder: number
  permission: string
  visible: number
  status: number
  children?: MenuItem[]
}

export interface MenuForm {
  id?: number
  parentId: number
  menuName: string
  menuType: number
  path: string
  component: string
  icon: string
  sortOrder: number
  permission: string
  visible: number
  status: number
}
