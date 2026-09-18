import { defineStore } from 'pinia'
import { createChatSession, deleteChatSession, listChatSessions, listMessages, sendMessage } from '@/api/chat'
import type { SseConnection } from '@/utils/sse'
import type { ChatMessage, ChatSession, SseDoneData } from '@/types'

/* ============================================================
   打字机的节奏（想调快慢就改这两个数）
   模型吐字比人眼阅读快得多，有时还一次给一大块；
   所以要把「数据到达」和「文字上屏」解耦：先入队，再按固定节奏放出来。
   ============================================================ */
/** 每个字的间隔（毫秒）：24 ≈ 42 字/秒；40 ≈ 25 字/秒（很从容）；10 ≈ 100 字/秒（几乎看不出打字机） */
const TYPE_INTERVAL_MS = 24
/** 队列积压超过这么多字就加速吐字，避免"AI 早说完了、字还在慢慢爬" */
const TYPE_CATCH_UP_AT = 40

/** 会话列表、当前会话、流式接收状态（见 docs/02 2.5，可从简） */
export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [] as ChatSession[],
    activeSessionId: null as number | null,
    /** sessionId → 消息列表 */
    messages: {} as Record<number, ChatMessage[]>,
    streaming: false,
    /** 本地临时消息 id 计数（流式追加阶段消息尚无后端 id） */
    _tempId: -1
  }),
  getters: {
    activeMessages(state): ChatMessage[] {
      return state.activeSessionId != null ? state.messages[state.activeSessionId] ?? [] : []
    }
  },
  actions: {
    async fetchSessions() {
      try {
        this.sessions = await listChatSessions()
      } catch {
        this.sessions = []
      }
    },
    async newSession(): Promise<ChatSession | null> {
      try {
        const session = await createChatSession()
        this.sessions.unshift(session)
        return session
      } catch {
        return null
      }
    },
    async removeSession(id: number) {
      try {
        await deleteChatSession(id)
      } catch {
        // 删除失败也先从列表移除失败提示已由 request 层给出
        return
      }
      this.sessions = this.sessions.filter((s) => s.id !== id)
      delete this.messages[id]
      if (this.activeSessionId === id) this.activeSessionId = null
    },
    async openSession(id: number) {
      this.activeSessionId = id
      if (this.messages[id]) return
      try {
        const data = await listMessages(id)
        this.messages[id] = data.list
      } catch {
        this.messages[id] = []
      }
    },
    /** 发送消息并流式接收；返回 SSE 连接（可 abort 停止生成） */
    send(content: string, callbacks?: { onError?: () => void }): SseConnection | null {
      const sessionId = this.activeSessionId
      if (sessionId == null) return null
      const list = (this.messages[sessionId] ??= [])

      const now = new Date().toISOString()
      list.push({
        id: this._tempId--,
        sessionId,
        role: 'user',
        content,
        createdAt: now
      })
      list.push({
        id: this._tempId--,
        sessionId,
        role: 'assistant',
        content: '',
        createdAt: now
      })

      // ⚠️ 一定要通过 list[assistantIndex] 改内容：push 进去的那个"裸对象"直接改
      //    不会触发 Vue 的视图更新（表现：回复要全部生成完才一次性出现）。
      const assistantIndex = list.length - 1

      // ===== 打字机：队列和定时器都是本轮 send 的局部变量，多轮对话之间互不干扰 =====
      let queue: string[] = []
      let timer: ReturnType<typeof setTimeout> | null = null
      let streamFinished = false
      let donePayload: SseDoneData | null = null

      /** 每次"滴答"：从队列里取几个字上屏；队列空了且流也结束了，才真正收尾 */
      const pump = () => {
        timer = null
        const msg = list[assistantIndex]
        if (queue.length > 0) {
          // 积压越多，一次吐越多（否则会越落越远，AI 早就说完了字还在爬）
          const take = queue.length > TYPE_CATCH_UP_AT ? Math.ceil(queue.length / 20) : 1
          msg.content += queue.splice(0, take).join('')
          timer = setTimeout(pump, TYPE_INTERVAL_MS)
          return
        }
        if (streamFinished) {
          if (donePayload?.messageId) msg.id = donePayload.messageId
          if (donePayload?.content) msg.content = donePayload.content
          this.streaming = false
        }
      }

      this.streaming = true
      const conn = sendMessage(
        sessionId,
        { content },
        {
          onChunk: ({ delta }) => {
            // Array.from 按"码点"拆，不会把 emoji 劈成两半
            queue = queue.concat(Array.from(delta))
            if (timer == null) pump()
          },
          onDone: (data) => {
            streamFinished = true
            donePayload = data
            // 队列已空的话，pump 会立刻收尾；还有字就继续慢慢吐
            if (timer == null) pump()
          },
          onError: ({ message }) => {
            // 出错就别慢慢打字了：清空队列，立刻显示错误
            queue = []
            if (timer != null) {
              clearTimeout(timer)
              timer = null
            }
            const msg = list[assistantIndex]
            if (!msg.content) msg.content = `⚠️ ${message || 'AI 服务暂时不可用，请稍后再试'}`
            this.streaming = false
            callbacks?.onError?.()
          }
        }
      )
      conn.finished.finally(() => {
        // 连接结束了（正常 done / 出错 / 用户点停止都算）。
        // 标记流已结束：还在打字就让它吐完再收尾，否则立刻解锁，避免卡在"生成中"。
        streamFinished = true
        if (timer == null) pump()
      })
      return conn
    }
  }
})
