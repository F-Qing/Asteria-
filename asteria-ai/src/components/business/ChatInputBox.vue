<template>
  <div class="chat-input-box glass">
    <textarea
      ref="textareaRef"
      v-model="text"
      class="chat-textarea"
      rows="1"
      placeholder="输入问题，Enter 发送，Shift+Enter 换行"
      :disabled="disabled"
      @keydown.enter.exact.prevent="submit"
      @input="autoResize"
    ></textarea>

    <SoftButton v-if="streaming" variant="danger" :icon="Square" @click="$emit('stop')">停止</SoftButton>
    <SoftButton v-else :icon="SendHorizontal" :disabled="!text.trim() || disabled" @click="submit">发送</SoftButton>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Square, SendHorizontal } from 'lucide-vue-next'
import SoftButton from '@/components/base/SoftButton.vue'

/** 聊天输入（发送 / 停止） */
withDefaults(
  defineProps<{
    streaming?: boolean
    disabled?: boolean
  }>(),
  { streaming: false, disabled: false }
)

const emit = defineEmits<{
  (e: 'send', content: string): void
  (e: 'stop'): void
}>()

const text = ref('')
const textareaRef = ref<HTMLTextAreaElement>()

function submit() {
  const content = text.value.trim()
  if (!content) return
  emit('send', content)
  text.value = ''
  autoResize()
}

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}
</script>

<style scoped>
.chat-input-box {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-xl);
}
.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  line-height: 1.7;
  color: var(--text-main);
  max-height: 160px;
  padding: 6px 0;
  min-width: 0;
}
.chat-textarea::placeholder {
  color: var(--text-muted);
}
</style>
