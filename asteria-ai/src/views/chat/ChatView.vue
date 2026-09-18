<template>
  <div class="chat-view">
    <!-- ── 会话侧栏（页面内第二列） ── -->
    <aside class="session-panel glass">
      <div class="panel-head">
        <span class="panel-title">会话</span>
        <button class="new-btn" title="新建会话" @click="onNewSession">
          <Plus :size="15" :stroke-width="2" />
        </button>
      </div>
      <div class="session-list">
        <div
          v-for="s in chatStore.sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: s.id === chatStore.activeSessionId }"
          @click="openSession(s.id)"
        >
          <span class="session-title">{{ s.title || '新会话' }}</span>
          <button class="del-btn" title="删除会话" @click.stop="onDeleteSession(s)">
            <Trash2 :size="13" :stroke-width="1.8" />
          </button>
        </div>
        <div v-if="!chatStore.sessions.length" class="session-empty text-muted">
          暂无会话，点右上角 + 开始
        </div>
      </div>
    </aside>

    <!-- ── 聊天主区：仅在已打开会话时升起 ── -->
    <Transition name="chat-rise" mode="out-in">
      <section v-if="chatStore.activeSessionId != null" class="chat-main glass">
        <div ref="msgListRef" class="message-list">
          <TransitionGroup name="list">
            <ChatMessageItem
              v-for="(m, i) in chatStore.activeMessages"
              :key="m.id"
              :message="m"
              :streaming="chatStore.streaming"
              :caret="isCaretTarget(m)"
              :style="{ '--stagger': Math.min(i, 8) }"
            />
          </TransitionGroup>
          <div v-if="chatStore.streaming" class="streaming-line">
            <BreathingLoader small text="Asteria 正在思考…" />
          </div>
          <EmptyState
            v-if="!chatStore.activeMessages.length && !chatStore.streaming"
            scene="plane"
            text="开始提问吧"
            hint="AI 会结合你的题库内容回答"
          />
        </div>

        <div class="input-area">
          <ChatInputBox :streaming="chatStore.streaming" @send="onSend" @stop="onStop" />
        </div>
      </section>

      <!-- 无会话时的轻量引导：无边框面板，只有一枚漂浮提示 -->
      <div v-else class="chat-placeholder">
        <EmptyState
          :icon="Cloud"
          text="点击左侧 + 新建会话"
          hint="或从会话列表中选择，开启你的 AI 学习对话"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Trash2, Cloud } from 'lucide-vue-next'
import BreathingLoader from '@/components/base/BreathingLoader.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ChatMessageItem from '@/components/business/ChatMessageItem.vue'
import ChatInputBox from '@/components/business/ChatInputBox.vue'
import { useChatStore } from '@/stores/chat'
import { useAiConfigStore } from '@/stores/aiConfig'
import type { SseConnection } from '@/utils/sse'
import type { ChatMessage, ChatSession } from '@/types'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const aiConfig = useAiConfigStore()

/** 流式光标目标：生成中 + 最后一条 AI 消息且有内容 */
function isCaretTarget(m: ChatMessage): boolean {
  if (!chatStore.streaming || m.role !== 'assistant' || !m.content) return false
  const list = chatStore.activeMessages
  return list.length > 0 && list[list.length - 1].id === m.id
}

const msgListRef = ref<HTMLElement>()
let currentConn: SseConnection | null = null

onMounted(async () => {
  await chatStore.fetchSessions()

  // 路由是 chat/:sessionId?（可选参数）。从侧边栏进 /chat 时没有 sessionId，
  // vue-router 给的是空字符串 ''，而 Number('') === 0、Number.isFinite(0) === true，
  // 会被当成合法 id 去查会话 0 → 后端返回 40404，用户一进页面就看到红条。
  const raw = route.params.sessionId
  const text = Array.isArray(raw) ? raw[0] : raw
  if (text != null && text !== '') {
    const sid = Number(text)
    if (Number.isInteger(sid) && sid > 0) {
      await chatStore.openSession(sid)
    }
  }
  scrollToBottom()
})

