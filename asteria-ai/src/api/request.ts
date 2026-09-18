import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { aiConfigHeaders } from '@/utils/aiConfig'
import type { ApiResponse } from '@/types'

/* ============================================================
   axios 实例（见 docs/03 3.0）：
   - baseURL /api（Vite proxy → http://localhost:8080）
   - 解包统一响应 {code,message,data}
   - code !== 0 时 reject 并 ElMessage 提示；40100 预留跳转
   ============================================================ */

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})

/* 请求拦截器：用户已配置 AI 服务时，随请求头携带 X-AI-* 配置。
   该请求携带用户自带密钥 —— 后端不得落库、不得进日志。 */
instance.interceptors.request.use((config) => {
  Object.assign(config.headers, aiConfigHeaders())
  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data as never
      }
      // 40100 未登录/登录过期：MVP 预留，暂不使用
      if (body.code === 40100) {
        // TODO: 预留跳转登录
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || `请求失败（code ${body.code}）`))
    }
    // 非统一结构（异常兜底），原样返回
    return response.data as never
  },
  (error) => {
    // 网络错误 / 超时 / 5xx：后端未就绪时静默 reject，由页面决定空态或提示
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    }
    return Promise.reject(error)
  }
)

/** GET 请求，返回解包后的 data */
export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return instance.get(url, { params }) as unknown as Promise<T>
}

/** POST 请求，返回解包后的 data */
export function post<T>(url: string, data?: unknown): Promise<T> {
  return instance.post(url, data) as unknown as Promise<T>
}

/** DELETE 请求，返回解包后的 data */
export function del<T>(url: string): Promise<T> {
  return instance.delete(url) as unknown as Promise<T>
}

/** PUT 请求，返回解包后的 data */
export function put<T>(url: string, data?: unknown): Promise<T> {
  return instance.put(url, data) as unknown as Promise<T>
}

/** multipart 上传 */
export function upload<T>(url: string, formData: FormData): Promise<T> {
  return instance.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  }) as unknown as Promise<T>
}

export default instance
