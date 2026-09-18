<template>
  <div class="question-stem">
    <MarkdownView :source="stem" />
    <div v-if="knowledgePoints?.length" class="kp-list">
      <SoftTag v-for="kp in knowledgePoints" :key="kp" variant="lavender">{{ kp }}</SoftTag>
    </div>
  </div>
</template>

<script setup lang="ts">
import MarkdownView from '@/components/base/MarkdownView.vue'
import SoftTag from '@/components/base/SoftTag.vue'

/** 题干渲染（Markdown + 知识点标签） */
withDefaults(
  defineProps<{
    stem: string
    knowledgePoints?: string[]
  }>(),
  { knowledgePoints: () => [] }
)
</script>

<style scoped>
.question-stem {
  font-size: 16px;
}
.kp-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
</style>
