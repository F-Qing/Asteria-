<template>
  <div class="answer-analysis glass">
    <div class="analysis-head">
      <WandSparkles :size="17" :stroke-width="1.8" />
      <h3>AI 解析</h3>
      <SoftTag v-if="isCorrect === true" variant="mint">回答正确</SoftTag>
      <SoftTag v-else-if="isCorrect === false" variant="sakura">回答错误</SoftTag>
      <SoftTag v-else variant="lavender">主观题 · 请自评</SoftTag>
    </div>
    <div v-if="answer" class="analysis-answer">
      <span class="answer-label">{{ isEssay ? '参考答案' : '正确答案' }}</span>
      <MarkdownView :source="answer" />
    </div>
    <MarkdownView v-if="analysis" class="analysis-body" :source="analysis" />
    <p v-else class="analysis-empty text-muted">暂无解析</p>
  </div>
</template>

<script setup lang="ts">
import { WandSparkles } from 'lucide-vue-next'
import MarkdownView from '@/components/base/MarkdownView.vue'
import SoftTag from '@/components/base/SoftTag.vue'

/** AI 解析面板（提交后展开） */
withDefaults(
  defineProps<{
    answer?: string
    analysis?: string
    isCorrect?: boolean | null
    isEssay?: boolean
  }>(),
  { answer: '', analysis: '', isCorrect: null, isEssay: false }
)
</script>

<style scoped>
.answer-analysis {
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}
.analysis-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.analysis-head i {
  color: var(--primary-soft);
}
.analysis-answer {
  display: flex;
  gap: var(--space-3);
  align-items: baseline;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--blue-gray);
  margin-bottom: var(--space-4);
}
.answer-label {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-muted);
}
.analysis-empty {
  font-size: 13px;
}
</style>
