import type { SseChunkData, SseDoneData, SseErrorData } from '@/types'

/* ============================================================
   SSE 客户端：fetch + ReadableStream 解析 text/event-stream
   用于 POST /chat/sessions/{id}/messages（见 docs/03 3.5）
   ============================================================ */

export interface SseHandlers {
  onChunk?: (data: SseChunkData) => void
  onDone?: (data: SseDoneData) => void
  onError?: (error: SseErrorData) => void
}

export interface SseConnection {
  /** 中断连接（后端落盘已生成内容并标记 interrupted） */
  abort: () => void
  /** 整个流结束（含 done / error / 网络异常）时 resolve */
  finished: Promise<void>
}

/**
 * 发起 SSE POST 请求。
 * 解析 `event:` / `data:` 行，按 chunk / done / error 分发。
 * headers 参数用于携带用户 AI 配置（X-AI-*，见 utils/aiConfig.ts）。
 */
export function ssePost(
  url: string,
  body: unknown,
  handlers: SseHandlers,
  headers: Record<string, string> = {}
): SseConnection {
  const controller = new AbortController()

  const finished = (async () => {
    let buffer = ''
    try {
      const resp = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...headers
        },
        body: JSON.stringify(body),
        signal: controller.signal
      })

      if (!resp.ok || !resp.body) {
        handlers.onError?.({ code: resp.status, message: `请求失败（${resp.status}）` })
        return
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')

      // 解析一个 SSE 事件块（以空行分隔）
      const dispatch = (rawEvent: string) => {
        let event = 'message'
        const dataLines: string[] = []
        for (const line of rawEvent.split('\n')) {
          if (line.startsWith('event:')) {
            event = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trimStart())
          }
        }
        if (dataLines.length === 0) return
        const dataStr = dataLines.join('\n')
        let data: unknown
        try {
          data = JSON.parse(dataStr)
        } catch {
          return // 非 JSON 数据行，忽略
        }
        if (event === 'chunk') {
          handlers.onChunk?.(data as SseChunkData)
        } else if (event === 'done') {
          handlers.onDone?.(data as SseDoneData)
        } else if (event === 'error') {
          handlers.onError?.(data as SseErrorData)
        }
      }

      for (;;) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 事件以空行分隔
        let idx: number
        while ((idx = buffer.search(/\r?\n\r?\n/)) !== -1) {
          const rawEvent = buffer.slice(0, idx)
          const sepMatch = buffer.slice(idx).match(/^\r?\n\r?\n/)
          buffer = buffer.slice(idx + (sepMatch ? sepMatch[0].length : 2))
          if (rawEvent.trim()) dispatch(rawEvent)
        }
      }
      // 流结束时 flush 残余 buffer
      if (buffer.trim()) dispatch(buffer)
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        return // 用户主动中断，不视为错误
      }
      handlers.onError?.({
        code: 50000,
        message: err instanceof Error ? err.message : '网络连接异常'
      })
    }
  })()

  return {
    abort: () => controller.abort(),
    finished
  }
}
