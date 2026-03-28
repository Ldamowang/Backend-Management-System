/** 统一 API 响应 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应数据 */
export interface PageData<T = unknown> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/** 分页响应 */
export type PageResponse<T = unknown> = ApiResponse<PageData<T>>
