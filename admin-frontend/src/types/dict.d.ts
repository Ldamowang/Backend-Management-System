export interface DictType {
  id: number
  dictName: string
  dictType: string
  status: number
  description: string
  createdTime: string
}

export interface DictTypeForm {
  id?: number
  dictName: string
  dictType: string
  status: number
  description: string
}

export interface DictData {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  sortOrder: number
  status: number
  description: string
  createdTime: string
}

export interface DictDataForm {
  id?: number
  dictType: string
  dictLabel: string
  dictValue: string
  sortOrder: number
  status: number
  description: string
}
