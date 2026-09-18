import { del, get, post } from './request'
import { ssePost } from '@/utils/sse'
import { aiConfigHeaders } from '@/utils/aiConfig'
import type { SseConnection, SseHandlers } from '@/utils/sse'
import type {
  ChatMessage,
  ChatMessageSendReq,
  ChatSession,
  ChatSessionCreateReq,
  PageData
} from '@/types'

/** GET /chat/sessions — 会话列表（按更新时间倒序） */
export function listChatSessions(): Promise<ChatSession[]> {
  return get<ChatSession[]>('/chat/sessions')
}

/** POST /chat/sessions — 新建会话（title 为空由首条消息自动生成） */
export function createChatSession(data: ChatSessionCreateReq = {}): Promise<ChatSession> {
  return post<ChatSession>('/chat/sessions', { title: '', agentMode: 'BUILTIN', ...data })
}

/** DELETE /chat/sessions/{id} — 删除会话及其消息 */
export function deleteChatSession(id: number): Promise<null> {
  return del<null>(`/chat/sessions/${id}`)
}

/** GET /chat/sessions/{id}/messages — 历史消息（按时间正序） */
export function listMessages(sessionId: number, page = 1, pageSize = 50): Promise<PageData<ChatMessage>> {
  return get<PageData<ChatMessage>>(`/chat/sessions/${sessionId}/messages`, { page, pageSize })
}

/**
 * POST /chat/sessions/{id}/messages — 发送消息（SSE 流式）
 * 通过 fetch + ReadableStream 解析 chunk/done/error 事件（见 utils/sse.ts）。
 * 携带用户自带 AI 配置请求头（X-AI-*）—— 后端不得落库、不得进日志。
 */
export function sendMessage(sessionId: number, data: ChatMessageSendReq, handlers: SseHandlers): SseConnection {
  return ssePost(`/api/chat/sessions/${sessionId}/messages`, data, handlers, aiConfigHeaders())
}
