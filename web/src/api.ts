import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('educore.token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (envelope?.code && envelope.code !== 'SUCCESS') {
      const error = new Error(envelope.message || '请求失败')
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return envelope?.data ?? envelope
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('educore.token')
      localStorage.removeItem('educore.user')
      window.dispatchEvent(new Event('educore:unauthorized'))
    }
    const message = error.response?.data?.message || (error.code === 'ERR_NETWORK' ? '无法连接服务，请检查后端是否已启动' : error.message || '请求失败')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export const api = {
  get: <T = any>(path: string, params?: Record<string, unknown>) => http.get<any, T>(path, { params }),
  post: <T = any>(path: string, data?: unknown) => http.post<any, T>(path, data),
  put: <T = any>(path: string, data?: unknown) => http.put<any, T>(path, data),
  patch: <T = any>(path: string, data?: unknown) => http.patch<any, T>(path, data),
}
