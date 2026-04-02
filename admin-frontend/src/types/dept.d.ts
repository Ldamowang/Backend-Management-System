export interface DeptItem {
  id: number
  parentId: number
  deptName: string
  sortOrder: number
  leader: string
  phone: string
  email: string
  status: number
  createdTime: string
  children?: DeptItem[]
}

export interface DeptSimpleItem {
  id: number
  deptName: string
  children?: DeptSimpleItem[]
}

export interface DeptForm {
  id?: number
  parentId: number
  deptName: string
  sortOrder: number
  leader: string
  phone: string
  email: string
  status: number
}
