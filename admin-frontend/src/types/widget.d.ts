export interface ChartConfig {
  chartType: 'line' | 'bar' | 'pie' | 'area'
  dataSource: 'api' | 'static'
  apiEndpoint?: string
  staticData?: {
    labels: string[]
    values: number[]
  }
}

export interface StatConfig {
  dataSource: 'api' | 'static'
  apiEndpoint?: string
  apiField?: string
  staticValue?: number
  icon: string
  color: string
}

export interface NoteConfig {
  content: string
}

export interface LinkConfig {
  links: Array<{
    name: string
    path: string
    icon: string
    color: string
  }>
}

export type CustomWidgetType = 'chart' | 'stat' | 'note' | 'link'

export interface CustomWidget {
  id: string
  name: string
  type: CustomWidgetType
  span: number
  config: ChartConfig | StatConfig | NoteConfig | LinkConfig
  createdAt: number
  updatedAt: number
}

export interface PresetApi {
  key: string
  name: string
  description: string
  fetcher: () => Promise<Record<string, unknown>>
  fields: Array<{
    key: string
    label: string
    type: 'number' | 'string'
  }>
}