watch(
  () => chatStore.activeMessages.map((m) => m.content).join(''),
  () => scrollToBottom()
)

function scrollToBottom() {
  nextTick(() => {
    const el = msgListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function openSession(id: number) {
  if (chatStore.streaming) onStop()
  await chatStore.openSession(id)
  router.replace(`/chat/${id}`)
  scrollToBottom()
}

async function onNewSession() {
  const s = await chatStore.newSession()
  if (!s) {
    ElMessage.warning('暂时无法创建会话，请确认后端已启动')
    return
  }
  await chatStore.openSession(s.id)
  router.replace(`/chat/${s.id}`)
}

async function onDeleteSession(s: ChatSession) {
  try {
    await ElMessageBox.confirm(`删除会话「${s.title || '新会话'}」？该会话的消息将一并删除。`, '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await chatStore.removeSession(s.id)
  if (chatStore.activeSessionId == null) router.replace('/chat')
}

function onSend(content: string) {
  // 未配置 AI 密钥时温和引导，不阻塞浏览与历史查看
  if (!aiConfig.isConfigured) {
    ElMessage.warning('请先在「设置」中配置 AI 服务密钥与模型')
    return
  }
  currentConn = chatStore.send(content)
  if (!currentConn) ElMessage.warning('请先选择或新建一个会话')
  scrollToBottom()
}

function onStop() {
  currentConn?.abort()
  currentConn = null
  chatStore.streaming = false
}
</script>

<style scoped>
.chat-view {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: var(--space-5);
  height: calc(100vh - 148px);
  max-width: none !important;
}

/* ── 会话侧栏 ── */
.session-panel {
  border-radius: var(--radius-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-4) var(--space-2);
}
.panel-title {
  font-family: var(--font-display);
  font-weight: 600;
}
.new-btn {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  background: var(--blue-gray);
  color: var(--primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.new-btn:hover {
  background: var(--primary-soft);
  color: var(--text-on-primary);
}
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2) var(--space-3) var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-sub);
  transition: var(--transition);
}
.session-item:hover {
  background: var(--blue-gray);
}
.session-item.active {
  background: var(--blue-gray);
  color: var(--text-main);
  font-weight: 600;
}
.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.del-btn {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition);
  flex-shrink: 0;
}
.session-item:hover .del-btn {
  opacity: 1;
}
.del-btn:hover {
  background: var(--error-bg);
  color: var(--error);
}
.session-empty {
  padding: var(--space-5) var(--space-3);
  font-size: 13px;
  text-align: center;
}

/* ── 聊天主区 ── */
.chat-main {
  border-radius: var(--radius-xl);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
/* 无消息时让空状态在消息区垂直居中 */
.message-list:has(> .empty-state) {
  display: flex;
  flex-direction: column;
}
.message-list > .empty-state {
  margin: auto 0;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
}
.streaming-line {
  margin-bottom: var(--space-4);
}
.input-area {
  padding: 0 var(--space-5) var(--space-5);
}

/* ── 无会话占位引导 ── */
.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

/* ── 聊天栏升起过渡 ── */
.chat-rise-enter-active {
  transition:
    opacity var(--duration-slow) var(--ease-out),
    transform var(--duration-slow) var(--ease-spring);
}
.chat-rise-leave-active {
  transition:
    opacity var(--duration-fast) ease,
    transform var(--duration-normal) var(--ease-standard);
}
.chat-rise-enter-from {
  opacity: 0;
  transform: translateY(36px) scale(0.98);
}
.chat-rise-leave-to {
  opacity: 0;
  transform: translateY(14px);
}
@media (prefers-reduced-motion: reduce) {
  .chat-rise-enter-active,
  .chat-rise-leave-active {
    transition: opacity var(--duration-normal) ease;
  }
  .chat-rise-enter-from,
  .chat-rise-leave-to {
    transform: none;
  }
}

@media (max-width: 900px) {
  .chat-view {
    grid-template-columns: 1fr;
  }
  .session-panel {
    display: none;
  }
}
</style>
