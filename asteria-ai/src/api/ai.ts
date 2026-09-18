import { post } from './request'
import type { AiTestResult } from '@/types'

/**
 * POST /ai/test — 用当前配置测试 AI 是否可用。
 *
 * 不用传任何参数：配置由 request.ts 的拦截器自动挂到 X-AI-* 请求头上。
 * 返回 { ok, message }：ok 判断成败，message 是原因（可直接显示给用户）。
 */
export function testAiConfig(): Promise<AiTestResult> {
  return post<AiTestResult>('/ai/test')
}
