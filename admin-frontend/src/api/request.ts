import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, setToken, setRefreshToken, clearAuth, getRefreshToken } from '@/utils/auth'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

// 是否正在刷新 Token
let isRefreshing = false
// 等待刷新的请求队列
let requestQueue: Array<(token: string) => void> = []

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message } = response.data
    if (code === 200) {
      return response.data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const { response, config } = error

    // 401: Token 过期，尝试刷新
    if (response?.status === 401 && !config._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          requestQueue.push((token: string) => {
            config.headers.Authorization = `Bearer ${token}`
            resolve(service(config))
          })
        })
      }

      config._retry = true
      isRefreshing = true

      try {
        const refreshToken = getRefreshToken()
        if (!refreshToken) throw new Error('No refresh token')

        const { data } = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
          { refreshToken }
        )

        const newToken = data.data.accessToken
        setToken(newToken)
        setRefreshToken(data.data.refreshToken)

        // 重新执行队列中的请求
        requestQueue.forEach(cb => cb(newToken))
        requestQueue = []

        config.headers.Authorization = `Bearer ${newToken}`
        return service(config)
      } catch (refreshError) {
        requestQueue.forEach(cb => cb(''))
        requestQueue = []
        clearAuth()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    const msg = response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
