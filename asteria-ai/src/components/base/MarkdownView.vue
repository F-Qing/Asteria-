<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div class="markdown-view" v-html="html"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

/**
 * Markdown 安全渲染（markdown-it + DOMPurify，见 docs/02 2.6）。
 * 所有 AI 输出、题干、解析统一走本组件，禁止直接 v-html 原始数据。
 */
const props = withDefaults(defineProps<{ source?: string }>(), { source: '' })

const html = computed(() => renderMarkdown(props.source))
</script>

<style scoped>
.markdown-view {
  font-size: 15px;
  line-height: 1.9;
  color: var(--text-main);
  word-break: break-word;
}
.markdown-view :deep(p) {
  margin: 0 0 var(--space-3);
}
.markdown-view :deep(p:last-child) {
  margin-bottom: 0;
}
.markdown-view :deep(h1),
.markdown-view :deep(h2),
.markdown-view :deep(h3),
.markdown-view :deep(h4) {
  margin: var(--space-4) 0 var(--space-2);
}
.markdown-view :deep(ul),
.markdown-view :deep(ol) {
  padding-left: var(--space-5);
  margin: 0 0 var(--space-3);
}
.markdown-view :deep(li) {
  margin-bottom: var(--space-1);
}
.markdown-view :deep(code) {
  background: var(--blue-gray);
  border-radius: var(--radius-sm);
  padding: 1px 6px;
  font-size: 13px;
}
.markdown-view :deep(pre) {
  background: var(--blue-gray);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  overflow-x: auto;
  margin: 0 0 var(--space-3);
}
.markdown-view :deep(pre code) {
  background: none;
  padding: 0;
}
.markdown-view :deep(blockquote) {
  border-left: 3px solid var(--sakura);
  padding-left: var(--space-3);
  color: var(--text-sub);
  margin: 0 0 var(--space-3);
}
.markdown-view :deep(a) {
  color: var(--primary);
}
.markdown-view :deep(table) {
  border-collapse: collapse;
  margin: 0 0 var(--space-3);
}
.markdown-view :deep(th),
.markdown-view :deep(td) {
  border: 1px solid var(--glass-border);
  padding: var(--space-2) var(--space-3);
  font-size: 13px;
}
</style>
