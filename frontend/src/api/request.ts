import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const request = axios.create({
  // CloudStudio/Vite proxy 模式下使用相对路径，生产部署时可通过 VITE_API_BASE_URL 覆盖
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
})

// 请求拦截：附加 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('hrms-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<unknown>
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      // 登录接口失败（如密码错误）也是 body code 401，此时整页跳转会丢失登录表单
      if (res.code === 401 && !String(response.config.url || '').includes('/auth/login')) {
        localStorage.removeItem('hrms-token')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data as never
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('hrms-token')
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('无权限访问该操作')
    } else {
      ElMessage.error(error?.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

/** 便捷类型化的请求方法（默认 any，业务侧可显式指定泛型） */
export const http = {
  get<T = any>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request.get(url, { params }) as Promise<T>
  },
  post<T = any>(url: string, data?: unknown): Promise<T> {
    return request.post(url, data) as Promise<T>
  },
  put<T = any>(url: string, data?: unknown): Promise<T> {
    return request.put(url, data) as Promise<T>
  },
  delete<T = any>(url: string, params?: Record<string, unknown>): Promise<T> {
    return request.delete(url, { params }) as Promise<T>
  },
}

export default request
