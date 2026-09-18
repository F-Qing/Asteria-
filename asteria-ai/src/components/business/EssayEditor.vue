<template>
  <div class="essay-editor">
    <textarea
      :value="modelValue"
      class="essay-textarea"
      :placeholder="placeholder"
      :disabled="disabled"
      rows="6"
      @input="onInput"
    ></textarea>
    <div v-if="showCount" class="essay-count">{{ modelValue.length }} 字</div>
  </div>
</template>

<script setup lang="ts">
/** 作答框（textarea + 字数），简答与填空共用；填空时传 :show-count="false" */
withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    disabled?: boolean
    /** 是否显示右下角字数（填空不需要） */
    showCount?: boolean
  }>(),
  { placeholder: '写下你的答案…', disabled: false, showCount: true }
)

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}
</script>

<style scoped>
.essay-editor {
  position: relative;
}
.essay-textarea {
  width: 100%;
  border: 1.5px solid var(--glass-border);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  padding: var(--space-4);
  font-size: 15px;
  line-height: 1.9;
  color: var(--text-main);
  resize: vertical;
  outline: none;
  transition: var(--transition);
}
.essay-textarea:focus {
  border-color: var(--primary-soft);
  box-shadow: var(--glow-primary);
}
.essay-textarea:disabled {
  opacity: 0.7;
}
.essay-count {
  position: absolute;
  right: var(--space-3);
  bottom: var(--space-2);
  font-size: 13px;
  color: var(--text-muted);
  pointer-events: none;
}
</style>
