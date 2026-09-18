<template>
  <GlassCard class="summary-section">
    <div class="section-head">
      <span class="section-icon" :style="{ background: iconBg }">
        <component :is="icon" :size="16" :stroke-width="1.8" />
      </span>
      <h3>{{ title }}</h3>
    </div>
    <ul class="section-list">
      <li v-for="(item, i) in items" :key="i">
        <MarkdownView :source="item" />
      </li>
    </ul>
    <EmptyState v-if="!items.length" :icon="Cloud" text="暂无内容" />
  </GlassCard>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { Cloud } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import MarkdownView from '@/components/base/MarkdownView.vue'
import EmptyState from '@/components/base/EmptyState.vue'

/** 知识点总结分区展示（线性图标 + Markdown 列表渲染） */
withDefaults(
  defineProps<{
    title: string
    icon?: Component
    items: string[]
    iconBg?: string
  }>(),
  { iconBg: 'var(--blue-gray)' }
)
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
.section-icon {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-soft);
}
.section-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.section-list > li {
  position: relative;
  padding-left: var(--space-4);
  color: var(--text-sub);
}
.section-list > li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-soft);
}
</style>
