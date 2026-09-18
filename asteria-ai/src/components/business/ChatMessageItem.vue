<template>
  <div class="chat-message" :class="message.role">
    <div class="bubble">
      <MarkdownView :source="message.content" />
      <span v-if="caret" class="stream-caret"></span>
      <div v-if="message.role === 'assistant' && message.id > 0 && !streaming" class="msg-actions">
        <button class="action-btn" title="复制" @click="copy">
          <Copy :size="13" :stroke-width="1.8" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Copy } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import MarkdownView from '@/components/base/MarkdownView.vue'
import type { ChatMessage } from '@/types'

/** 聊天气泡（user 右淡蓝灰 / assistant 左薰衣草灰，悬停显示复制） */
const props = withDefaults(
  defineProps<{
    message: ChatMessage
    streaming?: boolean
    /** 流式输出光标（仅生成中的最后一条 AI 消息为 true） */
    caret?: boolean
  }>(),
  { streaming: false, caret: false }
)

async function copy() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败')
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: var(--space-4);
}
.chat-message.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 78%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  transition: var(--transition);
}
.chat-message.user .bubble {
  background: var(--blue-gray);
  border-bottom-right-radius: var(--radius-sm);
}
.chat-message.assistant .bubble {
  background: var(--lavender);
  border-bottom-left-radius: var(--radius-sm);
}
.msg-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-standard);
}
.bubble:hover .msg-actions {
  opacity: 1;
}
.action-btn {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.action-btn:hover {
  background: var(--glass-bg-strong);
  color: var(--text-main);
}
</style>
